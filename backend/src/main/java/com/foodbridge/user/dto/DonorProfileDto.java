package com.foodbridge.user.dto;

import com.foodbridge.user.DonorProfile;

public record DonorProfileDto(
        Long id,
        String businessName,
        String businessType,
        String contactPerson,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String fssaiLicense
) {
    public static DonorProfileDto from(DonorProfile p) {
        return new DonorProfileDto(p.getId(), p.getBusinessName(), p.getBusinessType(),
                p.getContactPerson(), p.getAddressLine1(), p.getAddressLine2(),
                p.getCity(), p.getState(), p.getPostalCode(), p.getFssaiLicense());
    }
}
