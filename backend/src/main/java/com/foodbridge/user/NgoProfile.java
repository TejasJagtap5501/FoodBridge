package com.foodbridge.user;

import com.foodbridge.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Profile of an NGO. Carries the verification workflow state; only NGOs whose
 * {@code verificationStatus} is {@code APPROVED} may claim donations.
 */
@Entity
@Table(name = "ngo_profiles")
@Getter
@Setter
@NoArgsConstructor
public class NgoProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "organization_name", nullable = false, length = 160)
    private String organizationName;

    @Column(name = "registration_number", nullable = false, length = 64, unique = true)
    private String registrationNumber;

    @Column(name = "contact_person", nullable = false, length = 120)
    private String contactPerson;

    @Column(length = 200)
    private String website;

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

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 16)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "verification_note", length = 500)
    private String verificationNote;

    @Column(name = "verified_by")
    private Long verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    public boolean isApproved() {
        return verificationStatus == VerificationStatus.APPROVED;
    }
}
