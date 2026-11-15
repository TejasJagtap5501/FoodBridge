package com.foodbridge.claim;

import com.foodbridge.claim.dto.CancelClaimRequest;
import com.foodbridge.claim.dto.ClaimRequest;
import com.foodbridge.claim.dto.ClaimResponse;
import com.foodbridge.claim.dto.ClaimStatsResponse;
import com.foodbridge.common.PageRequestFactory;
import com.foodbridge.common.PageResponse;
import com.foodbridge.donation.Donation;
import com.foodbridge.donation.DonationRepository;
import com.foodbridge.donation.DonationStatus;
import com.foodbridge.exception.ApiException;
import com.foodbridge.notification.NotificationService;
import com.foodbridge.notification.NotificationType;
import com.foodbridge.pickup.Pickup;
import com.foodbridge.pickup.PickupRepository;
import com.foodbridge.pickup.PickupStatus;
import com.foodbridge.pickup.dto.PickupResponse;
import com.foodbridge.security.AuthenticatedUser;
import com.foodbridge.user.NgoProfile;
import com.foodbridge.user.NgoProfileRepository;
import com.foodbridge.user.Role;
import com.foodbridge.user.User;
import com.foodbridge.user.UserRepository;
import com.foodbridge.user.VerificationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

/**
 * Donation claiming - the concurrency-critical core of FoodBridge.
 *
 * <h2>How duplicate and concurrent claims are prevented</h2>
 * <ol>
 *   <li><b>Atomic conditional UPDATE</b> -
 *       {@link DonationRepository#claimAtomically} runs
 *       {@code UPDATE donations SET status='CLAIMED' WHERE id=? AND status='AVAILABLE' AND expires_at>now}.
 *       InnoDB evaluates that predicate while holding the row lock, so of two
 *       simultaneous transactions exactly one sees {@code 1 row affected}; the
 *       other sees {@code 0} and is rejected with HTTP 409. This is checked
 *       <i>before</i> the claim row is inserted, so the loser never creates a
 *       claim.</li>
 *   <li><b>Database uniqueness</b> - the generated column
 *       {@code claims.active_donation_id} plus the unique index
 *       {@code uk_claims_active_donation} make a second ACTIVE claim physically
 *       impossible even if application logic regressed.</li>
 *   <li><b>Transaction rollback</b> - the status flip and the claim insert live
 *       in one transaction; any failure restores the donation to AVAILABLE.</li>
 *   <li><b>Optimistic locking</b> - the donation row has a {@code @Version}
 *       column, so an unrelated concurrent edit raises
 *       {@code OptimisticLockingFailureException} instead of silently
 *       overwriting data.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimService {

    private static final Set<String> SORTABLE = Set.of("claimedAt", "status");

    private final ClaimRepository claimRepository;
    private final DonationRepository donationRepository;
    private final UserRepository userRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final NotificationService notificationService;
    private final PickupRepository pickupRepository;

    /**
     * Claim a donation. Runs in a single transaction: either the donation flips
     * to CLAIMED and a claim exists, or nothing changes at all.
     */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ClaimResponse claim(AuthenticatedUser actor, Long donationId, ClaimRequest request) {
        if (actor.role() != Role.NGO) {
            throw ApiException.forbidden("ROLE_REQUIRED", "Only NGO accounts can claim donations");
        }
        NgoProfile profile = ngoProfileRepository.findByUserId(actor.id())
                .orElseThrow(() -> ApiException.forbidden("NGO_PROFILE_MISSING",
                        "Complete your organisation profile before claiming donations"));
        if (profile.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw ApiException.forbidden("NGO_NOT_VERIFIED",
                    "Your organisation is not verified yet. Claims become available once an administrator approves your application.");
        }
        if (!request.suitabilityCheck()) {
            throw ApiException.badRequest("SUITABILITY_NOT_CONFIRMED",
                    "You must confirm that your organisation has assessed the food's suitability");
        }

        // ------------------------------------------------------------------
        // Step 1: attempt the atomic state transition BEFORE reading the row.
        //
        // Reading the donation first and then running a bulk UPDATE on the same
        // row is unsafe under REPEATABLE READ: if another transaction commits a
        // change in between, MariaDB aborts the UPDATE with ER_RECORD_CHANGED.
        // Issuing the conditional UPDATE as the very first statement that
        // touches the row avoids that entirely and keeps the operation atomic.
        // ------------------------------------------------------------------
        int updated;
        try {
            updated = donationRepository.claimAtomically(donationId, Instant.now());
        } catch (org.springframework.dao.DataAccessException ex) {
            // MariaDB reports ER_RECORD_CHANGED ("Record has changed since last read")
            // when a bulk statement races with a concurrent commit. Semantically this
            // is simply "we lost the race", so it becomes the same 409 as a zero-row
            // update instead of leaking a 500 to the client.
            if (ex.getMessage() != null && ex.getMessage().contains("Record has changed since last read")) {
                log.info("Claim lost to a concurrent modification: donationId={} ngo={}", donationId, actor.id());
                throw ApiException.conflict("CONCURRENT_MODIFICATION",
                        "Another organisation claimed this donation at the same moment. Please refresh.");
            }
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "This donation was updated by another request at the same moment. Please refresh.");
        }
        if (updated == 0) {
            Donation current = donationRepository.findDetailedById(donationId)
                    .orElseThrow(() -> ApiException.notFound("DONATION_NOT_FOUND", "Donation not found"));
            log.info("Claim rejected: donation={} status={} state=NGO:{}", current.getReferenceCode(),
                    current.getStatus(), actor.id());
            if (current.getStatus() != DonationStatus.AVAILABLE) {
                throw ApiException.conflict("DONATION_NOT_AVAILABLE",
                        "This donation is no longer available (current status: " + current.getStatus() + ")");
            }
            throw ApiException.conflict("DONATION_EXPIRED",
                    "This donation has passed its expiry time and can no longer be claimed");
        }

        // Step 2: we won the transition - the row is locked by this transaction
        // until commit, so this read reflects our own update.
        Donation donation = donationRepository.findDetailedById(donationId)
                .orElseThrow(() -> ApiException.notFound("DONATION_NOT_FOUND", "Donation not found"));
        if (donation.getDonor().getId().equals(actor.id())) {
            // Throwing here rolls the whole transaction back, including step 1.
            throw ApiException.forbidden("SELF_CLAIM", "You cannot claim your own donation");
        }

        User ngo = userRepository.findById(actor.id())
                .orElseThrow(() -> ApiException.unauthorized("UNAUTHENTICATED", "Authentication is required"));

        Claim claim = new Claim();
        claim.setDonation(donation);
        claim.setNgo(ngo);
        claim.setStatus(ClaimStatus.ACTIVE);
        claim.setMessage(trimToNull(request.message()));
        claim.setSuitabilityCheck(true);
        claim.setClaimedAt(Instant.now());
        Claim saved = claimRepository.save(claim);

        notificationService.create(donation.getDonor().getId(), NotificationType.DONATION_CLAIMED,
                "Donation claimed",
                profile.getOrganizationName() + " has claimed \"" + donation.getFoodName()
                        + "\". Arrange a pickup to complete the handover.",
                "/donor/claims");

        log.info("Donation {} claimed by NGO {} (claim id {})", donation.getReferenceCode(), actor.id(), saved.getId());
        return ClaimResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ClaimResponse getClaim(AuthenticatedUser actor, Long claimId) {
        Claim claim = loadDetailed(claimId);
        assertParticipant(actor, claim);
        PickupResponse pickup = pickupFor(claim.getId());
        return ClaimResponse.from(claim, pickup);
    }

    @Transactional(readOnly = true)
    public PageResponse<ClaimResponse> myClaims(AuthenticatedUser actor, ClaimStatus status,
                                                int page, int size, String sortBy, String direction) {
        Pageable pageable = PageRequestFactory.of(page, size, sortBy, direction, SORTABLE);
        if (pageable.getSort().isUnsorted()) {
            pageable = org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "claimedAt"));
        }
        return switch (actor.role()) {
            case NGO -> PageResponse.of(claimRepository.findByNgo(actor.id(), status, pageable),
                    claim -> ClaimResponse.from(claim, pickupFor(claim.getId())));
            case DONOR -> PageResponse.of(claimRepository.findByDonor(actor.id(), status, pageable),
                    claim -> ClaimResponse.from(claim, pickupFor(claim.getId())));
            default -> PageResponse.of(claimRepository.searchAll(status, null, pageable),
                    claim -> ClaimResponse.from(claim, pickupFor(claim.getId())));
        };
    }

    @Transactional(readOnly = true)
    public PageResponse<ClaimResponse> searchForAdmin(ClaimStatus status, String search, int page, int size,
                                                      String sortBy, String direction) {
        Pageable pageable = PageRequestFactory.of(page, size, sortBy, direction, SORTABLE);
        if (pageable.getSort().isUnsorted()) {
            pageable = org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "claimedAt"));
        }
        return PageResponse.of(claimRepository.searchAll(status, search, pageable),
                claim -> ClaimResponse.from(claim, pickupFor(claim.getId())));
    }

    /**
     * Cancel an active claim. Allowed for the claiming NGO (before handover) and
     * for the donor (e.g. the food spoiled). Administrators use
     * {@link #forceCancel}.
     */
    @Transactional
    public ClaimResponse cancel(AuthenticatedUser actor, Long claimId, CancelClaimRequest request) {
        Claim claim = loadDetailed(claimId);
        boolean ngoOwner = claim.getNgo().getId().equals(actor.id());
        boolean donorOwner = claim.getDonation().getDonor().getId().equals(actor.id());
        if (!ngoOwner && !donorOwner) {
            throw ApiException.forbidden("ACCESS_DENIED", "You cannot cancel this claim");
        }
        if (claim.getStatus() != ClaimStatus.ACTIVE) {
            throw ApiException.conflict("CLAIM_NOT_ACTIVE", "Only active claims can be cancelled");
        }
        if (hasHandoverStarted(claim.getId())) {
            throw ApiException.conflict("HANDOVER_STARTED",
                    "The food has already been handed over, so this claim can no longer be cancelled");
        }

        Donation donation = claim.getDonation();
        dononationRollback(claim, donation, request.reason(), actor);

        notificationService.create(ngoOwner ? donation.getDonor().getId() : claim.getNgo().getId(),
                NotificationType.CLAIM_CANCELLED,
                "Claim cancelled",
                "The claim on \"" + donation.getFoodName() + "\" was cancelled. Reason: " + request.reason(),
                ngoOwner ? "/donor/claims" : "/ngo/claims");

        log.info("Claim {} cancelled by user {}: {}", claim.getId(), actor.id(), request.reason());
        return ClaimResponse.from(claim);
    }

    /** Administrative cancellation; recorded in the audit log by the caller. */
    @Transactional
    public ClaimResponse forceCancel(AuthenticatedUser actor, Long claimId, String reason) {
        Claim claim = loadDetailed(claimId);
        if (claim.getStatus() != ClaimStatus.ACTIVE) {
            throw ApiException.conflict("CLAIM_NOT_ACTIVE", "Only active claims can be cancelled");
        }
        dononationRollback(claim, claim.getDonation(), reason, actor);
        notificationService.create(claim.getNgo().getId(), NotificationType.CLAIM_CANCELLED,
                "Claim cancelled by administrator",
                "The claim on \"" + claim.getDonation().getFoodName() + "\" was cancelled by an administrator. Reason: " + reason,
                "/ngo/claims");
        return ClaimResponse.from(claim);
    }

    @Transactional
    public Claim markCompleted(Claim claim) {
        if (claim.getStatus() != ClaimStatus.ACTIVE) {
            throw ApiException.conflict("CLAIM_NOT_ACTIVE", "Only active claims can be completed");
        }
        claim.setStatus(ClaimStatus.COMPLETED);
        claim.setResolvedAt(Instant.now());
        return claimRepository.save(claim);
    }

    @Transactional(readOnly = true)
    public ClaimStatsResponse statsForNgo(Long ngoId) {
        return new ClaimStatsResponse(
                claimRepository.countByNgoId(ngoId),
                claimRepository.countByNgoIdAndStatus(ngoId, ClaimStatus.ACTIVE),
                claimRepository.countByNgoIdAndStatus(ngoId, ClaimStatus.COMPLETED),
                claimRepository.countByNgoIdAndStatus(ngoId, ClaimStatus.CANCELLED));
    }

    /** Returns the active claim for a donation, if any (used by the detail page). */
    @Transactional(readOnly = true)
    public ClaimResponse activeClaimForDonation(Long donationId) {
        return claimRepository.findByDonationIdAndStatus(donationId, ClaimStatus.ACTIVE)
                .map(ClaimResponse::from)
                .orElse(null);
    }

    /** Latest pickup for a claim, mapped for the API. */
    private PickupResponse pickupFor(Long claimId) {
        return pickupRepository.findFirstByClaimIdOrderByCreatedAtDesc(claimId)
                .map(PickupResponse::from)
                .orElse(null);
    }

    /** True once either party confirmed a handover - cancellation is then refused. */
    private boolean hasHandoverStarted(Long claimId) {
        return pickupRepository.findFirstByClaimIdOrderByCreatedAtDesc(claimId)
                .map(p -> p.getStatus() == PickupStatus.HANDED_OVER
                        || p.getStatus() == PickupStatus.COMPLETED
                        || p.hasDonorConfirmed() || p.hasNgoConfirmed())
                .orElse(false);
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private void dononationRollback(Claim claim, Donation donation, String reason, AuthenticatedUser actor) {
        claim.setStatus(ClaimStatus.CANCELLED);
        claim.setResolvedAt(Instant.now());
        claim.setCancelledReason(reason);
        claimRepository.save(claim);

        // Release the donation so another NGO can rescue the food, unless it has
        // already expired - in which case it must become EXPIRED, not AVAILABLE.
        if (donation.getExpiresAt().isAfter(Instant.now())) {
            donationRepository.releaseToAvailable(donation.getId());
        } else {
            donationRepository.transition(donation.getId(), DonationStatus.CLAIMED, DonationStatus.EXPIRED);
        }
        claim.setDonation(donation);
        log.info("Claim {} cancelled; donation {}", claim.getId(), donation.getReferenceCode());
    }

    private Claim loadDetailed(Long claimId) {
        return claimRepository.findDetailedById(claimId)
                .orElseThrow(() -> ApiException.notFound("CLAIM_NOT_FOUND", "Claim not found"));
    }

    private void assertParticipant(AuthenticatedUser actor, Claim claim) {
        if (actor.role() == Role.ADMIN) {
            return;
        }
        boolean participant = claim.getNgo().getId().equals(actor.id())
                || claim.getDonation().getDonor().getId().equals(actor.id());
        if (!participant) {
            throw ApiException.forbidden("ACCESS_DENIED", "You are not a party to this claim");
        }
    }

    private static String trimToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
