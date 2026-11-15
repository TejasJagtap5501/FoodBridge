package com.foodbridge.donation;

import com.foodbridge.common.PageResponse;
import com.foodbridge.donation.dto.*;
import com.foodbridge.exception.ApiException;
import com.foodbridge.support.IntegrationTestBase;
import com.foodbridge.support.TestPrincipals;
import com.foodbridge.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Donation creation, editing, cancellation, ownership and search behaviour.
 */
class DonationServiceIT extends IntegrationTestBase {

    @Autowired private DonationService donationService;

    private DonationCreateRequest createRequest() {
        return new DonationCreateRequest(
                "Paneer Tikka", "Freshly grilled", new BigDecimal("6.5"), "KG", FoodType.VEGETARIAN,
                dataFactory.category("cooked-meals").getId(),
                Instant.now().minus(1, ChronoUnit.HOURS),
                Instant.now().plus(5, ChronoUnit.HOURS),
                "12 FC Road, Pune", "Pune", "Use the service entrance",
                "Refrigerate below 5C", "Contains dairy", "Use clean containers", true);
    }

    private DonationUpdateRequest updateRequest(String newName) {
        return new DonationUpdateRequest(
                newName, "Updated description", new BigDecimal("4"), "KG", FoodType.VEGETARIAN,
                dataFactory.category("cooked-meals").getId(),
                Instant.now().minus(30, ChronoUnit.MINUTES),
                Instant.now().plus(4, ChronoUnit.HOURS),
                "12 FC Road, Pune", "Pune", "Ring twice", "Refrigerate", "Dairy", "Clean containers");
    }

    @Test
    @DisplayName("A donor creates a donation and its derived fields are computed")
    void createDonation() {
        User donor = dataFactory.donor();
        DonationResponse created = donationService.create(TestPrincipals.of(donor), createRequest());

        assertThat(created.id()).isNotNull();
        assertThat(created.referenceCode()).startsWith("FB-");
        assertThat(created.status()).isEqualTo(DonationStatus.AVAILABLE);
        assertThat(created.estimatedMeals()).isPositive();
        assertThat(created.donorEmail()).isEqualTo(donor.getEmail()); // owner sees contact data
        assertThat(created.safetyDeclaration()).isTrue();
    }

    @Test
    @DisplayName("An NGO account cannot create a donation")
    void ngoCannotCreateDonation() {
        User ngo = dataFactory.approvedNgo();
        assertThatThrownBy(() -> donationService.create(TestPrincipals.of(ngo), createRequest()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("ROLE_REQUIRED"));
    }

    @Test
    @DisplayName("Expiry must be after preparation and inside the 48h window")
    void expiryWindowValidation() {
        User donor = dataFactory.donor();
        DonationCreateRequest reversed = new DonationCreateRequest(
                "X", null, BigDecimal.ONE, "KG", FoodType.VEGETARIAN,
                dataFactory.category("cooked-meals").getId(),
                Instant.now(), Instant.now().minusSeconds(60), "A", "Pune", null, null, null, null, true);
        assertThatThrownBy(() -> donationService.create(TestPrincipals.of(donor), reversed))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("INVALID_EXPIRY"));

        DonationCreateRequest tooLong = new DonationCreateRequest(
                "X", null, BigDecimal.ONE, "KG", FoodType.VEGETARIAN,
                dataFactory.category("cooked-meals").getId(),
                Instant.now(), Instant.now().plus(72, ChronoUnit.HOURS), "A", "Pune", null, null, null, null, true);
        assertThatThrownBy(() -> donationService.create(TestPrincipals.of(donor), tooLong))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("48 hours");
    }

