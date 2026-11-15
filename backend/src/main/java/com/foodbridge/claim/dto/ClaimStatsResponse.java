package com.foodbridge.claim.dto;

public record ClaimStatsResponse(
        long totalClaims,
        long activeClaims,
        long completedClaims,
        long cancelledClaims
) {
}
