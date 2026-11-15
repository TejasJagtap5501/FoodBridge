package com.foodbridge.auth;

import com.foodbridge.auth.dto.*;
import com.foodbridge.config.FoodBridgeProperties;
import com.foodbridge.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

/**
 * Authentication endpoints.
 *
 * <p>Refresh tokens live in an HttpOnly, SameSite cookie scoped to
 * {@code /api/auth}. JavaScript can therefore never read them, which removes the
 * most common XSS token-theft vector, while the short-lived access token is kept
 * in memory on the client.</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, token refresh and password recovery")
public class AuthController {

    private final AuthService authService;
    private final FoodBridgeProperties properties;

    @PostMapping("/register/donor")
    @Operation(summary = "Register a food donor (restaurant / hotel / cafe)")
    public ResponseEntity<AuthResponse> registerDonor(@Valid @RequestBody RegisterDonorRequest request,
                                                      HttpServletRequest http, HttpServletResponse response) {
        AuthService.IssuedTokens issued = authService.registerDonor(request, http.getHeader(HttpHeaders.USER_AGENT), clientIp(http));
        setRefreshCookie(response, issued.rawRefreshToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(issued.response());
    }

    @PostMapping("/register/ngo")
    @Operation(summary = "Register an NGO (requires admin verification before claiming)")
    public ResponseEntity<AuthResponse> registerNgo(@Valid @RequestBody RegisterNgoRequest request,
                                                    HttpServletRequest http, HttpServletResponse response) {
        AuthService.IssuedTokens issued = authService.registerNgo(request, http.getHeader(HttpHeaders.USER_AGENT), clientIp(http));
        setRefreshCookie(response, issued.rawRefreshToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(issued.response());
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive an access token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest http, HttpServletResponse response) {
        AuthService.IssuedTokens issued = authService.login(request, http.getHeader(HttpHeaders.USER_AGENT), clientIp(http));
        setRefreshCookie(response, issued.rawRefreshToken());
        return ResponseEntity.ok(issued.response());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange the refresh cookie for a new access token (rotating)")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest http, HttpServletResponse response) {
        String token = readRefreshCookie(http);
        try {
            AuthService.IssuedTokens issued = authService.refresh(token, http.getHeader(HttpHeaders.USER_AGENT), clientIp(http));
            setRefreshCookie(response, issued.rawRefreshToken());
            return ResponseEntity.ok(issued.response());
        } catch (RuntimeException ex) {
            clearRefreshCookie(response);
            throw ex;
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the current refresh token and clear the cookie")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest http, HttpServletResponse response) {
        authService.logout(readRefreshCookie(http));
        clearRefreshCookie(response);
        return ResponseEntity.ok(Map.of("message", "Signed out"));
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Revoke every session for the current user")
    public ResponseEntity<Map<String, String>> logoutAll(HttpServletResponse response) {
        authService.logoutAll(CurrentUser.requireId());
        clearRefreshCookie(response);
        return ResponseEntity.ok(Map.of("message", "All sessions revoked"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset token")
    public PasswordResetResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                HttpServletRequest http) {
        return authService.forgotPassword(request, clientIp(http));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Complete a password reset using a one-time token")
    public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return Map.of("message", "Password updated. Please sign in with your new password.");
    }

    // ------------------------------------------------------------------
    // Cookie handling
    // ------------------------------------------------------------------

    private void setRefreshCookie(HttpServletResponse response, String value) {
        long maxAge = authService.refreshTokenTtlSeconds();
        ResponseCookie cookie = ResponseCookie.from(properties.getSecurity().getRefreshCookieName(), value)
                .httpOnly(true)
                .secure(properties.getSecurity().isCookieSecure())
                .sameSite(properties.getSecurity().getCookieSameSite())
                .path("/api/auth")
                .maxAge(Duration.ofSeconds(maxAge))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(properties.getSecurity().getRefreshCookieName(), "")
                .httpOnly(true)
                .secure(properties.getSecurity().isCookieSecure())
                .sameSite(properties.getSecurity().getCookieSameSite())
                .path("/api/auth")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String readRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (properties.getSecurity().getRefreshCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * Resolves the client IP, honouring a single proxy hop. X-Forwarded-For is
     * only trusted because the app is deployed behind a trusted reverse proxy;
     * the value is truncated so it cannot be used for header injection.
     */
    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
        return ip.length() > 60 ? ip.substring(0, 60) : ip;
    }
}
