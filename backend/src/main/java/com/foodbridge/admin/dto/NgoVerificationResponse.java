package com.foodbridge.admin.dto;

import com.foodbridge.user.NgoProfile;
import com.foodbridge.user.VerificationStatus;

import java.time.Instant;

public record NgoVerificationResponse(
        Long profileId,
        Long userId,
        String organizationName,
        String registrationNumber,
        String contactPerson,
        String website,
        String email,
        String phone,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        VerificationStatus verificationStatus,
        String verificationNote,
        Instant verifiedAt,
        Instant appliedAt
) {
    public static NgoVerificationResponse from(NgoProfile p) {
        var u = p.getUser();
        return new NgoVerificationResponse(
                p.getId(), u.getId(), p.getOrganizationName(), p.getRegistrationNumber(),
                p.getContactPerson(), p.getWebsite(), u.getEmail(), u.getPhone(),
                p.getAddressLine1(), p.getAddressLine2(), p.getCity(), p.getState(), p.getPostalCode(),
                p.getVerificationStatus(), p.getVerificationNote(), p.getVerifiedAt(), p.getCreatedAt());
    }
}
