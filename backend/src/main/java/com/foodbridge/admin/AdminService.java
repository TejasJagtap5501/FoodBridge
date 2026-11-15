package com.foodbridge.admin;

import com.foodbridge.admin.dto.*;
import com.foodbridge.audit.AuditAction;
import com.foodbridge.audit.AuditLog;
import com.foodbridge.audit.AuditService;
import com.foodbridge.claim.ClaimRepository;
import com.foodbridge.claim.ClaimStatus;
import com.foodbridge.common.PageRequestFactory;
import com.foodbridge.common.PageResponse;
import com.foodbridge.donation.DonationRepository;
import com.foodbridge.donation.DonationStatus;
import com.foodbridge.exception.ApiException;
import com.foodbridge.impact.ImpactLogRepository;
import com.foodbridge.impact.ImpactQueryRepository;
import com.foodbridge.notification.NotificationService;
import com.foodbridge.notification.NotificationType;
import com.foodbridge.pickup.PickupRepository;
import com.foodbridge.pickup.PickupStatus;
import com.foodbridge.security.AuthenticatedUser;
import com.foodbridge.user.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Administrative operations: NGO verification, account management, platform
 * statistics, reporting and the audit log.
 *
 * <p>Every mutating method writes an audit entry and notifies the affected user
 * so decisions are both traceable and visible to the party concerned.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private static final Set<String> USER_SORTABLE = Set.of("createdAt", "email", "fullName", "lastLoginAt", "status");
    private static final Set<String> NGO_SORTABLE = Set.of("createdAt", "organizationName", "verificationStatus");
    private static final Set<String> AUDIT_SORTABLE = Set.of("createdAt", "action");

    private final UserRepository userRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final DonationRepository donationRepository;
    private final ClaimRepository claimRepository;
    private final PickupRepository pickupRepository;
    private final ImpactLogRepository impactLogRepository;
    private final ImpactQueryRepository impactQueryRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    // ------------------------------------------------------------------
    // Dashboard
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PlatformStatsResponse platformStats() {
        Instant weekAgo = Instant.now().minusSeconds(7 * 86_400L);
        return new PlatformStatsResponse(
                userRepository.count(),
                userRepository.countByRole(Role.DONOR),
                userRepository.countByRole(Role.NGO),
                ngoProfileRepository.countByVerificationStatus(VerificationStatus.PENDING),
                ngoProfileRepository.countByVerificationStatus(VerificationStatus.APPROVED),
                ngoProfileRepository.countByVerificationStatus(VerificationStatus.REJECTED),
                userRepository.countByStatus(UserStatus.INACTIVE),
                donationRepository.count(),
                donationRepository.countByStatus(DonationStatus.AVAILABLE),
                donationRepository.countByStatus(DonationStatus.CLAIMED),
                donationRepository.countByStatus(DonationStatus.COMPLETED),
                donationRepository.countByStatus(DonationStatus.EXPIRED),
                donationRepository.countByStatus(DonationStatus.CANCELLED),
                claimRepository.countByStatus(ClaimStatus.ACTIVE),
                claimRepository.count(),
                pickupRepository.countByStatus(PickupStatus.COMPLETED),
                impactLogRepository.sumMealsPlatform(),
                impactQueryRepository.countCompletedSince(weekAgo),
                impactQueryRepository.countNewUsersSince(weekAgo));
    }

    // ------------------------------------------------------------------
    // User management
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> searchUsers(Role role, UserStatus status, String search,
                                                       int page, int size, String sortBy, String direction) {
        Pageable pageable = PageRequestFactory.of(page, size, sortBy, direction, USER_SORTABLE);
        if (pageable.getSort().isUnsorted()) {
            pageable = org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        Page<User> users = userRepository.search(role, status, blankToNull(search), pageable);
        return PageResponse.of(users, user -> {
            if (user.getRole() == Role.NGO) {
                return ngoProfileRepository.findByUserId(user.getId())
                        .map(p -> new AdminUserResponse(user.getId(), user.getEmail(), user.getFullName(),
                                user.getPhone(), user.getRole(), user.getStatus(), user.isEmailVerified(),
                                user.getLastLoginAt(), user.getCreatedAt(), p.getOrganizationName(),
                                p.getRegistrationNumber(), p.getVerificationStatus()))
                        .orElseGet(() -> AdminUserResponse.from(user));
            }
            return AdminUserResponse.from(user);
        });
    }

    @Transactional
    public AdminUserResponse updateAccountStatus(AuthenticatedUser actor, Long userId,
                                                 AccountStatusRequest request, String ip) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
        if (user.getId().equals(actor.id())) {
            throw ApiException.badRequest("SELF_MODIFICATION", "You cannot change the status of your own account");
        }
        if (user.getRole() == Role.ADMIN) {
            throw ApiException.forbidden("ADMIN_PROTECTED", "Administrator accounts cannot be modified through this endpoint");
        }
        if (request.status() == UserStatus.PENDING) {
            throw ApiException.badRequest("INVALID_STATUS", "Accounts can only be activated or deactivated");
        }
        UserStatus previous = user.getStatus();
        user.setStatus(request.status());
        userRepository.save(user);

        auditService.record(actor, request.status() == UserStatus.ACTIVE
                        ? AuditAction.USER_ACTIVATED : AuditAction.USER_DEACTIVATED,
                "User", user.getId(),
                "Status " + previous + " -> " + request.status()
                        + (request.reason() == null ? "" : ". Reason: " + request.reason()), ip);

        notificationService.create(user.getId(), NotificationType.ACCOUNT_STATUS,
                request.status() == UserStatus.ACTIVE ? "Account activated" : "Account deactivated",
                request.status() == UserStatus.ACTIVE
                        ? "Your FoodBridge account is now active."
                        : "Your FoodBridge account has been deactivated. Contact the administrator for details.",
                "/notifications");

        log.info("Admin {} set user {} status to {}", actor.email(), userId, request.status());
        return AdminUserResponse.from(user);
    }

    // ------------------------------------------------------------------
    // NGO verification
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<NgoVerificationResponse> searchNgoApplications(VerificationStatus status, String search,
                                                                      int page, int size, String sortBy,
                                                                      String direction) {
        Pageable pageable = PageRequestFactory.of(page, size, sortBy, direction, NGO_SORTABLE);
        if (pageable.getSort().isUnsorted()) {
            pageable = org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "createdAt"));
        }
        return PageResponse.of(ngoProfileRepository.search(status, blankToNull(search), pageable),
                NgoVerificationResponse::from);
    }

    /**
     * Approve or reject an NGO application. Approval activates the account so the
     * NGO can immediately claim donations; rejection keeps the account usable for
     * browsing but blocked from claiming.
     */
    @Transactional
    public NgoVerificationResponse decideVerification(AuthenticatedUser actor, Long profileId,
                                                      VerificationDecisionRequest request, String ip) {
        NgoProfile profile = ngoProfileRepository.findById(profileId)
                .orElseThrow(() -> ApiException.notFound("NGO_NOT_FOUND", "NGO application not found"));
        if (profile.getVerificationStatus() != VerificationStatus.PENDING) {
            throw ApiException.conflict("ALREADY_DECIDED",
                    "This application has already been " + profile.getVerificationStatus().name().toLowerCase());
        }
        boolean approved = Boolean.TRUE.equals(request.approved());
        profile.setVerificationStatus(approved ? VerificationStatus.APPROVED : VerificationStatus.REJECTED);
        profile.setVerificationNote(blankToNull(request.note()));
        profile.setVerifiedBy(actor.id());
        profile.setVerifiedAt(Instant.now());
        ngoProfileRepository.save(profile);

        User user = profile.getUser();
        user.setStatus(approved ? UserStatus.ACTIVE : UserStatus.INACTIVE);
        userRepository.save(user);

        auditService.record(actor, approved ? AuditAction.NGO_APPROVED : AuditAction.NGO_REJECTED,
                "NgoProfile", profile.getId(),
                profile.getOrganizationName() + (request.note() == null ? "" : ". Note: " + request.note()), ip);

        notificationService.create(user.getId(), NotificationType.NGO_VERIFICATION,
                approved ? "Organisation verified" : "Verification rejected",
                approved
                        ? "Congratulations - " + profile.getOrganizationName()
                          + " is verified. You can now claim available food donations."
                        : "Your verification was rejected."
                          + (request.note() == null ? "" : " Reason: " + request.note()),
                approved ? "/ngo/browse" : "/ngo/profile");

        log.info("Admin {} {} NGO {}", actor.email(), approved ? "approved" : "rejected", profile.getId());
        return NgoVerificationResponse.from(profile);
    }

    /** Audit hook for administrative claim cancellation. */
    @Transactional
    public void auditClaimCancellation(AuthenticatedUser actor, com.foodbridge.claim.dto.ClaimResponse claim,
                                       String reason, String ip) {
        auditService.record(actor, AuditAction.CLAIM_FORCE_CANCELLED, "Claim", claim.id(),
                "Force-cancelled claim on donation " + claim.donationReference() + ". Reason: " + reason, ip);
    }

    // ------------------------------------------------------------------
    // Reports and activity
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ReportResponse reports(int days) {
        int window = Math.min(Math.max(days, 7), 90);
        Map<LocalDate, Long> donations = impactQueryRepository.donationsPerDay(window);
        Map<LocalDate, Long> meals = impactQueryRepository.mealsPerDay(window);

        List<ReportResponse.DailyPoint> donationPoints = new ArrayList<>();
        List<ReportResponse.DailyPoint> mealPoints = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = window - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            donationPoints.add(new ReportResponse.DailyPoint(date.toString(), donations.getOrDefault(date, 0L)));
            mealPoints.add(new ReportResponse.DailyPoint(date.toString(), meals.getOrDefault(date, 0L)));
        }

        List<ReportResponse.CategoryBreakdown> categories = impactQueryRepository.categoryBreakdown().stream()
                .map(row -> new ReportResponse.CategoryBreakdown(
                        String.valueOf(row[0]), ((Number) row[1]).longValue(), ((Number) row[2]).longValue()))
                .toList();
        List<ReportResponse.CityBreakdown> cities = impactQueryRepository.cityBreakdown(5).stream()
                .map(row -> new ReportResponse.CityBreakdown(
                        String.valueOf(row[0]), ((Number) row[1]).longValue(), ((Number) row[2]).longValue()))
                .toList();
        List<ReportResponse.TopDonor> donors = impactQueryRepository.topDonors(5).stream()
                .map(row -> new ReportResponse.TopDonor(
                        ((Number) row[0]).longValue(), String.valueOf(row[1]),
                        ((Number) row[2]).longValue(), ((Number) row[3]).longValue()))
                .toList();

        BigDecimal quantity = impactLogRepository.findAll().stream()
                .map(i -> "KG".equalsIgnoreCase(i.getQuantityUnit()) ? i.getQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReportResponse(donationPoints, mealPoints, categories, cities, donors,
                impactLogRepository.sumMealsPlatform(), quantity);
    }

    @Transactional(readOnly = true)
    public List<ActivityItem> recentActivity(int limit) {
        int cap = Math.min(Math.max(limit, 1), 50);
        List<ActivityItem> items = new ArrayList<>();
        donationRepository.findAll(org.springframework.data.domain.PageRequest.of(0, cap,
                        Sort.by(Sort.Direction.DESC, "createdAt")))
                .forEach(d -> items.add(new ActivityItem("DONATION",
                        "Donation \"" + d.getFoodName() + "\" (" + d.getStatus() + ")",
                        d.getDonor().getFullName(), d.getCreatedAt(), "/admin/donations")));
        claimRepository.findAll(org.springframework.data.domain.PageRequest.of(0, cap,
                        Sort.by(Sort.Direction.DESC, "claimedAt")))
                .forEach(c -> items.add(new ActivityItem("CLAIM",
                        "Claim " + c.getStatus() + " on \"" + c.getDonation().getFoodName() + "\"",
                        c.getNgo().getFullName(), c.getClaimedAt(), "/admin/claims")));
        return items.stream()
                .sorted((a, b) -> b.occurredAt().compareTo(a.occurredAt()))
                .limit(cap)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLog> auditLogs(String action, String search, int page, int size) {
        Pageable pageable = PageRequestFactory.of(page, size, "createdAt", "desc", AUDIT_SORTABLE);
        return auditService.search(blankToNull(action), blankToNull(search), pageable);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
