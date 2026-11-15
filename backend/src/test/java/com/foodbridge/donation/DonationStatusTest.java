package com.foodbridge.donation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The donation lifecycle is a state machine; these tests pin down every legal
 * and illegal transition so a future refactor cannot silently weaken it.
 */
class DonationStatusTest {

    @Test
    @DisplayName("Legal transitions")
    void legalTransitions() {
        assertThat(DonationStatus.AVAILABLE.canTransitionTo(DonationStatus.CLAIMED)).isTrue();
        assertThat(DonationStatus.AVAILABLE.canTransitionTo(DonationStatus.EXPIRED)).isTrue();
        assertThat(DonationStatus.AVAILABLE.canTransitionTo(DonationStatus.CANCELLED)).isTrue();
        assertThat(DonationStatus.CLAIMED.canTransitionTo(DonationStatus.COMPLETED)).isTrue();
        assertThat(DonationStatus.CLAIMED.canTransitionTo(DonationStatus.CANCELLED)).isTrue();
    }

    @Test
    @DisplayName("Illegal transitions are refused")
    void illegalTransitions() {
        assertThat(DonationStatus.AVAILABLE.canTransitionTo(DonationStatus.COMPLETED)).isFalse();
        assertThat(DonationStatus.CLAIMED.canTransitionTo(DonationStatus.AVAILABLE)).isFalse();
        assertThat(DonationStatus.CLAIMED.canTransitionTo(DonationStatus.EXPIRED)).isFalse();
        assertThat(DonationStatus.COMPLETED.canTransitionTo(DonationStatus.CANCELLED)).isFalse();
        assertThat(DonationStatus.EXPIRED.canTransitionTo(DonationStatus.CLAIMED)).isFalse();
        assertThat(DonationStatus.CANCELLED.canTransitionTo(DonationStatus.AVAILABLE)).isFalse();
    }

    @Test
    @DisplayName("Terminal states have no outgoing transitions")
    void terminalStates() {
        assertThat(DonationStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(DonationStatus.EXPIRED.isTerminal()).isTrue();
        assertThat(DonationStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(DonationStatus.AVAILABLE.isTerminal()).isFalse();
        assertThat(DonationStatus.CLAIMED.isTerminal()).isFalse();
    }
}
