package com.foodbridge.user;

import com.foodbridge.auth.AuthService;
import com.foodbridge.auth.dto.ChangePasswordRequest;
import com.foodbridge.security.CurrentUser;
import com.foodbridge.user.dto.MyProfileResponse;
import com.foodbridge.user.dto.UpdateProfileRequest;
import com.foodbridge.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Manage the signed-in user's profile")
public class ProfileController {

    private final UserProfileService profileService;
    private final AuthService authService;

    @GetMapping
    @Operation(summary = "Current user summary")
    public UserResponse me() {
        return profileService.me(CurrentUser.requireId());
    }

    @GetMapping("/profile")
    @Operation(summary = "Full profile including donor/NGO details")
    public MyProfileResponse profile() {
        return profileService.getProfile(CurrentUser.requireId());
    }

    @PutMapping("/profile")
    @Operation(summary = "Update profile details")
    public MyProfileResponse update(@Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(CurrentUser.requireId(), request);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password (revokes all other sessions)")
    public Map<String, String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(CurrentUser.requireId(), request);
        return Map.of("message", "Password updated. Please sign in again.");
    }
}
