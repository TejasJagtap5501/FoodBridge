package com.foodbridge.auth.dto;

import com.foodbridge.user.dto.UserResponse;

/**
 * Login/refresh response. The refresh token itself is delivered as an
 * HttpOnly cookie and never appears in the JSON body.
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
}
