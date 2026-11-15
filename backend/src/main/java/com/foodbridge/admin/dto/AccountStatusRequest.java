package com.foodbridge.admin.dto;

import com.foodbridge.user.UserStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountStatusRequest(
        @NotNull UserStatus status,
        @Size(max = 300) String reason
) {
}
