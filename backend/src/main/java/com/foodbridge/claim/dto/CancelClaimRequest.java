package com.foodbridge.claim.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelClaimRequest(
        @NotBlank @Size(max = 300) String reason
) {
}
