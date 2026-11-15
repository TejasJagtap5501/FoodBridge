package com.foodbridge.impact;

import com.foodbridge.common.CreatedOnlyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable record written once when a donation is completed. Keeping this
 * separate from the mutable donation row makes platform impact reporting cheap
 * and gives an auditable history of rescued food.
 */
@Entity
@Table(name = "impact_logs")
@Getter
@Setter
@NoArgsConstructor
public class ImpactLog extends CreatedOnlyEntity {

    @Column(name = "donation_id", nullable = false, unique = true)
    private Long donationId;

    @Column(name = "donor_id", nullable = false)
    private Long donorId;

    @Column(name = "ngo_id", nullable = false)
    private Long ngoId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "quantity_unit", nullable = false, length = 16)
    private String quantityUnit;

    @Column(name = "meals_served", nullable = false)
    private int mealsServed;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;
}
