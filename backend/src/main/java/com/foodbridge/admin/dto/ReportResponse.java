package com.foodbridge.admin.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregated reporting payload used by the admin Reports page and charts.
 */
public record ReportResponse(
        List<DailyPoint> donationsPerDay,
        List<DailyPoint> mealsPerDay,
        List<CategoryBreakdown> categoryBreakdown,
        List<CityBreakdown> topCities,
        List<TopDonor> topDonors,
        long totalMealsServed,
        BigDecimal totalQuantityRescued
) {
    public record DailyPoint(String date, long count) {
    }

    public record CategoryBreakdown(String category, long donations, long meals) {
    }

    public record CityBreakdown(String city, long donations, long meals) {
    }

    public record TopDonor(Long donorId, String donorName, long completedDonations, long meals) {
    }
}
