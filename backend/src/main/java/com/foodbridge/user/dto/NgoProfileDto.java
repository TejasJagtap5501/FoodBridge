package com.foodbridge.user.dto;

import com.foodbridge.user.NgoProfile;
import com.foodbridge.user.VerificationStatus;

import java.time.Instant;

public record NgoProfileDto(
        Long id,
        String organizationName,
        String registrationNumber,
        String contactPerson,
        String website,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        VerificationStatus verificationStatus,
        String verificationNote,
        Instant verifiedAt
) {
    public static NgoProfileDto from(NgoProfile p) {
        return new NgoProfileDto(p.getId(), p.getOrganizationName(), p.getRegistrationNumber(),
                p.getContactPerson(), p.getWebsite(), p.getAddressLine1(), p.getAddressLine2(),
                p.getCity(), p.getState(), p.getPostalCode(),
                p.getVerificationStatus(), p.getVerificationNote(), p.getVerifiedAt());
    }
}
