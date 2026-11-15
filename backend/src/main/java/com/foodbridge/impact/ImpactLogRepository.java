package com.foodbridge.impact;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface ImpactLogRepository extends JpaRepository<ImpactLog, Long> {

    Optional<ImpactLog> findByDonationId(Long donationId);

    long countByDonorId(Long donorId);

    long countByNgoId(Long ngoId);

    @Query("SELECT COALESCE(SUM(i.mealsServed), 0) FROM ImpactLog i WHERE i.donorId = :donorId")
    long sumMealsByDonor(@Param("donorId") Long donorId);

    @Query("SELECT COALESCE(SUM(i.mealsServed), 0) FROM ImpactLog i WHERE i.ngoId = :ngoId")
    long sumMealsByNgo(@Param("ngoId") Long ngoId);

    @Query("SELECT COALESCE(SUM(i.mealsServed), 0) FROM ImpactLog i")
    long sumMealsPlatform();

    @Query("SELECT COUNT(i) FROM ImpactLog i WHERE i.completedAt >= :since")
    long countSince(@Param("since") Instant since);
}
