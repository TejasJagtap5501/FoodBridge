package com.foodbridge.admin.dto;

import com.foodbridge.user.Role;
import com.foodbridge.user.User;
import com.foodbridge.user.UserStatus;
import com.foodbridge.user.VerificationStatus;

import java.time.Instant;

/**
 * Administrative user projection. Exposes only the metadata an administrator
 * needs - never the password hash or lockout internals.
 */
public record AdminUserResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        Role role,
        UserStatus status,
        boolean emailVerified,
        Instant lastLoginAt,
        Instant createdAt,
        String organizationName,
        String registrationNumber,
        VerificationStatus ngoVerificationStatus
) {
    public static AdminUserResponse from(User u) {
        return new AdminUserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getRole(),
                u.getStatus(), u.isEmailVerified(), u.getLastLoginAt(), u.getCreatedAt(),
                null, null, null);
    }
}
