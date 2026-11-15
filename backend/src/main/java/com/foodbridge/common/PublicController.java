package com.foodbridge.common;

import com.foodbridge.admin.AdminService;
import com.foodbridge.impact.ImpactLogRepository;
import com.foodbridge.user.Role;
import com.foodbridge.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public, unauthenticated statistics for the marketing site. Only aggregate,
 * non-personal numbers are exposed so the landing page can show real platform
 * activity instead of invented figures.
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Public", description = "Public platform information")
public class PublicController {

    private final UserRepository userRepository;
    private final ImpactLogRepository impactLogRepository;
    private final AdminService adminService;

    @GetMapping("/stats")
    @Operation(summary = "Public impact counters")
    public Map<String, Object> stats() {
        var platform = adminService.platformStats();
        return Map.of(
                "mealsServed", impactLogRepository.sumMealsPlatform(),
                "donationsCompleted", platform.completedDonations(),
                "donors", userRepository.countByRole(Role.DONOR),
                "ngos", userRepository.countByRole(Role.NGO),
                "activeDonations", platform.availableDonations()
        );
    }
}
