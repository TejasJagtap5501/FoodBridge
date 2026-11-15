package com.foodbridge.admin;

import com.foodbridge.admin.dto.*;
import com.foodbridge.auth.AuthController;
import com.foodbridge.audit.AuditLog;
import com.foodbridge.claim.ClaimService;
import com.foodbridge.claim.ClaimStatus;
import com.foodbridge.claim.dto.CancelClaimRequest;
import com.foodbridge.claim.dto.ClaimResponse;
import com.foodbridge.common.PageResponse;
import com.foodbridge.donation.CategoryService;
import com.foodbridge.donation.DonationService;
import com.foodbridge.donation.DonationStatus;
import com.foodbridge.donation.dto.CategoryRequest;
import com.foodbridge.donation.dto.CategoryResponse;
import com.foodbridge.donation.dto.DonationSummaryResponse;
import com.foodbridge.security.CurrentUser;
import com.foodbridge.user.Role;
import com.foodbridge.user.UserStatus;
import com.foodbridge.user.VerificationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Administrative API. The entire {@code /api/admin/**} tree is restricted to
 * {@code ROLE_ADMIN} in {@link com.foodbridge.config.SecurityConfig}; every
 * mutating endpoint additionally records an audit entry.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Administration", description = "Platform administration (ROLE_ADMIN only)")
public class AdminController {

    private final AdminService adminService;
    private final DonationService donationService;
    private final ClaimService claimService;
    private final CategoryService categoryService;

    // ------------------------------------------------------------------
    // Dashboard & reports
    // ------------------------------------------------------------------

    @GetMapping("/stats")
    @Operation(summary = "Platform-wide statistics")
    public PlatformStatsResponse stats() {
        return adminService.platformStats();
    }

    @GetMapping("/activity")
    @Operation(summary = "Recent platform activity feed")
    public List<ActivityItem> activity(@RequestParam(defaultValue = "10") int limit) {
        return adminService.recentActivity(limit);
    }

    @GetMapping("/reports")
    @Operation(summary = "Aggregated impact and donation reports")
    public ReportResponse reports(@RequestParam(defaultValue = "30") int days) {
        return adminService.reports(days);
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Audit trail of sensitive administrative actions")
    public PageResponse<AuditLog> auditLogs(@RequestParam(required = false) String action,
                                            @RequestParam(required = false) String search,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return adminService.auditLogs(action, search, page, size);
    }

    // ------------------------------------------------------------------
    // Users
    // ------------------------------------------------------------------

    @GetMapping("/users")
    @Operation(summary = "Search and filter all platform users")
    public PageResponse<AdminUserResponse> users(@RequestParam(required = false) Role role,
                                                 @RequestParam(required = false) UserStatus status,
                                                 @RequestParam(required = false) String search,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 @RequestParam(defaultValue = "createdAt") String sortBy,
                                                 @RequestParam(defaultValue = "desc") String direction) {
        return adminService.searchUsers(role, status, search, page, size, sortBy, direction);
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Activate or deactivate an account")
    public AdminUserResponse setUserStatus(@PathVariable Long id,
                                           @Valid @RequestBody AccountStatusRequest request,
                                           HttpServletRequest http) {
        return adminService.updateAccountStatus(CurrentUser.require(), id, request, AuthController.clientIp(http));
    }

    // ------------------------------------------------------------------
    // NGO verification
    // ------------------------------------------------------------------

    @GetMapping("/ngos")
    @Operation(summary = "List NGO applications (filter by verification status)")
    public PageResponse<NgoVerificationResponse> ngos(@RequestParam(required = false) VerificationStatus status,
                                                      @RequestParam(required = false) String search,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size,
                                                      @RequestParam(defaultValue = "createdAt") String sortBy,
                                                      @RequestParam(defaultValue = "asc") String direction) {
        return adminService.searchNgoApplications(status, search, page, size, sortBy, direction);
    }

    @PostMapping("/ngos/{profileId}/verification")
    @Operation(summary = "Approve or reject an NGO application")
    public NgoVerificationResponse decide(@PathVariable Long profileId,
                                          @Valid @RequestBody VerificationDecisionRequest request,
                                          HttpServletRequest http) {
        return adminService.decideVerification(CurrentUser.require(), profileId, request,
                AuthController.clientIp(http));
    }

    // ------------------------------------------------------------------
    // Donations & claims
    // ------------------------------------------------------------------

    @GetMapping("/donations")
    @Operation(summary = "All donations with filters")
    public PageResponse<DonationSummaryResponse> donations(@RequestParam(required = false) DonationStatus status,
                                                           @RequestParam(required = false) Long donorId,
                                                           @RequestParam(required = false) Long categoryId,
                                                           @RequestParam(required = false) String search,
                                                           @RequestParam(required = false) String city,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size,
                                                           @RequestParam(defaultValue = "createdAt") String sortBy,
                                                           @RequestParam(defaultValue = "desc") String direction) {
        return donationService.listForAdmin(status, donorId, categoryId, search, city, page, size, sortBy, direction);
    }

    @GetMapping("/claims")
    @Operation(summary = "All claims with filters")
    public PageResponse<ClaimResponse> claims(@RequestParam(required = false) ClaimStatus status,
                                              @RequestParam(required = false) String search,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @RequestParam(defaultValue = "claimedAt") String sortBy,
                                              @RequestParam(defaultValue = "desc") String direction) {
        return claimService.searchForAdmin(status, search, page, size, sortBy, direction);
    }

    @PostMapping("/claims/{id}/cancel")
    @Operation(summary = "Force-cancel an active claim")
    public ClaimResponse cancelClaim(@PathVariable Long id, @Valid @RequestBody CancelClaimRequest request,
                                     HttpServletRequest http) {
        var response = claimService.forceCancel(CurrentUser.require(), id, request.reason());
        adminService.auditClaimCancellation(CurrentUser.require(), response, request.reason(),
                AuthController.clientIp(http));
        return response;
    }

    // ------------------------------------------------------------------
    // Categories
    // ------------------------------------------------------------------

    @PostMapping("/categories")
    @Operation(summary = "Create a food category")
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request, HttpServletRequest http) {
        return categoryService.create(CurrentUser.require(), request, AuthController.clientIp(http));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update a food category")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request,
                                           HttpServletRequest http) {
        return categoryService.update(CurrentUser.require(), id, request, AuthController.clientIp(http));
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Deactivate a food category")
    public Map<String, String> deactivateCategory(@PathVariable Long id, HttpServletRequest http) {
        categoryService.deactivate(CurrentUser.require(), id, AuthController.clientIp(http));
        return Map.of("message", "Category deactivated");
    }
}
