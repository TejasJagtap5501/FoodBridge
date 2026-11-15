package com.foodbridge.notification;

import com.foodbridge.exception.ApiException;
import com.foodbridge.security.JwtService;
import com.foodbridge.user.Role;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validates the access token supplied as a query parameter on the SSE stream.
 *
 * <p>The standard {@code EventSource} browser API cannot attach an
 * {@code Authorization} header, so the token travels in the URL. To keep that
 * exposure as small as possible the token is short-lived (15 minutes) and only
 * used to resolve the caller's own user id - it can never be used to read
 * another user's stream because the registry is keyed strictly by that id.</p>
 */
@Component
@RequiredArgsConstructor
public class SseTokenValidator {

    private final JwtService jwtService;

    public Long userIdFor(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw ApiException.unauthorized("UNAUTHENTICATED", "Access token is required");
        }
        Claims claims = jwtService.parse(accessToken)
                .orElseThrow(() -> ApiException.unauthorized("INVALID_TOKEN", "Access token is invalid or expired"));
        try {
            Long userId = Long.valueOf(claims.getSubject());
            String role = claims.get(JwtService.CLAIM_ROLE, String.class);
            Role.valueOf(role);
            return userId;
        } catch (RuntimeException ex) {
            throw ApiException.unauthorized("INVALID_TOKEN", "Access token is invalid");
        }
    }
}
