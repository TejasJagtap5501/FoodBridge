package com.foodbridge.donation;

import com.foodbridge.config.FoodBridgeProperties;
import com.foodbridge.notification.NotificationService;
import com.foodbridge.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Background sweeper that keeps donation status truthful without human action.
 *
 * <p>Runs on a fixed delay (default 60 s) and does two things:</p>
 * <ol>
 *   <li><b>Expire</b> - any AVAILABLE donation whose expiry has passed becomes
 *       EXPIRED. The conditional UPDATE means a donation claimed in the same
 *       instant is never clobbered.</li>
 *   <li><b>Remind</b> - AVAILABLE donations expiring within the configured
 *       window notify their donor so food can still be rescued.</li>
 * </ol>
 *
 * <p>Claiming also re-checks {@code expires_at} in its own atomic statement, so
 * even if this job is delayed, an expired donation can never be claimed.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DonationExpiryJob {

    private static final int BATCH = 200;

    private final DonationRepository donationRepository;
    private final NotificationService notificationService;
    private final FoodBridgeProperties properties;

    @Scheduled(fixedDelayString = "${foodbridge.expiry.scan-interval-ms:60000}", initialDelay = 15_000)
    @Transactional
    public void sweep() {
        Instant now = Instant.now();
        List<Donation> expired = donationRepository.findExpiredAvailable(now, PageRequest.of(0, BATCH));
        int transitioned = 0;
        for (Donation donation : expired) {
            int updated = donationRepository.transition(donation.getId(),
                    DonationStatus.AVAILABLE, DonationStatus.EXPIRED);
            if (updated > 0) {
                transitioned++;
                notificationService.create(donation.getDonor().getId(), NotificationType.DONATION_EXPIRED,
                        "Donation expired",
                        "\"" + donation.getFoodName() + "\" expired without being claimed and has been marked as expired.",
                        "/donor/donations/" + donation.getId());
            }
        }
        if (transitioned > 0) {
            log.info("Expiry sweep: {} donation(s) marked EXPIRED", transitioned);
        }

        int reminderHours = properties.getExpiry().getReminderHoursBefore();
        Instant threshold = now.plus(Duration.ofHours(reminderHours));
        List<Donation> expiringSoon = donationRepository.findExpiringSoon(now, threshold, PageRequest.of(0, BATCH));
        for (Donation donation : expiringSoon) {
            notificationService.create(donation.getDonor().getId(), NotificationType.DONATION_EXPIRING,
                    "Donation expiring soon",
                    "\"" + donation.getFoodName() + "\" expires in less than " + reminderHours + " hours.",
                    "/donor/donations/" + donation.getId());
        }
    }

    /** Exposed for tests and the admin maintenance endpoint. */
    @Transactional
    public int expireNow() {
        Instant now = Instant.now();
        int total = 0;
        List<Donation> expired = donationRepository.findExpiredAvailable(now, PageRequest.of(0, BATCH));
        for (Donation donation : expired) {
            total += donationRepository.transition(donation.getId(),
                    DonationStatus.AVAILABLE, DonationStatus.EXPIRED);
        }
        return total;
    }
}
