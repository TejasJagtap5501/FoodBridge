package com.foodbridge.claim;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    @Query("""
            SELECT c FROM Claim c
              JOIN FETCH c.donation d
              JOIN FETCH d.category
              JOIN FETCH d.donor
              JOIN FETCH c.ngo
             WHERE c.id = :id
            """)
    Optional<Claim> findDetailedById(@Param("id") Long id);

    @Query("""
            SELECT c FROM Claim c
              JOIN FETCH c.donation d
              JOIN FETCH d.category
              JOIN FETCH d.donor
             WHERE c.ngo.id = :ngoId
               AND (:status IS NULL OR c.status = :status)
             ORDER BY c.claimedAt DESC
            """)
    Page<Claim> findByNgo(@Param("ngoId") Long ngoId,
                          @Param("status") ClaimStatus status,
                          Pageable pageable);

    @Query("""
            SELECT c FROM Claim c
              JOIN FETCH c.donation d
              JOIN FETCH d.category
              JOIN FETCH c.ngo
             WHERE d.donor.id = :donorId
               AND (:status IS NULL OR c.status = :status)
             ORDER BY c.claimedAt DESC
            """)
    Page<Claim> findByDonor(@Param("donorId") Long donorId,
                            @Param("status") ClaimStatus status,
                            Pageable pageable);

    @Query("""
            SELECT c FROM Claim c
              JOIN FETCH c.donation d
              JOIN FETCH d.category
              JOIN FETCH d.donor
              JOIN FETCH c.ngo
             WHERE (:status IS NULL OR c.status = :status)
               AND (:search IS NULL
                    OR LOWER(d.foodName) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(d.referenceCode) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Claim> searchAll(@Param("status") ClaimStatus status,
                          @Param("search") String search,
                          Pageable pageable);

    Optional<Claim> findByDonationIdAndStatus(Long donationId, ClaimStatus status);

    List<Claim> findByDonationIdOrderByClaimedAtDesc(Long donationId);

    long countByNgoId(Long ngoId);

    long countByNgoIdAndStatus(Long ngoId, ClaimStatus status);

    long countByStatus(ClaimStatus status);
}
