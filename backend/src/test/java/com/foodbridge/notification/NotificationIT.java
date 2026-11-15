package com.foodbridge.notification;

import com.foodbridge.claim.ClaimService;
import com.foodbridge.claim.dto.ClaimRequest;
import com.foodbridge.exception.ApiException;
import com.foodbridge.support.IntegrationTestBase;
import com.foodbridge.support.TestPrincipals;
import com.foodbridge.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.*;

/**
 * Notifications must be persisted, addressed only to the right recipient, and
 * readable only by that recipient.
 */
class NotificationIT extends IntegrationTestBase {

    @Autowired private NotificationService notificationService;
    @Autowired private ClaimService claimService;

    @Test
    @DisplayName("A donation claim notifies the donor, not the NGO, and is persisted")
    void claimNotifiesDonor() {
        User donor = dataFactory.donor();
        User ngo = dataFactory.approvedNgo();
        var donation = donationRepository.save(dataFactory.donation(donor));

        claimService.claim(TestPrincipals.of(ngo), donation.getId(), new ClaimRequest("hi", true));

        var donorNotifications = notificationService.list(donor.getId(), PageRequest.of(0, 20));
        assertThat(donorNotifications.content())
                .anyMatch(n -> n.type() == NotificationType.DONATION_CLAIMED);

        var ngoNotifications = notificationService.list(ngo.getId(), PageRequest.of(0, 20));
        assertThat(ngoNotifications.content())
                .noneMatch(n -> n.type() == NotificationType.DONATION_CLAIMED
                        && n.message().contains(ngo.getFullName()));
    }

    @Test
    @DisplayName("Unread count and mark-all-read work per user")
    void unreadCounters() {
        User donor = dataFactory.donor();
        User ngo = dataFactory.approvedNgo();
        var donation = donationRepository.save(dataFactory.donation(donor));
        claimService.claim(TestPrincipals.of(ngo), donation.getId(), new ClaimRequest("hi", true));

        long donorUnread = notificationService.unreadCount(donor.getId());
        assertThat(donorUnread).isGreaterThan(0);
        assertThat(notificationService.unreadCount(ngo.getId())).isZero();

        notificationService.markAllRead(donor.getId());
        assertThat(notificationService.unreadCount(donor.getId())).isZero();
    }

    @Test
    @DisplayName("A user cannot mark another user's notification as read")
    void cannotReadForeignNotification() {
        User donor = dataFactory.donor();
        User other = dataFactory.donor();
        var donation = donationRepository.save(dataFactory.donation(donor));
        User ngo = dataFactory.approvedNgo();
        claimService.claim(TestPrincipals.of(ngo), donation.getId(), new ClaimRequest("hi", true));

        var donorNotification = notificationService.list(donor.getId(), PageRequest.of(0, 1)).content().get(0);

        assertThatThrownBy(() -> notificationService.markRead(other.getId(), donorNotification.id()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("NOTIFICATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("Users only ever see their own notification history")
    void historyIsScopedToRecipient() {
        User donor = dataFactory.donor();
        User ngo = dataFactory.approvedNgo();
        var donation = donationRepository.save(dataFactory.donation(donor));
        claimService.claim(TestPrincipals.of(ngo), donation.getId(), new ClaimRequest("hi", true));

        var ngoHistory = notificationService.list(ngo.getId(), PageRequest.of(0, 50));
        assertThat(ngoHistory.content()).allMatch(n -> {
            var stored = notificationRepository.findById(n.id()).orElseThrow();
            return stored.getRecipientId().equals(ngo.getId());
        });
    }
}
