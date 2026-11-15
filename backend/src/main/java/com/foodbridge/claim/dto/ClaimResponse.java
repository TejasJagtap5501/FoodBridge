package com.foodbridge.claim.dto;

import com.foodbridge.claim.Claim;
import com.foodbridge.claim.ClaimStatus;
import com.foodbridge.pickup.dto.PickupResponse;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Claim projection including the donation summary the NGO and donor need in
 * order to coordinate. Donor/NGO contact details are included because both
 * parties must be able to reach each other for the pickup - and each claim
 * response is only ever returned to those two parties or an administrator.
 */
public record ClaimResponse(
        Long id,
        ClaimStatus status,
        String message,
        boolean suitabilityCheck,
        Instant claimedAt,
        Instant resolvedAt,
        String cancelledReason,
        Long donationId,
        String donationReference,
        String foodName,
        String categoryName,
        BigDecimal quantity,
        String quantityUnit,
        String city,
        Instant expiresAt,
        String pickupAddress,
        Long donorId,
        String donorName,
        String donorEmail,
        String donorPhone,
        Long ngoId,
        String ngoName,
        String ngoEmail,
        String ngoPhone,
        PickupResponse pickup
) {
    public static ClaimResponse from(Claim c) {
        return from(c, null);
    }

    public static ClaimResponse from(Claim c, PickupResponse pickup) {
        var d = c.getDonation();
        var donor = d.getDonor();
        var ngo = c.getNgo();
        return new ClaimResponse(
                c.getId(),
                c.getStatus(),
                c.getMessage(),
                c.isSuitabilityCheck(),
                c.getClaimedAt(),
                c.getResolvedAt(),
                c.getCancelledReason(),
                d.getId(),
                d.getReferenceCode(),
                d.getFoodName(),
                d.getCategory().getName(),
                d.getQuantity(),
                d.getQuantityUnit(),
                d.getCity(),
                d.getExpiresAt(),
                d.getPickupAddress(),
                donor.getId(),
                donor.getFullName(),
                donor.getEmail(),
                donor.getPhone(),
                ngo.getId(),
                ngo.getFullName(),
                ngo.getEmail(),
                ngo.getPhone(),
                pickup
        );
    }
}
