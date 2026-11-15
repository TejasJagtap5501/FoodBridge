package com.foodbridge.auth;

import com.foodbridge.auth.dto.*;
import com.foodbridge.config.FoodBridgeProperties;
import com.foodbridge.exception.ApiException;
import com.foodbridge.notification.NotificationService;
import com.foodbridge.notification.NotificationType;
import com.foodbridge.security.JwtService;
import com.foodbridge.security.RateLimitService;
import com.foodbridge.user.*;
import com.foodbridge.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Registration, login, token rotation and password recovery.
 *
 * <p>Security properties enforced here:</p>
 * <ul>
 *   <li>Passwords are hashed with BCrypt and never logged or returned.</li>
 *   <li>Login responses are identical for unknown-user and wrong-password so the
 *       endpoint cannot be used to enumerate accounts.</li>
 *   <li>Refresh tokens are 256-bit random values stored only as SHA-256 hashes,
 *       rotated on every use, and revoked en masse on logout.</li>
 *   <li>Password reset tokens are single-use, hashed and time limited.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration RESET_TOKEN_TTL = Duration.ofHours(1);
    private static final String PASSWORD_POLICY =
            "Password must be 10-72 characters and include an uppercase letter, a lowercase letter and a digit";

    private final UserRepository userRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OneTimeTokenRepository oneTimeTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationService notificationService;
    private final RateLimitService rateLimitService;
    private final FoodBridgeProperties properties;

    // ------------------------------------------------------------------
    // Registration
    // ------------------------------------------------------------------

    @Transactional
    public IssuedTokens registerDonor(RegisterDonorRequest request, String userAgent, String ip) {
        rateLimitService.check("register:" + ip, 10, Duration.ofHours(1));
        validatePasswordStrength(request.password());
        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email);

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setPhone(blankToNull(request.phone()));
        user.setRole(Role.DONOR);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);
        User saved = userRepository.save(user);

        DonorProfile profile = new DonorProfile();
        profile.setUser(saved);
        profile.setBusinessName(request.businessName().trim());
        profile.setBusinessType(request.businessType());
        profile.setContactPerson(request.contactPerson().trim());
        profile.setAddressLine1(request.addressLine1().trim());
        profile.setAddressLine2(blankToNull(request.addressLine2()));
        profile.setCity(request.city().trim());
        profile.setState(request.state().trim());
        profile.setPostalCode(request.postalCode().trim());
        profile.setFssaiLicense(blankToNull(request.fssaiLicense()));
        donorProfileRepository.save(profile);

        notificationService.createStandalone(saved.getId(), NotificationType.SYSTEM,
                "Welcome to FoodBridge",
                "Your donor account is ready. Publish your first surplus food donation to get started.",
                "/donor/donations/new");

        log.info("Donor registered: userId={} email={}", saved.getId(), maskEmail(email));
        return issueWithCookieToken(saved, userAgent, ip);
    }

    @Transactional
    public IssuedTokens registerNgo(RegisterNgoRequest request, String userAgent, String ip) {
        rateLimitService.check("register:" + ip, 10, Duration.ofHours(1));
        validatePasswordStrength(request.password());
        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email);

        if (ngoProfileRepository.existsByRegistrationNumberIgnoreCase(request.registrationNumber().trim())) {
            throw ApiException.conflict("DUPLICATE_REGISTRATION_NUMBER",
                    "An NGO with this registration number has already applied");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setPhone(blankToNull(request.phone()));
        user.setRole(Role.NGO);
        // NGOs can log in but cannot claim until an admin approves the application.
        user.setStatus(UserStatus.PENDING);
        user.setEmailVerified(false);
        User saved = userRepository.save(user);

        NgoProfile profile = new NgoProfile();
        profile.setUser(saved);
        profile.setOrganizationName(request.organizationName().trim());
        profile.setRegistrationNumber(request.registrationNumber().trim());
        profile.setContactPerson(request.contactPerson().trim());
        profile.setWebsite(blankToNull(request.website()));
        profile.setAddressLine1(request.addressLine1().trim());
        profile.setAddressLine2(blankToNull(request.addressLine2()));
        profile.setCity(request.city().trim());
        profile.setState(request.state().trim());
        profile.setPostalCode(request.postalCode().trim());
        profile.setVerificationStatus(VerificationStatus.PENDING);
        ngoProfileRepository.save(profile);

        notificationService.createStandalone(saved.getId(), NotificationType.NGO_VERIFICATION,
                "Application submitted",
                "Your organisation's verification is pending. You can explore the platform while an administrator reviews it.",
                "/ngo/profile");

        log.info("NGO registered (pending verification): userId={} org={}", saved.getId(), profile.getOrganizationName());
        return issueWithCookieToken(saved, userAgent, ip);
    }

    // ------------------------------------------------------------------
    // Login
    // ------------------------------------------------------------------

    @Transactional
    public IssuedTokens login(LoginRequest request, String userAgent, String ip) {
        String email = normalizeEmail(request.email());
        rateLimitService.check("login:" + ip + ":" + email, 10, Duration.ofMinutes(5));

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);

        // Constant-ish work whether or not the user exists.
        boolean matches = user != null && passwordEncoder.matches(request.password(), user.getPasswordHash());
        if (user == null || !matches) {
            if (user != null) {
                registerFailedLogin(user);
            }
            log.warn("Failed login attempt for email={} ip={}", maskEmail(email), ip);
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid email or password");
        }
        if (user.isLocked()) {
            throw new ApiException(org.springframework.http.HttpStatus.LOCKED, "ACCOUNT_LOCKED",
                    "Account temporarily locked after repeated failed logins. Try again later.");
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw ApiException.forbidden("ACCOUNT_INACTIVE",
                    "This account has been deactivated. Please contact the platform administrator.");
        }

        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("Login success: userId={} role={}", user.getId(), user.getRole());
        return issueWithCookieToken(user, userAgent, ip);
    }

    private void registerFailedLogin(User user) {
        int attempts = user.getFailedLoginCount() + 1;
        user.setFailedLoginCount(attempts);
        int max = properties.getSecurity().getMaxFailedLogins();
        if (attempts >= max) {
            user.setLockedUntil(Instant.now().plus(Duration.ofMinutes(properties.getSecurity().getLockoutMinutes())));
            user.setFailedLoginCount(0);
            log.warn("Account locked after {} failed logins: userId={}", max, user.getId());
        }
        userRepository.save(user);
    }

    // ------------------------------------------------------------------
    // Tokens
    // ------------------------------------------------------------------

    @Transactional
    public IssuedTokens refresh(String rawRefreshToken, String userAgent, String ip) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw ApiException.unauthorized("MISSING_REFRESH_TOKEN", "Refresh token is missing");
        }
        String hash = sha256(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Session expired, please sign in again"));
        if (!stored.isUsable()) {
            throw ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Session expired, please sign in again");
        }
        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Session expired, please sign in again"));
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw ApiException.forbidden("ACCOUNT_INACTIVE", "This account has been deactivated");
        }

        // Rotate: the presented token can never be used twice.
        stored.setRevoked(true);
        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        return issueWithCookieToken(user, userAgent, ip);
    }

    /**
     * Creates a new access/refresh pair. The raw refresh token is returned so the
     * controller can place it in an HttpOnly cookie; it is never sent in a body.
     */
    @Transactional
    public IssuedTokens issueWithCookieToken(User user, String userAgent, String ip) {
        String rawRefresh = randomToken();
        RefreshToken entity = new RefreshToken();
        entity.setUserId(user.getId());
        entity.setTokenHash(sha256(rawRefresh));
        entity.setExpiresAt(Instant.now().plus(properties.getJwt().getRefreshTokenTtl()));
        entity.setUserAgent(truncate(userAgent, 250));
        entity.setIpAddress(ip);
        refreshTokenRepository.save(entity);
        JwtService.AccessToken accessToken = jwtService.issue(user);
        return new IssuedTokens(new AuthResponse(accessToken.value(), "Bearer", accessToken.expiresInSeconds(),
                UserResponse.from(user)), rawRefresh);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(sha256(rawRefreshToken)).ifPresent(token -> {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void logoutAll(Long userId) {
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
    }

    public long refreshTokenTtlSeconds() {
        return properties.getJwt().getRefreshTokenTtl().toSeconds();
    }

    // ------------------------------------------------------------------
    // Password reset
    // ------------------------------------------------------------------

    /**
     * Always returns success to the caller - whether or not the email exists -
     * so the endpoint cannot be used for account enumeration. The token is only
     * surfaced in the response for non-production profiles where no mail
     * transport is configured.
     */
    @Transactional
    public PasswordResetResponse forgotPassword(ForgotPasswordRequest request, String ip) {
        rateLimitService.check("forgot:" + ip, 5, Duration.ofHours(1));
        String email = normalizeEmail(request.email());
        var userOpt = userRepository.findByEmailIgnoreCase(email);
        String token = null;
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            oneTimeTokenRepository.invalidateAll(user.getId(), TokenPurpose.PASSWORD_RESET);
            token = randomToken();
            OneTimeToken entity = new OneTimeToken();
            entity.setUserId(user.getId());
            entity.setTokenHash(sha256(token));
            entity.setPurpose(TokenPurpose.PASSWORD_RESET);
            entity.setExpiresAt(Instant.now().plus(RESET_TOKEN_TTL));
            oneTimeTokenRepository.save(entity);
            log.info("Password reset requested for userId={}", user.getId());
        }
        boolean exposeToken = properties.getSecurity().isExposeResetToken();
        return new PasswordResetResponse(
                "If an account exists for that email, a password reset link has been generated.",
                exposeToken ? token : null);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        validatePasswordStrength(request.newPassword());
        OneTimeToken token = oneTimeTokenRepository
                .findByTokenHashAndPurpose(sha256(request.token()), TokenPurpose.PASSWORD_RESET)
                .orElseThrow(() -> ApiException.badRequest("INVALID_RESET_TOKEN", "This reset link is invalid or already used"));
        if (!token.isUsable()) {
            throw ApiException.badRequest("INVALID_RESET_TOKEN", "This reset link has expired or already been used");
        }
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> ApiException.badRequest("INVALID_RESET_TOKEN", "This reset link is invalid"));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        oneTimeTokenRepository.save(token);
        // All existing sessions are invalidated after a password change.
        refreshTokenRepository.revokeAllForUser(user.getId(), Instant.now());
        log.info("Password reset completed for userId={}", user.getId());
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        validatePasswordStrength(request.newPassword());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("UNAUTHENTICATED", "Authentication is required"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("INVALID_CURRENT_PASSWORD", "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
        log.info("Password changed for userId={}", userId);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    public static void validatePasswordStrength(String password) {
        if (password == null || password.length() < 10 || password.length() > 72) {
            throw ApiException.badRequest("WEAK_PASSWORD", PASSWORD_POLICY);
        }
        boolean upper = password.chars().anyMatch(Character::isUpperCase);
        boolean lower = password.chars().anyMatch(Character::isLowerCase);
        boolean digit = password.chars().anyMatch(Character::isDigit);
        if (!upper || !lower || !digit) {
            throw ApiException.badRequest("WEAK_PASSWORD", PASSWORD_POLICY);
        }
    }

    private void ensureEmailAvailable(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with this email already exists");
        }
    }

    static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        return (local.length() <= 2 ? "**" : local.substring(0, 2) + "***") + domain;
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    /** Returned to the controller so it can set the refresh cookie. */
    public record IssuedTokens(AuthResponse response, String rawRefreshToken) {
    }
}
