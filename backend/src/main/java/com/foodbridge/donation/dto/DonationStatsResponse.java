package com.foodbridge.donation.dto;

/**
 * Dashboard counters for a donor or the platform. All values are computed from
 * the database on request - nothing is hard-coded in the UI.
 */
public record DonationStatsResponse(
        long totalDonations,
        long available,
        long claimed,
        long completed,
        long expired,
        long cancelled,
        long estimatedMealsContributed,
        long mealsRescued,
        long pendingClaims
) {
}
