package com.foodbridge.auth;

import com.foodbridge.config.FoodBridgeProperties;
import com.foodbridge.exception.ApiException;
import com.foodbridge.security.JwtService;
import com.foodbridge.user.Role;
import com.foodbridge.user.User;
import com.foodbridge.user.UserStatus;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Pure unit tests: BCrypt hashing, password policy and JWT issuing/validation.
 * No Spring context, so these run in milliseconds.
 */
class AuthUnitTest {

    private static FoodBridgeProperties props() {
        FoodBridgeProperties properties = new FoodBridgeProperties();
        properties.getJwt().setSecret("unit-test-secret-key-that-is-long-enough-for-hs256-signing");
        properties.getJwt().setAccessTokenTtl(Duration.ofMinutes(15));
        properties.getJwt().setIssuer("foodbridge-test");
        return properties;
    }

    private static User user() {
        User user = new User();
        user.setId(42L);
        user.setEmail("donor@example.org");
        user.setFullName("Test Donor");
        user.setRole(Role.DONOR);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash(new BCryptPasswordEncoder(10).encode("SecretPass123"));
        return user;
    }

    // ------------------------------------------------------------------
    // BCrypt
    // ------------------------------------------------------------------

    @Test
    @DisplayName("BCrypt produces a salted hash, never the plaintext")
    void bcryptHashesPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String hash = encoder.encode("SecretPass123");

        assertThat(hash).isNotEqualTo("SecretPass123");
        assertThat(hash).startsWith("$2");
        assertThat(hash).hasSize(60);
        assertThat(encoder.matches("SecretPass123", hash)).isTrue();
        assertThat(encoder.matches("secretpass123", hash)).isFalse();
    }

    @Test
    @DisplayName("BCrypt uses a random salt so identical passwords hash differently")
    void bcryptSaltsEveryHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String first = encoder.encode("SecretPass123");
        String second = encoder.encode("SecretPass123");

        assertThat(first).isNotEqualTo(second);
        assertThat(encoder.matches("SecretPass123", first)).isTrue();
        assertThat(encoder.matches("SecretPass123", second)).isTrue();
    }

    // ------------------------------------------------------------------
    // Password policy
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Password policy rejects short, lowercase-only and digit-less passwords")
    void passwordPolicy() {
        assertThatCode(() -> AuthService.validatePasswordStrength("StrongPass1"))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> AuthService.validatePasswordStrength("Short1"))
                .isInstanceOf(ApiException.class).hasMessageContaining("10-72");
        assertThatThrownBy(() -> AuthService.validatePasswordStrength("alllowercase1"))
                .isInstanceOf(ApiException.class).hasMessageContaining("uppercase");
        assertThatThrownBy(() -> AuthService.validatePasswordStrength("NoDigitsHere"))
                .isInstanceOf(ApiException.class).hasMessageContaining("digit");
        assertThatThrownBy(() -> AuthService.validatePasswordStrength("A".repeat(73) + "a1"))
                .isInstanceOf(ApiException.class);
    }

    // ------------------------------------------------------------------
    // JWT
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Issued JWT carries subject, role and email and validates")
    void jwtIssueAndParse() {
        JwtService jwtService = new JwtService(props());
        JwtService.AccessToken token = jwtService.issue(user());

        assertThat(token.value()).isNotBlank();
        assertThat(token.expiresInSeconds()).isEqualTo(900);

        Optional<Claims> claims = jwtService.parse(token.value());
        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo("42");
        assertThat(claims.get().get(JwtService.CLAIM_ROLE, String.class)).isEqualTo("DONOR");
        assertThat(claims.get().get(JwtService.CLAIM_EMAIL, String.class)).isEqualTo("donor@example.org");
        assertThat(claims.get().getIssuer()).isEqualTo("foodbridge-test");
    }

    @Test
    @DisplayName("A JWT signed with a different key is rejected")
    void jwtRejectsForeignSignature() {
        JwtService issuer = new JwtService(props());

        FoodBridgeProperties other = props();
        other.getJwt().setSecret("a-completely-different-secret-key-long-enough-for-hs256");
        JwtService verifier = new JwtService(other);

        String token = issuer.issue(user()).value();
        assertThat(verifier.parse(token)).isEmpty();
    }

    @Test
    @DisplayName("Expired JWTs are rejected")
    void jwtRejectsExpiredToken() {
        FoodBridgeProperties properties = props();
        properties.getJwt().setAccessTokenTtl(Duration.ofSeconds(-30));
        JwtService jwtService = new JwtService(properties);

        String token = jwtService.issue(user()).value();
        assertThat(jwtService.parse(token)).isEmpty();
    }

    @Test
    @DisplayName("Garbage and tampered tokens are rejected without throwing")
    void jwtRejectsGarbage() {
        JwtService jwtService = new JwtService(props());
        assertThat(jwtService.parse("not-a-jwt")).isEmpty();
        assertThat(jwtService.parse("")).isEmpty();

        String token = jwtService.issue(user()).value();
        String tampered = token.substring(0, token.length() - 3) + "abc";
        assertThat(jwtService.parse(tampered)).isEmpty();
    }

    @Test
    @DisplayName("A secret shorter than 256 bits fails fast at startup")
    void jwtRequiresStrongSecret() {
        FoodBridgeProperties properties = props();
        properties.getJwt().setSecret("too-short");
        assertThatThrownBy(() -> new JwtService(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    // ------------------------------------------------------------------
    // Token hashing helper
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Refresh/reset tokens are stored as SHA-256 hashes")
    void sha256Hash() {
        String hash = AuthService.sha256("raw-refresh-token");
        assertThat(hash).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(hash).isEqualTo(AuthService.sha256("raw-refresh-token"));
        assertThat(hash).isNotEqualTo(AuthService.sha256("other-token"));
    }

    @Test
    @DisplayName("Emails are normalised before lookup")
    void emailNormalisation() {
        assertThat(AuthService.normalizeEmail("  Donor@Example.ORG ")).isEqualTo("donor@example.org");
        assertThat(AuthService.normalizeEmail(null)).isNull();
    }
}
