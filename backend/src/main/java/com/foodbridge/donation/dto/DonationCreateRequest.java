package com.foodbridge.donation.dto;

import com.foodbridge.donation.FoodType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payload for creating a donation. Validation is intentionally strict: this is
 * the record NGOs rely on to judge whether food is safe to collect.
 */
public record DonationCreateRequest(
        @NotBlank @Size(max = 160) String foodName,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin(value = "0.01", message = "must be greater than zero")
        @Digits(integer = 8, fraction = 2) BigDecimal quantity,
        @NotBlank @Size(max = 16) String quantityUnit,
        @NotNull FoodType foodType,
        @NotNull Long categoryId,
        @NotNull @PastOrPresent(message = "preparation time cannot be in the future") Instant preparedAt,
        @NotNull @Future(message = "expiry must be in the future") Instant expiresAt,
        @NotBlank @Size(max = 300) String pickupAddress,
        @NotBlank @Size(max = 80) String city,
        @Size(max = 500) String pickupInstructions,
        @Size(max = 255) String storageConditions,
        @Size(max = 500) String allergenInfo,
        @Size(max = 500) String handlingInstructions,
        @AssertTrue(message = "you must confirm the food safety declaration") boolean safetyDeclaration
) {
}
