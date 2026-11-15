package com.foodbridge.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Profile update payload. Email and role are immutable - changing the login
 * identity requires a dedicated, audited flow.
 */
public record UpdateProfileRequest(
        @NotBlank @Size(max = 120) String fullName,
        @Pattern(regexp = "^$|^[0-9+\\-() ]{7,24}$", message = "must be a valid phone number") String phone,
        @Size(max = 160) String businessName,
        @Size(max = 32) String businessType,
        @Size(max = 160) String organizationName,
        @Size(max = 200) String website,
        @NotBlank @Size(max = 200) String addressLine1,
        @Size(max = 200) String addressLine2,
        @NotBlank @Size(max = 80) String city,
        @NotBlank @Size(max = 80) String state,
        @NotBlank @Size(max = 16) String postalCode
) {
}
