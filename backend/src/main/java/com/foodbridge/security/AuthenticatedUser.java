package com.foodbridge.security;

import com.foodbridge.user.Role;
import com.foodbridge.user.UserStatus;

/**
 * The authenticated principal placed in the SecurityContext by
 * {@link JwtAuthenticationFilter}. Deliberately minimal: it does not hold the
 * password hash or any other credential material.
 */
public record AuthenticatedUser(
        Long id,
        String email,
        String fullName,
        Role role,
        UserStatus status
) {
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
