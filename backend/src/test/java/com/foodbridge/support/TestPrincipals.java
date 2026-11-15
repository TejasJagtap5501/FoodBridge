package com.foodbridge.support;

import com.foodbridge.security.AuthenticatedUser;
import com.foodbridge.user.User;

/**
 * Adapts a persisted {@link User} to the {@link AuthenticatedUser} principal the
 * services expect, so service-layer tests do not need an HTTP request.
 */
public final class TestPrincipals {

    private TestPrincipals() {
    }

    public static AuthenticatedUser of(User user) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getFullName(),
                user.getRole(), user.getStatus());
    }
}