    @Test
    @DisplayName("Only the owner can edit a donation, and only while AVAILABLE")
    void editRules() {
        User owner = dataFactory.donor();
        User other = dataFactory.donor();
        DonationResponse donation = donationService.create(TestPrincipals.of(owner), createRequest());

        DonationResponse updated = donationService.update(TestPrincipals.of(owner), donation.id(),
                updateRequest("Paneer Tikka Deluxe"));
        assertThat(updated.foodName()).isEqualTo("Paneer Tikka Deluxe");

        assertThatThrownBy(() -> donationService.update(TestPrincipals.of(other), donation.id(),
                updateRequest("Hijacked")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("A claimed donation cannot be edited or cancelled by the donor")
    void claimedDonationIsLocked() {
        User owner = dataFactory.donor();
        User ngo = dataFactory.approvedNgo();
        DonationResponse created = donationService.create(TestPrincipals.of(owner), createRequest());

        Donation donation = donationRepository.findById(created.id()).orElseThrow();
        donation.setStatus(DonationStatus.CLAIMED);
        donationRepository.save(donation);

        assertThatThrownBy(() -> donationService.update(TestPrincipals.of(owner), created.id(),
                updateRequest("Too late")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only available donations can be edited");

        assertThatThrownBy(() -> donationService.cancelByDonor(TestPrincipals.of(owner), created.id(),
                "changed my mind"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("CLAIM_ACTIVE"));
    }

    @Test
    @DisplayName("Cancelling an available donation moves it to CANCELLED")
    void cancelDonation() {
        User owner = dataFactory.donor();
        DonationResponse created = donationService.create(TestPrincipals.of(owner), createRequest());

        DonationResponse cancelled = donationService.cancelByDonor(TestPrincipals.of(owner), created.id(),
                "Food spoiled");
        assertThat(cancelled.status()).isEqualTo(DonationStatus.CANCELLED);
    }

    @Test
    @DisplayName("NGO browse returns only AVAILABLE, unexpired donations, sorted by nearest expiry")
    void browseOnlyShowsClaimable() {
        User donor = dataFactory.donor();
        DonationResponse soon = donationService.create(TestPrincipals.of(donor), createRequest());
        DonationResponse later = donationService.create(TestPrincipals.of(donor), new DonationCreateRequest(
                "Later meal", null, new BigDecimal("3"), "KG", FoodType.VEGETARIAN,
                dataFactory.category("cooked-meals").getId(),
                Instant.now().minusSeconds(600), Instant.now().plus(20, ChronoUnit.HOURS),
                "12 FC Road", "Pune", null, null, null, null, true));

        Donation claimed = donationRepository.findById(later.id()).orElseThrow();
        claimed.setStatus(DonationStatus.CLAIMED);
        donationRepository.save(claimed);

        Donation expired = donationRepository.save(dataFactory.donation(donor, Instant.now().minusSeconds(3600)));

        PageResponse<DonationSummaryResponse> page = donationService.browseAvailable(
                null, null, null, null, null, 0, 20, "expiresAt", "asc");

        assertThat(page.content()).extracting(DonationSummaryResponse::id).contains(soon.id());
        assertThat(page.content()).extracting(DonationSummaryResponse::id).doesNotContain(later.id(), expired.getId());
        assertThat(page.content()).allMatch(d -> d.status() == DonationStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Search and filters apply on the server side with pagination")
    void searchAndFilter() {
        User donor = dataFactory.donor();
        donationService.create(TestPrincipals.of(donor), createRequest());
        donationService.create(TestPrincipals.of(donor), new DonationCreateRequest(
                "Chocolate Cake", null, new BigDecimal("2"), "KG", FoodType.VEGETARIAN,
                dataFactory.category("bakery-bread").getId(),
                Instant.now().minusSeconds(600), Instant.now().plus(8, ChronoUnit.HOURS),
                "12 FC Road", "Pune", null, null, null, null, true));

        PageResponse<DonationSummaryResponse> byName = donationService.listForDonor(
                TestPrincipals.of(donor), null, null, "cake", null, null, 0, 10, "createdAt", "desc");
        assertThat(byName.content()).hasSize(1);
        assertThat(byName.content().get(0).foodName()).isEqualTo("Chocolate Cake");

        PageResponse<DonationSummaryResponse> byCategory = donationService.listForDonor(
                TestPrincipals.of(donor), null, dataFactory.category("bakery-bread").getId(),
                null, null, null, 0, 10, "createdAt", "desc");
        assertThat(byCategory.content()).hasSize(1);

        PageResponse<DonationSummaryResponse> paged = donationService.listForDonor(
                TestPrincipals.of(donor), null, null, null, null, null, 0, 1, "createdAt", "desc");
        assertThat(paged.content()).hasSize(1);
        assertThat(paged.totalElements()).isEqualTo(2);
        assertThat(paged.totalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("Donor statistics are derived from actual data")
    void donorStats() {
        User donor = dataFactory.donor();
        donationService.create(TestPrincipals.of(donor), createRequest());
        donationService.create(TestPrincipals.of(donor), createRequest());

        DonationStatsResponse stats = donationService.statsForDonor(donor.getId());
        assertThat(stats.totalDonations()).isEqualTo(2);
        assertThat(stats.available()).isEqualTo(2);
        assertThat(stats.completed()).isZero();
    }

    @Test
    @DisplayName("An unknown category is rejected")
    void unknownCategoryRejected() {
        User donor = dataFactory.donor();
        DonationCreateRequest request = new DonationCreateRequest(
                "X", null, BigDecimal.ONE, "KG", FoodType.VEGETARIAN, 999_999L,
                Instant.now().minusSeconds(60), Instant.now().plus(2, ChronoUnit.HOURS),
                "A", "Pune", null, null, null, null, true);
        assertThatThrownBy(() -> donationService.create(TestPrincipals.of(donor), request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("INVALID_CATEGORY"));
    }
}
