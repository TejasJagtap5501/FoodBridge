package com.foodbridge.claim;

import com.foodbridge.claim.dto.CancelClaimRequest;
import com.foodbridge.claim.dto.ClaimRequest;
import com.foodbridge.claim.dto.ClaimResponse;
import com.foodbridge.donation.Donation;
import com.foodbridge.donation.DonationStatus;
import com.foodbridge.exception.ApiException;
import com.foodbridge.support.IntegrationTestBase;
import com.foodbridge.support.TestPrincipals;
import com.foodbridge.user.User;
import com.foodbridge.user.VerificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * Business rules around claiming, expressed as executable requirements.
 */
class ClaimRulesIT extends IntegrationTestBase {

    @Autowired private ClaimService claimService;

    @Test
    @DisplayName("An approved NGO can claim an available donation")
    void approvedNgoCanClaim() {
        User donor = dataFactory.donor();
        User ngo = dataFactory.approvedNgo();
        Donation donation = donationRepository.save(dataFactory.donation(donor));

        ClaimResponse response = claimService.claim(TestPrincipals.of(ngo), donation.getId(),
                new ClaimRequest("We can collect at 6pm", true));

        assertThat(response.status()).isEqualTo(ClaimStatus.ACTIVE);
        assertThat(response.donationReference()).isEqualTo(donation.getReferenceCode());
        assertThat(donationRepository.findById(donation.getId()).orElseThrow().getStatus())
                .isEqualTo(DonationStatus.CLAIMED);
    }

