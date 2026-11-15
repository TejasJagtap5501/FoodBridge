package com.foodbridge.security;

import com.foodbridge.exception.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Convenience accessor for the authenticated principal. Throws 401 when no
 * authenticated user is present so controllers never have to null-check.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static AuthenticatedUser require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw ApiException.unauthorized("UNAUTHENTICATED", "Authentication is required");
        }
        return user;
    }

    public static Long requireId() {
        return require().id();
    }
}
