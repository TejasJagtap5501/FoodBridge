package com.foodbridge.donation.dto;

import com.foodbridge.donation.Donation;
import com.foodbridge.donation.DonationStatus;
import com.foodbridge.donation.FoodType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Lightweight list projection used by search/browse endpoints so that list pages
 * do not need to load images or donor contact data.
 */
public record DonationSummaryResponse(
        Long id,
        String referenceCode,
        String foodName,
        String categoryName,
        BigDecimal quantity,
        String quantityUnit,
        FoodType foodType,
        String city,
        Instant expiresAt,
        DonationStatus status,
        int estimatedMeals,
        Long donorId
) {
    public static DonationSummaryResponse from(Donation d) {
        return new DonationSummaryResponse(
                d.getId(),
                d.getReferenceCode(),
                d.getFoodName(),
                d.getCategory().getName(),
                d.getQuantity(),
                d.getQuantityUnit(),
                d.getFoodType(),
                d.getCity(),
                d.getExpiresAt(),
                d.getStatus(),
                d.getEstimatedMeals(),
                d.getDonor().getId()
        );
    }
}
