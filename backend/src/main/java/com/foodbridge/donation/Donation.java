package com.foodbridge.donation;

import com.foodbridge.common.BaseEntity;
import com.foodbridge.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A surplus-food offer published by a donor.
 *
 * <p>The {@code status} field is the single source of truth for the donation
 * lifecycle and is only ever changed by {@link DonationService} following the
 * rules in {@link DonationStatus}. The {@code version} column provides
 * optimistic locking as a second line of defence against lost updates.</p>
 */
@Entity
@Table(name = "donations")
@Getter
@Setter
@NoArgsConstructor
public class Donation extends BaseEntity {

    @Column(name = "reference_code", nullable = false, unique = true, length = 24)
    private String referenceCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_id", nullable = false)
    private User donor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private FoodCategory category;

    @Column(name = "food_name", nullable = false, length = 160)
    private String foodName;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "quantity_unit", nullable = false, length = 16)
    private String quantityUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "food_type", nullable = false, length = 16)
    private FoodType foodType;

    @Column(name = "prepared_at", nullable = false)
    private Instant preparedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "pickup_address", nullable = false, length = 300)
    private String pickupAddress;

    @Column(nullable = false, length = 80)
    private String city;

    /** Reserved for future map integration. */
    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "pickup_instructions", length = 500)
    private String pickupInstructions;

    @Column(name = "storage_conditions", length = 255)
    private String storageConditions;

    @Column(name = "allergen_info", length = 500)
    private String allergenInfo;

    @Column(name = "handling_instructions", length = 500)
    private String handlingInstructions;

    /** Donor attests that the food information provided is accurate. */
    @Column(name = "safety_declaration", nullable = false)
    private boolean safetyDeclaration = false;

    @Column(name = "estimated_meals", nullable = false)
    private int estimatedMeals = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DonationStatus status = DonationStatus.AVAILABLE;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToMany(mappedBy = "donation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DonationImage> images = new ArrayList<>();

    public void addImage(DonationImage image) {
        image.setDonation(this);
        images.add(image);
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
