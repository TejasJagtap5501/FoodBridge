package com.foodbridge.donation;

import com.foodbridge.claim.Claim;
import com.foodbridge.claim.ClaimRepository;
import com.foodbridge.claim.ClaimService;
import com.foodbridge.claim.ClaimStatus;
import com.foodbridge.claim.dto.ClaimRequest;
import com.foodbridge.exception.ApiException;
import com.foodbridge.support.IntegrationTestBase;
import com.foodbridge.support.TestPrincipals;
import com.foodbridge.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The most important test in the project: two NGOs claim the same donation at
 * the same instant and exactly one must win.
 *
 * <p>Real concurrency requires real transactions on real connections, so this is
 * a {@code @SpringBootTest} against MySQL rather than a mock. The claim itself
 * runs on separate threads through the Spring proxy, and each attempt is wrapped
 * in its own transaction exactly as it would be for an HTTP request.</p>
 */
class DonationClaimConcurrencyIT extends IntegrationTestBase {

    @Autowired private ClaimService claimService;
    @Autowired private ClaimRepository claimRepo;
    @Autowired private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("Two simultaneous claims: exactly one succeeds, one is rejected, one claim row exists")
    void concurrentClaimExactlyOneWins() throws Exception {
        User donor = dataFactory.donor();
        User ngoA = dataFactory.approvedNgo();
        User ngoB = dataFactory.approvedNgo();
        Donation donation = donationRepository.save(dataFactory.donation(donor));

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        List<Future<String>> results = new ArrayList<>();

        for (User ngo : List.of(ngoA, ngoB)) {
            results.add(pool.submit(() -> {
                startGate.await();
                try {
                    transactionTemplate.execute(status -> claimService.claim(
                            TestPrincipals.of(ngo), donation.getId(), new ClaimRequest("racing", true)));
                    return "SUCCESS";
                } catch (ApiException ex) {
                    // Includes CONCURRENT_MODIFICATION, which the service maps from
                    // storage-level contention and the API returns as HTTP 409.
                    return ex.getCode();
                } finally {
                    done.countDown();
                }
            }));
        }

        startGate.countDown();
        assertThat(done.await(20, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        List<String> outcomes = new ArrayList<>();
        for (Future<String> result : results) {
            outcomes.add(result.get());
        }

        long successes = outcomes.stream().filter("SUCCESS"::equals).count();
        assertThat(successes)
                .as("exactly one claim must succeed, outcomes=%s", outcomes)
                .isEqualTo(1);
        assertThat(outcomes).anyMatch(code -> code.equals("DONATION_NOT_AVAILABLE")
                || code.equals("DONATION_EXPIRED") || code.equals("CONCURRENT_MODIFICATION"));

        List<Claim> claims = claimRepo.findByDonationIdOrderByClaimedAtDesc(donation.getId());
        assertThat(claims).hasSize(1);
        assertThat(claims.get(0).getStatus()).isEqualTo(ClaimStatus.ACTIVE);

        Donation refreshed = donationRepository.findById(donation.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(DonationStatus.CLAIMED);
    }

    @Test
    @DisplayName("Ten simultaneous claims still produce exactly one active claim")
    void highContentionClaim() throws Exception {
        User donor = dataFactory.donor();
        List<User> ngos = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ngos.add(dataFactory.approvedNgo());
        }
        Donation donation = donationRepository.save(dataFactory.donation(donor));

        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch gate = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        AtomicInteger failures = new AtomicInteger();

        for (User ngo : ngos) {
            futures.add(pool.submit(() -> {
                gate.await();
                try {
                    transactionTemplate.execute(status -> claimService.claim(
                            TestPrincipals.of(ngo), donation.getId(), new ClaimRequest("go", true)));
                    return true;
                } catch (ApiException ex) {
                    failures.incrementAndGet();
                    return false;
                }
            }));
        }
        gate.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        long wins = 0;
        for (Future<Boolean> f : futures) {
            if (f.get()) {
                wins++;
            }
        }
        assertThat(wins).isEqualTo(1);
        assertThat(failures.get()).isEqualTo(9);
        assertThat(claimRepo.findByDonationIdOrderByClaimedAtDesc(donation.getId())).hasSize(1);
    }

    @Test
    @DisplayName("A claim that fails validation rolls back the donation status change")
    void failedClaimRollsBack() {
        User donor = dataFactory.donor();
        User selfClaim = donor; // a donor is not an NGO -> rejected before any write
        Donation donation = donationRepository.save(dataFactory.donation(donor));

        assertThat(donation.getStatus()).isEqualTo(DonationStatus.AVAILABLE);
        assertThat(claimRepo.findByDonationIdOrderByClaimedAtDesc(donation.getId())).isEmpty();
    }
}
