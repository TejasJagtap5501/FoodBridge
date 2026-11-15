package com.foodbridge.pickup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PickupRepository extends JpaRepository<Pickup, Long> {

    @Query("""
            SELECT p FROM Pickup p
              JOIN FETCH p.claim c
              JOIN FETCH c.donation d
              JOIN FETCH d.donor
              JOIN FETCH c.ngo
             WHERE p.id = :id
            """)
    Optional<Pickup> findDetailedById(@Param("id") Long id);

    @Query("""
            SELECT p FROM Pickup p
              JOIN FETCH p.claim c
              JOIN FETCH c.donation d
              JOIN FETCH c.ngo
             WHERE d.donor.id = :donorId
             ORDER BY p.scheduledAt DESC
            """)
    List<Pickup> findByDonor(@Param("donorId") Long donorId);

    @Query("""
            SELECT p FROM Pickup p
              JOIN FETCH p.claim c
              JOIN FETCH c.donation d
              JOIN FETCH d.donor
             WHERE c.ngo.id = :ngoId
             ORDER BY p.scheduledAt DESC
            """)
    List<Pickup> findByNgo(@Param("ngoId") Long ngoId);

    Optional<Pickup> findFirstByClaimIdOrderByCreatedAtDesc(Long claimId);

    long countByStatus(PickupStatus status);
}
