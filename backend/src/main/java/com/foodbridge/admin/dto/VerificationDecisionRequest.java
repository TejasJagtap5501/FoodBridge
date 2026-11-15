package com.foodbridge.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VerificationDecisionRequest(
        @NotNull Boolean approved,
        @Size(max = 500) String note
) {
}
