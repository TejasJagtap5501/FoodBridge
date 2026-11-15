package com.foodbridge.request.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public record NgoRequestCreate(
        @NotBlank @Size(max = 160) String title,
        @Size(max = 1000) String description,
        Long categoryId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal quantityNeeded,
        @NotBlank @Size(max = 16) String quantityUnit,
        @NotBlank @Size(max = 80) String city,
        @NotNull @Future Instant neededBy,
        @Min(0) int beneficiaries
) {
}
