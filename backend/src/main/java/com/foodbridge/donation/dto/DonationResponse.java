package com.foodbridge.donation.dto;

import com.foodbridge.donation.Donation;
import com.foodbridge.donation.DonationImage;
import com.foodbridge.donation.DonationStatus;
import com.foodbridge.donation.FoodType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Full donation representation. Contact details of the donor are only included
 * for the donor themselves, the claiming NGO and administrators - see
 * {@link DonationMapper}.
 */
public record DonationResponse(
        Long id,
        String referenceCode,
        String foodName,
        String description,
        BigDecimal quantity,
        String quantityUnit,
        FoodType foodType,
        CategoryDto category,
        Instant preparedAt,
        Instant expiresAt,
        String pickupAddress,
        String city,
        String pickupInstructions,
        String storageConditions,
        String allergenInfo,
        String handlingInstructions,
        boolean safetyDeclaration,
        int estimatedMeals,
        DonationStatus status,
        boolean expired,
        long minutesUntilExpiry,
        Long donorId,
        String donorName,
        String donorEmail,
        String donorPhone,
        List<String> imageUrls,
        Instant createdAt,
        Instant updatedAt
) {
    public record CategoryDto(Long id, String name, String slug) {
    }

    public static DonationResponse from(Donation d) {
        return from(d, false);
    }

    public static DonationResponse from(Donation d, boolean includeDonorContact) {
        long minutes = d.getExpiresAt().isAfter(Instant.now())
                ? java.time.Duration.between(Instant.now(), d.getExpiresAt()).toMinutes()
                : 0;
        return new DonationResponse(
                d.getId(),
                d.getReferenceCode(),
                d.getFoodName(),
                d.getDescription(),
                d.getQuantity(),
                d.getQuantityUnit(),
                d.getFoodType(),
                new CategoryDto(d.getCategory().getId(), d.getCategory().getName(), d.getCategory().getSlug()),
                d.getPreparedAt(),
                d.getExpiresAt(),
                d.getPickupAddress(),
                d.getCity(),
                d.getPickupInstructions(),
                d.getStorageConditions(),
                d.getAllergenInfo(),
                d.getHandlingInstructions(),
                d.isSafetyDeclaration(),
                d.getEstimatedMeals(),
                d.getStatus(),
                d.isExpired(Instant.now()),
                minutes,
                d.getDonor().getId(),
                includeDonorContact ? d.getDonor().getFullName() : null,
                includeDonorContact ? d.getDonor().getEmail() : null,
                includeDonorContact ? d.getDonor().getPhone() : null,
                d.getImages().stream().map(DonationImage::getStorageKey).map(k -> "/uploads/" + k).toList(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }
}
