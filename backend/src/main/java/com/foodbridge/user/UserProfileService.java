package com.foodbridge.user;

import com.foodbridge.exception.ApiException;
import com.foodbridge.user.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read and update the signed-in user's own profile.
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final NgoProfileRepository ngoProfileRepository;

    @Transactional(readOnly = true)
    public MyProfileResponse getProfile(Long userId) {
        User user = load(userId);
        DonorProfileDto donor = donorProfileRepository.findByUserId(userId).map(DonorProfileDto::from).orElse(null);
        NgoProfileDto ngo = ngoProfileRepository.findByUserId(userId).map(NgoProfileDto::from).orElse(null);
        return new MyProfileResponse(UserResponse.from(user), donor, ngo);
    }

    @Transactional
    public MyProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = load(userId);
        user.setFullName(request.fullName().trim());
        user.setPhone(trimToNull(request.phone()));
        userRepository.save(user);

        if (user.getRole() == Role.DONOR) {
            DonorProfile profile = donorProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> ApiException.notFound("PROFILE_NOT_FOUND", "Donor profile not found"));
            if (request.businessName() != null && !request.businessName().isBlank()) {
                profile.setBusinessName(request.businessName().trim());
            }
            if (request.businessType() != null && !request.businessType().isBlank()) {
                profile.setBusinessType(request.businessType());
            }
            profile.setAddressLine1(request.addressLine1().trim());
            profile.setAddressLine2(trimToNull(request.addressLine2()));
            profile.setCity(request.city().trim());
            profile.setState(request.state().trim());
            profile.setPostalCode(request.postalCode().trim());
            donorProfileRepository.save(profile);
        } else if (user.getRole() == Role.NGO) {
            NgoProfile profile = ngoProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> ApiException.notFound("PROFILE_NOT_FOUND", "NGO profile not found"));
            if (request.organizationName() != null && !request.organizationName().isBlank()) {
                profile.setOrganizationName(request.organizationName().trim());
            }
            profile.setWebsite(trimToNull(request.website()));
            profile.setAddressLine1(request.addressLine1().trim());
            profile.setAddressLine2(trimToNull(request.addressLine2()));
            profile.setCity(request.city().trim());
            profile.setState(request.state().trim());
            profile.setPostalCode(request.postalCode().trim());
            ngoProfileRepository.save(profile);
        }
        return getProfile(userId);
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return UserResponse.from(load(userId));
    }

    private User load(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("UNAUTHENTICATED", "Authentication is required"));
    }

    private static String trimToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
