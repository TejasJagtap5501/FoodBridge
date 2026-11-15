package com.foodbridge.pickup.dto;

import com.foodbridge.pickup.Pickup;
import com.foodbridge.pickup.PickupStatus;

import java.time.Instant;

public record PickupResponse(
        Long id,
        Long claimId,
        Long donationId,
        String donationReference,
        String foodName,
        String donorName,
        String ngoName,
        Instant scheduledAt,
        PickupStatus status,
        String instructions,
        Instant donorConfirmedAt,
        Instant ngoConfirmedAt,
        Instant completedAt,
        String cancelledReason
) {
    public static PickupResponse from(Pickup p) {
        var claim = p.getClaim();
        var donation = claim.getDonation();
        return new PickupResponse(
                p.getId(),
                claim.getId(),
                donation.getId(),
                donation.getReferenceCode(),
                donation.getFoodName(),
                donation.getDonor().getFullName(),
                claim.getNgo().getFullName(),
                p.getScheduledAt(),
                p.getStatus(),
                p.getInstructions(),
                p.getDonorConfirmedAt(),
                p.getNgoConfirmedAt(),
                p.getCompletedAt(),
                p.getCancelledReason()
        );
    }
}
