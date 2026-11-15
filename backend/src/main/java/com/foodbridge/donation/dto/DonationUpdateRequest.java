package com.foodbridge.donation.dto;

import com.foodbridge.donation.FoodType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Editable fields of a donation. Only AVAILABLE donations can be edited and the
 * service re-validates every transition rule server side.
 */
public record DonationUpdateRequest(
        @NotBlank @Size(max = 160) String foodName,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal quantity,
        @NotBlank @Size(max = 16) String quantityUnit,
        @NotNull FoodType foodType,
        @NotNull Long categoryId,
        @NotNull @PastOrPresent Instant preparedAt,
        @NotNull @Future Instant expiresAt,
        @NotBlank @Size(max = 300) String pickupAddress,
        @NotBlank @Size(max = 80) String city,
        @Size(max = 500) String pickupInstructions,
        @Size(max = 255) String storageConditions,
        @Size(max = 500) String allergenInfo,
        @Size(max = 500) String handlingInstructions
) {
}
