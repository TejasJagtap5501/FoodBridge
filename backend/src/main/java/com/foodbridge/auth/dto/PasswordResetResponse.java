package com.foodbridge.auth.dto;

/**
 * @param message     human readable status
 * @param resetToken  only populated in non-production profiles when email
 *                    delivery is not configured, so the flow is testable
 *                    locally. It is {@code null} in production.
 */
public record PasswordResetResponse(String message, String resetToken) {
}
