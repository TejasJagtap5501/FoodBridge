package com.foodbridge.security;

import com.foodbridge.config.FoodBridgeProperties;
import com.foodbridge.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Issues and validates short-lived JWT access tokens.
 *
 * <p>Design decisions:</p>
 * <ul>
 *   <li>Access tokens carry the user id ({@code sub}), email, role and account
 *       status so the filter can reject deactivated accounts without a DB hit
 *       for the common path.</li>
 *   <li>Refresh tokens are opaque random strings stored as hashes - not JWTs -
 *       so they can be revoked server side.</li>
 *   <li>The HMAC secret is required to be at least 256 bits; startup fails fast
 *       otherwise.</li>
 * </ul>
 */
@Service
@Slf4j
public class JwtService {

    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_NAME = "name";
    public static final String CLAIM_STATUS = "status";

    private final SecretKey key;
    private final FoodBridgeProperties.Jwt config;

    public JwtService(FoodBridgeProperties properties) {
        this.config = properties.getJwt();
        this.key = buildKey(config.getSecret());
    }

    private static SecretKey buildKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret must be configured (JWT_SECRET)");
        }
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secret);
            if (bytes.length < 32) {
                bytes = secret.getBytes(StandardCharsets.UTF_8);
            }
        } catch (RuntimeException ex) {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public AccessToken issue(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(config.getAccessTokenTtl());
        String token = Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuer(config.getIssuer())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_NAME, user.getFullName())
                .claim(CLAIM_STATUS, user.getStatus().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
        return new AccessToken(token, expiry, config.getAccessTokenTtl().toSeconds());
    }

    /**
     * Validate a compact JWS.
     *
     * <p>Beyond verifying the HMAC, the token must be <em>canonically encoded</em>:
     * the signature segment is re-encoded and compared with what the client sent.
     * Without this check a token remains valid when extra characters are appended,
     * because the unused trailing bits of the final base64url character are
     * ignored by the decoder (signature malleability). Rejecting non-canonical
     * encodings means "valid token" and "exact token string" are the same thing.</p>
     */
    public Optional<Claims> parse(String token) {
        if (!isCanonicallyEncoded(token)) {
            log.debug("Rejected non-canonical JWT encoding");
            return Optional.empty();
        }
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(config.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejected JWT: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private static boolean isCanonicallyEncoded(String token) {
        if (token == null) {
            return false;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }
        try {
            String reEncoded = java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(java.util.Base64.getUrlDecoder().decode(parts[2]));
            return reEncoded.equals(parts[2]);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public record AccessToken(String value, Instant expiresAt, long expiresInSeconds) {
    }
}
