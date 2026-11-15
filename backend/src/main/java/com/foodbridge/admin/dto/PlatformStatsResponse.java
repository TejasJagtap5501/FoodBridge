package com.foodbridge.admin.dto;

/**
 * Every number on the admin dashboard, computed live from the database.
 */
public record PlatformStatsResponse(
        long totalUsers,
        long totalDonors,
        long totalNgos,
        long pendingNgoVerifications,
        long approvedNgos,
        long rejectedNgos,
        long inactiveUsers,
        long totalDonations,
        long availableDonations,
        long claimedDonations,
        long completedDonations,
        long expiredDonations,
        long cancelledDonations,
        long activeClaims,
        long totalClaims,
        long completedPickups,
        long estimatedMealsServed,
        long donationsCompletedLast7Days,
        long newUsersLast7Days
) {
}
