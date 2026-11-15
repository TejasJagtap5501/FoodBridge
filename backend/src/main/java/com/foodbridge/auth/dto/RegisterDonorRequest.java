package com.foodbridge.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Donor registration payload (restaurant / hotel / cafe / food business).
 */
public record RegisterDonorRequest(
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(min = 10, max = 72) String password,
        @NotBlank @Size(max = 120) String fullName,
        @Pattern(regexp = "^$|^[0-9+\\-() ]{7,24}$", message = "must be a valid phone number") String phone,

        @NotBlank @Size(max = 160) String businessName,
        @NotBlank @Pattern(regexp = "RESTAURANT|HOTEL|CAFE|BAKERY|CATERING|OTHER",
                message = "must be one of RESTAURANT, HOTEL, CAFE, BAKERY, CATERING, OTHER") String businessType,
        @NotBlank @Size(max = 120) String contactPerson,
        @NotBlank @Size(max = 200) String addressLine1,
        @Size(max = 200) String addressLine2,
        @NotBlank @Size(max = 80) String city,
        @NotBlank @Size(max = 80) String state,
        @NotBlank @Size(max = 16) String postalCode,
        @Size(max = 32) String fssaiLicense
) {
}
