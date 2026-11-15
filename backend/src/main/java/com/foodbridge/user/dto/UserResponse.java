package com.foodbridge.user.dto;

import com.foodbridge.user.Role;
import com.foodbridge.user.User;
import com.foodbridge.user.UserStatus;

import java.time.Instant;

/**
 * Safe projection of {@link User} for API responses. Contains no password hash,
 * lockout counters or other credential-adjacent fields.
 */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        Role role,
        UserStatus status,
        boolean emailVerified,
        Instant lastLoginAt,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}
