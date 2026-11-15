package com.foodbridge.pickup.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record ReschedulePickupRequest(
        @NotNull @Future(message = "pickup must be scheduled in the future") Instant scheduledAt,
        @Size(max = 500) String instructions
) {
}
