package com.foodbridge.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Single, consistent error envelope returned by every failing endpoint.
 *
 * <pre>
 * {
 *   "timestamp": "2026-11-05T10:15:30Z",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "code": "VALIDATION_FAILED",
 *   "message": "Request validation failed",
 *   "path": "/api/auth/register/donor",
 *   "requestId": "b4c0...",
 *   "fieldErrors": { "email": "must be a well-formed email address" }
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String requestId,
        Map<String, String> fieldErrors
) {
}
