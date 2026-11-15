package com.foodbridge.claim;

import com.foodbridge.common.BaseEntity;
import com.foodbridge.donation.Donation;
import com.foodbridge.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * An NGO's claim on a donation.
 *
 * <p>The database guarantees at most one ACTIVE claim per donation through the
 * generated column {@code active_donation_id} + unique constraint
 * {@code uk_claims_active_donation}, in addition to the conditional UPDATE in
 * {@link com.foodbridge.donation.DonationRepository#claimAtomically}.</p>
 *
 * <p>Cancelled/completed claims are kept forever as claim history.</p>
 */
@Entity
@Table(name = "claims")
@Getter
@Setter
@NoArgsConstructor
public class Claim extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donation_id", nullable = false)
    private Donation donation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ngo_id", nullable = false)
    private User ngo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ClaimStatus status = ClaimStatus.ACTIVE;

    @Column(length = 500)
    private String message;

    /** NGO confirms it independently assessed the food's suitability. */
    @Column(name = "suitability_check", nullable = false)
    private boolean suitabilityCheck = false;

    @Column(name = "claimed_at", nullable = false)
    private Instant claimedAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "cancelled_reason", length = 300)
    private String cancelledReason;
}
