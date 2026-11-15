package com.foodbridge.pickup;

import com.foodbridge.claim.ClaimService;
import com.foodbridge.claim.ClaimStatus;
import com.foodbridge.claim.dto.ClaimRequest;
import com.foodbridge.claim.dto.ClaimResponse;
import com.foodbridge.donation.Donation;
import com.foodbridge.donation.DonationRepository;
import com.foodbridge.donation.DonationStatus;
import com.foodbridge.exception.ApiException;
import com.foodbridge.impact.ImpactLog;
import com.foodbridge.impact.ImpactLogRepository;
import com.foodbridge.pickup.dto.PickupResponse;
import com.foodbridge.pickup.dto.SchedulePickupRequest;
import com.foodbridge.support.IntegrationTestBase;
import com.foodbridge.support.TestPrincipals;
import com.foodbridge.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Two-party pickup confirmation and its effect on the donation lifecycle.
 */
class PickupWorkflowIT extends IntegrationTestBase {

    @Autowired private ClaimService claimService;
    @Autowired private PickupService pickupService;
    @Autowired private DonationRepository donations;
    @Autowired private ImpactLogRepository impactLogs;

    private User donor;
    private User ngo;
    private Donation donation;
    private ClaimResponse claim;

    @BeforeEach
    void setUp() {
        donor = dataFactory.donor();
        ngo = dataFactory.approvedNgo();
        donation = donationRepository.save(dataFactory.donation(donor));
        claim = claimService.claim(TestPrincipals.of(ngo), donation.getId(),
                new ClaimRequest("Collecting at 7pm", true));
    }

    @Test
    @DisplayName("Full workflow: schedule → donor handover → NGO collection → donation COMPLETED + impact logged")
    void fullPickupWorkflow() {
        PickupResponse pickup = pickupService.schedule(TestPrincipals.of(ngo), claim.id(),
                new SchedulePickupRequest(Instant.now().plus(2, ChronoUnit.HOURS), "Side gate"));
        assertThat(pickup.status()).isEqualTo(PickupStatus.SCHEDULED);

        PickupResponse afterHandover = pickupService.confirmHandover(TestPrincipals.of(donor), pickup.id());
        assertThat(afterHandover.status()).isEqualTo(PickupStatus.HANDED_OVER);
        assertThat(afterHandover.donorConfirmedAt()).isNotNull();
        assertThat(afterHandover.ngoConfirmedAt()).isNull();

        // Donation is still CLAIMED until the NGO confirms collection.
        assertThat(donations.findById(donation.getId()).orElseThrow().getStatus())
                .isEqualTo(DonationStatus.CLAIMED);

        PickupResponse completed = pickupService.confirmCollection(TestPrincipals.of(ngo), pickup.id());
        assertThat(completed.status()).isEqualTo(PickupStatus.COMPLETED);
        assertThat(completed.completedAt()).isNotNull();

        Donation refreshed = donations.findById(donation.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(DonationStatus.COMPLETED);

        assertThat(claimRepository.findById(claim.id()).orElseThrow().getStatus())
                .isEqualTo(ClaimStatus.COMPLETED);

        ImpactLog impact = impactLogs.findByDonationId(donation.getId()).orElseThrow();
        assertThat(impact.getDonorId()).isEqualTo(donor.getId());
        assertThat(impact.getNgoId()).isEqualTo(ngo.getId());
        assertThat(impact.getMealsServed()).isGreaterThan(0);
    }

    @Test
    @DisplayName("The NGO cannot confirm the donor's handover")
    void ngoCannotConfirmHandover() {
        PickupResponse pickup = pickupService.schedule(TestPrincipals.of(ngo), claim.id(),
                new SchedulePickupRequest(Instant.now().plus(2, ChronoUnit.HOURS), null));

        assertThatThrownBy(() -> pickupService.confirmHandover(TestPrincipals.of(ngo), pickup.id()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("The donor cannot confirm the NGO's collection")
    void donorCannotConfirmCollection() {
        PickupResponse pickup = pickupService.schedule(TestPrincipals.of(ngo), claim.id(),
                new SchedulePickupRequest(Instant.now().plus(2, ChronoUnit.HOURS), null));

        assertThatThrownBy(() -> pickupService.confirmCollection(TestPrincipals.of(donor), pickup.id()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("A pickup cannot be scheduled after the food expires")
    void pickupCannotBeAfterExpiry() {
        Donation shortLived = donations.save(dataFactory.donation(donor, Instant.now().plus(1, ChronoUnit.HOURS)));
        ClaimResponse shortClaim = claimService.claim(TestPrincipals.of(ngo), shortLived.getId(),
                new ClaimRequest(null, true));

        assertThatThrownBy(() -> pickupService.schedule(TestPrincipals.of(ngo), shortClaim.id(),
                new SchedulePickupRequest(Instant.now().plus(5, ChronoUnit.HOURS), null)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("PICKUP_AFTER_EXPIRY"));
    }

    @Test
    @DisplayName("A pickup cannot be rescheduled after a handover confirmation exists")
    void cannotRescheduleAfterConfirmation() {
        PickupResponse pickup = pickupService.schedule(TestPrincipals.of(ngo), claim.id(),
                new SchedulePickupRequest(Instant.now().plus(2, ChronoUnit.HOURS), null));
        pickupService.confirmHandover(TestPrincipals.of(donor), pickup.id());

        assertThatThrownBy(() -> pickupService.schedule(TestPrincipals.of(ngo), claim.id(),
                new SchedulePickupRequest(Instant.now().plus(4, ChronoUnit.HOURS), null)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("HANDOVER_STARTED"));
    }

    @Test
    @DisplayName("Cancelling a pickup cancels the claim and releases the donation")
    void cancelPickupReleasesDonation() {
        PickupResponse pickup = pickupService.schedule(TestPrincipals.of(ngo), claim.id(),
                new SchedulePickupRequest(Instant.now().plus(2, ChronoUnit.HOURS), null));

        pickupService.cancel(TestPrincipals.of(ngo), pickup.id(), "Vehicle unavailable");

        assertThat(donations.findById(donation.getId()).orElseThrow().getStatus())
                .isEqualTo(DonationStatus.AVAILABLE);
        assertThat(claimRepository.findById(claim.id()).orElseThrow().getStatus())
                .isEqualTo(ClaimStatus.CANCELLED);
    }

    @Test
    @DisplayName("Confirmations are idempotent")
    void confirmationsAreIdempotent() {
        PickupResponse pickup = pickupService.schedule(TestPrincipals.of(ngo), claim.id(),
                new SchedulePickupRequest(Instant.now().plus(2, ChronoUnit.HOURS), null));
        pickupService.confirmHandover(TestPrincipals.of(donor), pickup.id());
        PickupResponse second = pickupService.confirmHandover(TestPrincipals.of(donor), pickup.id());
        assertThat(second.status()).isEqualTo(PickupStatus.HANDED_OVER);
    }
}
