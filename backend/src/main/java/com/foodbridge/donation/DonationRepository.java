package com.foodbridge.donation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DonationRepository extends JpaRepository<Donation, Long>, JpaSpecificationExecutor<Donation> {

    Optional<Donation> findByReferenceCode(String referenceCode);

    @Query("SELECT d FROM Donation d JOIN FETCH d.donor JOIN FETCH d.category WHERE d.id = :id")
    Optional<Donation> findDetailedById(@Param("id") Long id);

    long countByDonorId(Long donorId);

    long countByDonorIdAndStatus(Long donorId, DonationStatus status);

    long countByStatus(DonationStatus status);

    /**
     * Atomically flips an AVAILABLE, non-expired donation to CLAIMED.
     *
     * <p>This single SQL statement is the heart of the concurrency guarantee:
     * the {@code WHERE status = 'AVAILABLE' AND expires_at > :now} predicate is
     * evaluated by InnoDB under a row lock, so when two NGOs claim at the same
     * instant exactly one UPDATE reports one affected row and the other reports
     * zero. No application-level lock is required and no read-then-write race is
     * possible.</p>
     *
     * @return number of rows updated (1 = claim won, 0 = lost/invalid)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Donation d
               SET d.status = com.foodbridge.donation.DonationStatus.CLAIMED
             WHERE d.id = :donationId
               AND d.status = com.foodbridge.donation.DonationStatus.AVAILABLE
               AND d.expiresAt > :now
            """)
    int claimAtomically(@Param("donationId") Long donationId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Donation d
               SET d.status = com.foodbridge.donation.DonationStatus.AVAILABLE
             WHERE d.id = :donationId
               AND d.status = com.foodbridge.donation.DonationStatus.CLAIMED
            """)
    int releaseToAvailable(@Param("donationId") Long donationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Donation d
               SET d.status = :target
             WHERE d.id = :donationId
               AND d.status = :expected
            """)
    int transition(@Param("donationId") Long donationId,
                   @Param("expected") DonationStatus expected,
                   @Param("target") DonationStatus target);

    /** Finds AVAILABLE donations whose expiry has passed - used by the sweep job. */
    @Query("""
            SELECT d FROM Donation d
             WHERE d.status = com.foodbridge.donation.DonationStatus.AVAILABLE
               AND d.expiresAt <= :now
            """)
    List<Donation> findExpiredAvailable(@Param("now") Instant now, Pageable pageable);

    /** AVAILABLE donations approaching expiry, for reminder notifications. */
    @Query("""
            SELECT d FROM Donation d
             WHERE d.status = com.foodbridge.donation.DonationStatus.AVAILABLE
               AND d.expiresAt > :now
               AND d.expiresAt <= :threshold
            """)
    List<Donation> findExpiringSoon(@Param("now") Instant now,
                                   @Param("threshold") Instant threshold,
                                   Pageable pageable);

    @Query("""
            SELECT d FROM Donation d JOIN FETCH d.category
             WHERE d.status = com.foodbridge.donation.DonationStatus.AVAILABLE
               AND d.expiresAt > :now
               AND (:city IS NULL OR LOWER(d.city) = LOWER(:city))
               AND (:categoryId IS NULL OR d.category.id = :categoryId)
               AND (:minQuantity IS NULL OR d.quantity >= :minQuantity)
             ORDER BY d.expiresAt ASC
            """)
    Page<Donation> findAvailableForNgo(@Param("now") Instant now,
                                       @Param("city") String city,
                                       @Param("categoryId") Long categoryId,
                                       @Param("minQuantity") BigDecimal minQuantity,
                                       Pageable pageable);
}
