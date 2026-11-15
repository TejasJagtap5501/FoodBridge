package com.foodbridge.pickup;

import com.foodbridge.claim.Claim;
import com.foodbridge.claim.ClaimRepository;
import com.foodbridge.claim.ClaimService;
import com.foodbridge.claim.ClaimStatus;
import com.foodbridge.donation.Donation;
import com.foodbridge.donation.DonationService;
import com.foodbridge.exception.ApiException;
import com.foodbridge.notification.NotificationService;
import com.foodbridge.notification.NotificationType;
import com.foodbridge.pickup.dto.PickupResponse;
import com.foodbridge.pickup.dto.ReschedulePickupRequest;
import com.foodbridge.pickup.dto.SchedulePickupRequest;
import com.foodbridge.security.AuthenticatedUser;
import com.foodbridge.user.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Pickup coordination between the donor and the claiming NGO.
 *
 * <h2>Two-party confirmation</h2>
 * <pre>
 * SCHEDULED ──donor confirms handover──> HANDED_OVER ──NGO confirms collection──> COMPLETED
 *     │                                        │
 *     └────────────────cancelled───────────────┴──> CANCELLED (claim released)
 * </pre>
 * Exactly one pickup row exists per claim; scheduling again reschedules it.
 * Completion only happens when <i>both</i> confirmations are present, then the
 * donation is completed and the impact log is written in the same transaction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PickupService {

    private final PickupRepository pickupRepository;
    private final ClaimRepository claimRepository;
    private final ClaimService claimService;
    private final DonationService donationService;
    private final NotificationService notificationService;

    /**
     * Schedule (or reschedule) the pickup for a claim. Either the NGO or the
     * donor may propose a slot; the counterparty sees it immediately.
     */
    @Transactional
    public PickupResponse schedule(AuthenticatedUser actor, Long claimId, SchedulePickupRequest request) {
        Claim claim = loadClaim(claimId);
        boolean ngoOwner = claim.getNgo().getId().equals(actor.id());
        boolean donorOwner = claim.getDonation().getDonor().getId().equals(actor.id());
        if (!ngoOwner && !donorOwner) {
            throw ApiException.forbidden("ACCESS_DENIED", "Only the donor or the claiming NGO can schedule this pickup");
        }
        if (claim.getStatus() != ClaimStatus.ACTIVE) {
            throw ApiException.conflict("CLAIM_NOT_ACTIVE", "Only active claims can be scheduled for pickup");
        }
        Donation donation = claim.getDonation();
        if (!request.scheduledAt().isBefore(donation.getExpiresAt())) {
            throw ApiException.badRequest("PICKUP_AFTER_EXPIRY",
                    "The pickup must be scheduled before the food expires at " + donation.getExpiresAt());
        }

        Pickup pickup = pickupRepository.findFirstByClaimIdOrderByCreatedAtDesc(claimId).orElse(null);
        boolean isNew = pickup == null;
        if (isNew) {
            pickup = new Pickup();
            pickup.setClaim(claim);
            pickup.setCreatedBy(actor.id());
            pickup.setStatus(PickupStatus.SCHEDULED);
        } else if (pickup.getStatus() == PickupStatus.COMPLETED || pickup.getStatus() == PickupStatus.CANCELLED) {
            throw ApiException.conflict("PICKUP_CLOSED", "This pickup is already " + pickup.getStatus().name().toLowerCase());
        } else if (pickup.hasDonorConfirmed() || pickup.hasNgoConfirmed()) {
            throw ApiException.conflict("HANDOVER_STARTED",
                    "The pickup can no longer be rescheduled because a handover confirmation already exists");
        }
        pickup.setScheduledAt(request.scheduledAt());
        pickup.setInstructions(trimToNull(request.instructions()));
        Pickup saved = pickupRepository.save(pickup);

        Long counterpartId = ngoOwner ? donation.getDonor().getId() : claim.getNgo().getId();
        notificationService.create(counterpartId, NotificationType.PICKUP_SCHEDULED,
                isNew ? "Pickup scheduled" : "Pickup rescheduled",
                (actor.fullName() == null ? "The other party" : actor.fullName()) + " scheduled the pickup of \""
                        + donation.getFoodName() + "\" for " + request.scheduledAt(),
                ngoOwner ? "/donor/pickups" : "/ngo/pickups");

        log.info("Pickup {} for claim {} scheduled at {}", saved.getId(), claimId, request.scheduledAt());
        return PickupResponse.from(saved);
    }

    @Transactional
    public PickupResponse reschedule(AuthenticatedUser actor, Long pickupId, ReschedulePickupRequest request) {
        Pickup pickup = loadPickup(pickupId);
        return schedule(actor, pickup.getClaim().getId(),
                new SchedulePickupRequest(request.scheduledAt(), request.instructions()));
    }

    /** Donor confirms that the food has been handed to the NGO's representative. */
    @Transactional
    public PickupResponse confirmHandover(AuthenticatedUser actor, Long pickupId) {
        Pickup pickup = loadPickup(pickupId);
        Claim claim = pickup.getClaim();
        if (!claim.getDonation().getDonor().getId().equals(actor.id())) {
            throw ApiException.forbidden("ACCESS_DENIED", "Only the donating business can confirm the handover");
        }
        if (pickup.getStatus() == PickupStatus.COMPLETED) {
            throw ApiException.conflict("PICKUP_CLOSED", "This pickup is already completed");
        }
        if (pickup.getStatus() == PickupStatus.CANCELLED) {
            throw ApiException.conflict("PICKUP_CLOSED", "This pickup was cancelled");
        }
        if (pickup.hasDonorConfirmed()) {
            return PickupResponse.from(pickup);
        }
        pickup.setDonorConfirmedAt(Instant.now());
        pickup.setStatus(PickupStatus.HANDED_OVER);
        Pickup saved = pickupRepository.save(pickup);

        notificationService.create(claim.getNgo().getId(), NotificationType.PICKUP_HANDED_OVER,
                "Food handed over",
                claim.getDonation().getFoodName() + " has been handed over. Please confirm collection once you receive it.",
                "/ngo/pickups");

        log.info("Pickup {} handover confirmed by donor {}", pickupId, actor.id());
        return completeIfReady(saved);
    }

    /** NGO confirms that it has collected the food; completes the donation. */
    @Transactional
    public PickupResponse confirmCollection(AuthenticatedUser actor, Long pickupId) {
        Pickup pickup = loadPickup(pickupId);
        Claim claim = pickup.getClaim();
        if (!claim.getNgo().getId().equals(actor.id())) {
            throw ApiException.forbidden("ACCESS_DENIED", "Only the claiming NGO can confirm collection");
        }
        if (pickup.getStatus() == PickupStatus.COMPLETED) {
            throw ApiException.conflict("PICKUP_CLOSED", "This pickup is already completed");
        }
        if (pickup.getStatus() == PickupStatus.CANCELLED) {
            throw ApiException.conflict("PICKUP_CLOSED", "This pickup was cancelled");
        }
        if (pickup.hasNgoConfirmed()) {
            return PickupResponse.from(pickup);
        }
        pickup.setNgoConfirmedAt(Instant.now());
        Pickup saved = pickupRepository.save(pickup);

        notificationService.create(claim.getDonation().getDonor().getId(), NotificationType.PICKUP_COMPLETED,
                "Collection confirmed",
                claim.getNgo().getFullName() + " confirmed collecting " + claim.getDonation().getFoodName() + ".",
                "/donor/pickups");

        log.info("Pickup {} collection confirmed by NGO {}", pickupId, actor.id());
        return completeIfReady(saved);
    }

    @Transactional
    public PickupResponse cancel(AuthenticatedUser actor, Long pickupId, String reason) {
        Pickup pickup = loadPickup(pickupId);
        Claim claim = pickup.getClaim();
        boolean participant = claim.getNgo().getId().equals(actor.id())
                || claim.getDonation().getDonor().getId().equals(actor.id())
                || actor.role() == Role.ADMIN;
        if (!participant) {
            throw ApiException.forbidden("ACCESS_DENIED", "You cannot cancel this pickup");
        }
        if (pickup.getStatus() == PickupStatus.COMPLETED) {
            throw ApiException.conflict("PICKUP_CLOSED", "A completed pickup cannot be cancelled");
        }
        pickup.setStatus(PickupStatus.CANCELLED);
        pickup.setCancelledReason(reason);
        pickupRepository.save(pickup);

        // Cancelling the pickup cancels the claim and releases the donation.
        claimService.cancel(actor, claim.getId(),
                new com.foodbridge.claim.dto.CancelClaimRequest(reason));

        Long counterpartId = claim.getNgo().getId().equals(actor.id())
                ? claim.getDonation().getDonor().getId()
                : claim.getNgo().getId();
        notificationService.create(counterpartId, NotificationType.PICKUP_SCHEDULED,
                "Pickup cancelled", "Pickup for \"" + claim.getDonation().getFoodName()
                        + "\" was cancelled. Reason: " + reason, "/notifications");
        return PickupResponse.from(pickup);
    }

    @Transactional(readOnly = true)
    public List<PickupResponse> myPickups(AuthenticatedUser actor) {
        List<Pickup> pickups = actor.role() == Role.DONOR
                ? pickupRepository.findByDonor(actor.id())
                : pickupRepository.findByNgo(actor.id());
        return pickups.stream().map(PickupResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PickupResponse getPickup(AuthenticatedUser actor, Long pickupId) {
        Pickup pickup = loadPickup(pickupId);
        Claim claim = pickup.getClaim();
        boolean participant = claim.getNgo().getId().equals(actor.id())
                || claim.getDonation().getDonor().getId().equals(actor.id())
                || actor.role() == Role.ADMIN;
        if (!participant) {
            throw ApiException.forbidden("ACCESS_DENIED", "You are not a party to this pickup");
        }
        return PickupResponse.from(pickup);
    }

    @Transactional(readOnly = true)
    public Optional<PickupResponse> findForClaim(Long claimId) {
        return pickupRepository.findFirstByClaimIdOrderByCreatedAtDesc(claimId).map(PickupResponse::from);
    }

    @Transactional(readOnly = true)
    public boolean hasHandoverStarted(Long claimId) {
        return pickupRepository.findFirstByClaimIdOrderByCreatedAtDesc(claimId)
                .map(p -> p.getStatus() == PickupStatus.HANDED_OVER || p.getStatus() == PickupStatus.COMPLETED
                        || p.hasDonorConfirmed() || p.hasNgoConfirmed())
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public long countCompleted() {
        return pickupRepository.countByStatus(PickupStatus.COMPLETED);
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    /**
     * Called after each confirmation. When both parties have confirmed, the
     * pickup becomes COMPLETED, the claim becomes COMPLETED and the donation is
     * completed exactly once - all inside the caller's transaction so a failure
     * rolls everything back.
     */
    private PickupResponse completeIfReady(Pickup pickup) {
        if (pickup.isFullyConfirmed() && pickup.getStatus() != PickupStatus.COMPLETED) {
            pickup.setStatus(PickupStatus.COMPLETED);
            pickup.setCompletedAt(Instant.now());
            pickupRepository.save(pickup);

            Claim claim = pickup.getClaim();
            donationService.completeDonation(claim.getDonation(), claim.getNgo().getId());
            claimService.markCompleted(claim);
            log.info("Pickup {} completed; donation {} rescued", pickup.getId(),
                    claim.getDonation().getReferenceCode());
        }
        return PickupResponse.from(pickup);
    }

    private Pickup loadPickup(Long pickupId) {
        return pickupRepository.findDetailedById(pickupId)
                .orElseThrow(() -> ApiException.notFound("PICKUP_NOT_FOUND", "Pickup not found"));
    }

    private Claim loadClaim(Long claimId) {
        return claimRepository.findDetailedById(claimId)
                .orElseThrow(() -> ApiException.notFound("CLAIM_NOT_FOUND", "Claim not found"));
    }

    private static String trimToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
