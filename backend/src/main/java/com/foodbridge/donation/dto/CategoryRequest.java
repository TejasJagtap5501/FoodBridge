package com.foodbridge.donation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 255) String description,
        boolean active,
        int displayOrder
) {
}
