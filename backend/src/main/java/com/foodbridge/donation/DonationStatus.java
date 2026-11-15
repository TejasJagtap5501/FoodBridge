package com.foodbridge.donation;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Donation lifecycle.
 *
 * <pre>
 *  AVAILABLE ──claim──> CLAIMED ──pickup completed──> COMPLETED
 *      │                   │
 *      │                   └──claim cancelled by NGO / donor / admin──> CANCELLED
 *      ├──expiry sweep──> EXPIRED
 *      └──donor/admin cancel──> CANCELLED
 * </pre>
 *
 * <p>{@code COMPLETED}, {@code EXPIRED} and {@code CANCELLED} are terminal.</p>
 */
public enum DonationStatus {
    AVAILABLE,
    CLAIMED,
    COMPLETED,
    EXPIRED,
    CANCELLED;

    private static final Map<DonationStatus, Set<DonationStatus>> ALLOWED = Map.of(
            AVAILABLE, EnumSet.of(CLAIMED, EXPIRED, CANCELLED),
            CLAIMED, EnumSet.of(COMPLETED, CANCELLED),
            COMPLETED, EnumSet.noneOf(DonationStatus.class),
            EXPIRED, EnumSet.noneOf(DonationStatus.class),
            CANCELLED, EnumSet.noneOf(DonationStatus.class)
    );

    public boolean canTransitionTo(DonationStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == EXPIRED || this == CANCELLED;
    }
}