    @Test
    @DisplayName("An unverified NGO cannot claim")
    void unverifiedNgoRejected() {
        User donor = dataFactory.donor();
        User pendingNgo = dataFactory.ngo(VerificationStatus.PENDING);
        Donation donation = donationRepository.save(dataFactory.donation(donor));

        assertThatThrownBy(() -> claimService.claim(TestPrincipals.of(pendingNgo), donation.getId(),
                new ClaimRequest(null, true)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("NGO_NOT_VERIFIED"));

        assertThat(donationRepository.findById(donation.getId()).orElseThrow().getStatus())
                .isEqualTo(DonationStatus.AVAILABLE);
    }

    @Test
    @DisplayName("A rejected NGO cannot claim")
    void rejectedNgoCannotClaim() {
        User donor = dataFactory.donor();
        User rejected = dataFactory.ngo(VerificationStatus.REJECTED);
        Donation donation = donationRepository.save(dataFactory.donation(donor));

        assertThatThrownBy(() -> claimService.claim(TestPrincipals.of(rejected), donation.getId(),
                new ClaimRequest(null, true)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("NGO_NOT_VERIFIED"));
    }

    @Test
    @DisplayName("An expired donation cannot be claimed")
    void expiredDonationCannotBeClaimed() {
        User donor = dataFactory.donor();
        User ngo = dataFactory.approvedNgo();
        Donation donation = donationRepository.save(dataFactory.donation(donor, Instant.now().minusSeconds(60)));

        assertThatThrownBy(() -> claimService.claim(TestPrincipals.of(ngo), donation.getId(),
                new ClaimRequest(null, true)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode())
                        .isIn("DONATION_EXPIRED", "DONATION_NOT_AVAILABLE"));

        assertThat(claimRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("The same NGO cannot claim the same donation twice")
    void duplicateClaimRejected() {
        User donor = dataFactory.donor();
        User ngo = dataFactory.approvedNgo();
        Donation donation = donationRepository.save(dataFactory.donation(donor));

        claimService.claim(TestPrincipals.of(ngo), donation.getId(), new ClaimRequest(null, true));

        assertThatThrownBy(() -> claimService.claim(TestPrincipals.of(ngo), donation.getId(),
                new ClaimRequest(null, true)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode())
                        .isIn("DONATION_NOT_AVAILABLE", "CONCURRENT_MODIFICATION"));

        assertThat(claimRepository.findByDonationIdOrderByClaimedAtDesc(donation.getId())).hasSize(1);
    }

    @Test
    @DisplayName("A second NGO cannot claim an already-claimed donation")
    void secondNgoRejected() {
        User donor = dataFactory.donor();
        User first = dataFactory.approvedNgo();
        User second = dataFactory.approvedNgo();
        Donation donation = donationRepository.save(dataFactory.donation(donor));

        claimService.claim(TestPrincipals.of(first), donation.getId(), new ClaimRequest(null, true));

        assertThatThrownBy(() -> claimService.claim(TestPrincipals.of(second), donation.getId(),
                new ClaimRequest(null, true)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode())
                        .isIn("DONATION_NOT_AVAILABLE", "CONCURRENT_MODIFICATION"));
    }

    @Test
    @DisplayName("Claiming requires an explicit suitability declaration")
    void suitabilityDeclarationRequired() {
        User donor = dataFactory.donor();
        User ngo = dataFactory.approvedNgo();
        Donation donation = donationRepository.save(dataFactory.donation(donor));

        assertThatThrownBy(() -> claimService.claim(TestPrincipals.of(ngo), donation.getId(),
                new ClaimRequest(null, false)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("SUITABILITY_NOT_CONFIRMED"));
    }

    @Test
    @DisplayName("Cancelling an active claim returns the donation to AVAILABLE and keeps claim history")
    void cancelReleasesDonation() {
        User donor = dataFactory.donor();
        User ngo = dataFactory.approvedNgo();
        Donation donation = donationRepository.save(dataFactory.donation(donor));
        ClaimResponse claim = claimService.claim(TestPrincipals.of(ngo), donation.getId(),
                new ClaimRequest(null, true));

        claimService.cancel(TestPrincipals.of(ngo), claim.id(), new CancelClaimRequest("Truck broke down"));

        assertThat(donationRepository.findById(donation.getId()).orElseThrow().getStatus())
                .isEqualTo(DonationStatus.AVAILABLE);
        Claim stored = claimRepository.findById(claim.id()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(ClaimStatus.CANCELLED);
        assertThat(stored.getCancelledReason()).isEqualTo("Truck broke down");
        assertThat(stored.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("After a cancellation another NGO can claim the same donation")
    void cancelledClaimFreesDonation() {
        User donor = dataFactory.donor();
        User first = dataFactory.approvedNgo();
        User second = dataFactory.approvedNgo();
        Donation donation = donationRepository.save(dataFactory.donation(donor));

        ClaimResponse claim = claimService.claim(TestPrincipals.of(first), donation.getId(),
                new ClaimRequest(null, true));
        claimService.cancel(TestPrincipals.of(first), claim.id(), new CancelClaimRequest("Cannot collect"));

        ClaimResponse secondClaim = claimService.claim(TestPrincipals.of(second), donation.getId(),
                new ClaimRequest(null, true));
        assertThat(secondClaim.status()).isEqualTo(ClaimStatus.ACTIVE);
        assertThat(claimRepository.findByDonationIdOrderByClaimedAtDesc(donation.getId())).hasSize(2);
    }

    @Test
    @DisplayName("An unrelated NGO cannot cancel another organisation's claim")
    void foreignCancelRejected() {
        User donor = dataFactory.donor();
        User owner = dataFactory.approvedNgo();
        User stranger = dataFactory.approvedNgo();
        Donation donation = donationRepository.save(dataFactory.donation(donor));
        ClaimResponse claim = claimService.claim(TestPrincipals.of(owner), donation.getId(),
                new ClaimRequest(null, true));

        assertThatThrownBy(() -> claimService.cancel(TestPrincipals.of(stranger), claim.id(),
                new CancelClaimRequest("not mine")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("A donor account cannot claim donations")
    void donorCannotClaim() {
        User donor = dataFactory.donor();
        User otherDonor = dataFactory.donor();
        Donation donation = donationRepository.save(dataFactory.donation(donor));

        assertThatThrownBy(() -> claimService.claim(TestPrincipals.of(otherDonor), donation.getId(),
                new ClaimRequest(null, true)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("ROLE_REQUIRED"));
    }
}
