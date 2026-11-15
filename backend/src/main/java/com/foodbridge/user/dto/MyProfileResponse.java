package com.foodbridge.user.dto;

/**
 * Composite profile response for the "My Profile" screens. Exactly one of
 * {@code donorProfile} / {@code ngoProfile} is populated depending on the role.
 */
public record MyProfileResponse(
        UserResponse user,
        DonorProfileDto donorProfile,
        NgoProfileDto ngoProfile
) {
}
