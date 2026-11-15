package com.foodbridge.user;

import com.foodbridge.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Profile of a food donor: restaurant, hotel, cafe, bakery or any food business.
 */
@Entity
@Table(name = "donor_profiles")
@Getter
@Setter
@NoArgsConstructor
public class DonorProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "business_name", nullable = false, length = 160)
    private String businessName;

    /** RESTAURANT, HOTEL, CAFE, BAKERY, CATERING, OTHER */
    @Column(name = "business_type", nullable = false, length = 32)
    private String businessType;

    @Column(name = "contact_person", nullable = false, length = 120)
    private String contactPerson;

    @Column(name = "address_line1", nullable = false, length = 200)
    private String addressLine1;

    @Column(name = "address_line2", length = 200)
    private String addressLine2;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false, length = 80)
    private String state;

    @Column(name = "postal_code", nullable = false, length = 16)
    private String postalCode;

    /** Reserved for future map integration. */
    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "fssai_license", length = 32)
    private String fssaiLicense;
}
