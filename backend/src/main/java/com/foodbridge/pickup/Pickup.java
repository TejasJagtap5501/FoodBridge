package com.foodbridge.pickup;

import com.foodbridge.claim.Claim;
import com.foodbridge.common.BaseEntity;
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
 * Pickup coordination record for a claim.
 *
 * <p>Two-party confirmation: {@code donorConfirmedAt} is set by the donor when
 * the food is handed over, {@code ngoConfirmedAt} by the NGO when it collects
 * the food. When both are present the pickup is {@code COMPLETED} and the
 * donation is completed by the service layer.</p>
 */
@Entity
@Table(name = "pickups")
@Getter
@Setter
@NoArgsConstructor
public class Pickup extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PickupStatus status = PickupStatus.SCHEDULED;

    @Column(length = 500)
    private String instructions;

    @Column(name = "donor_confirmed_at")
    private Instant donorConfirmedAt;

    @Column(name = "ngo_confirmed_at")
    private Instant ngoConfirmedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_reason", length = 300)
    private String cancelledReason;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    public boolean hasDonorConfirmed() {
        return donorConfirmedAt != null;
    }

    public boolean hasNgoConfirmed() {
        return ngoConfirmedAt != null;
    }

    public boolean isFullyConfirmed() {
        return hasDonorConfirmed() && hasNgoConfirmed();
    }
}
