package com.foodbridge.support;

import com.foodbridge.claim.ClaimRepository;
import com.foodbridge.donation.DonationRepository;
import com.foodbridge.impact.ImpactLogRepository;
import com.foodbridge.notification.NotificationRepository;
import com.foodbridge.pickup.PickupRepository;
import com.foodbridge.request.NgoRequestRepository;
import com.foodbridge.user.DonorProfileRepository;
import com.foodbridge.user.NgoProfileRepository;
import com.foodbridge.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for tests that need the real schema.
 *
 * <p>Uses a dedicated MySQL database ({@code foodbridge_test}) and Flyway runs
 * the exact same migrations as production, so schema drift is caught by the
 * test suite. Tables are truncated before each test to keep tests isolated
 * without the overhead of recreating the schema.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Autowired protected UserRepository userRepository;
    @Autowired protected DonorProfileRepository donorProfileRepository;
    @Autowired protected NgoProfileRepository ngoProfileRepository;
    @Autowired protected DonationRepository donationRepository;
    @Autowired protected ClaimRepository claimRepository;
    @Autowired protected PickupRepository pickupRepository;
    @Autowired protected NotificationRepository notificationRepository;
    @Autowired protected NgoRequestRepository ngoRequestRepository;
    @Autowired protected ImpactLogRepository impactLogRepository;
    @Autowired protected TestDataFactory dataFactory;

    @BeforeEach
    void cleanDatabase() {
        // Order matters: children before parents (FK constraints).
        impactLogRepository.deleteAllInBatch();
        pickupRepository.deleteAllInBatch();
        claimRepository.deleteAllInBatch();
        notificationRepository.deleteAllInBatch();
        ngoRequestRepository.deleteAllInBatch();
        donationRepository.deleteAllInBatch();
        ngoProfileRepository.deleteAllInBatch();
        donorProfileRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        dataFactory.reset();
    }
}
