package com.foodbridge.donation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelDonationRequest(
        @NotBlank @Size(max = 300) String reason
) {
}
