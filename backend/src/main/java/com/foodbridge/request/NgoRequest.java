package com.foodbridge.request;

import com.foodbridge.common.BaseEntity;
import com.foodbridge.donation.FoodCategory;
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

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A food requirement published by an NGO so donors can target their donations.
 */
@Entity
@Table(name = "ngo_requests")
@Getter
@Setter
@NoArgsConstructor
public class NgoRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ngo_id", nullable = false)
    private User ngo;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private FoodCategory category;

    @Column(name = "quantity_needed", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityNeeded;

    @Column(name = "quantity_unit", nullable = false, length = 16)
    private String quantityUnit;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(name = "needed_by", nullable = false)
    private Instant neededBy;

    @Column(nullable = false)
    private int beneficiaries = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NgoRequestStatus status = NgoRequestStatus.OPEN;
}
