package com.foodbridge.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NgoProfileRepository extends JpaRepository<NgoProfile, Long> {

    Optional<NgoProfile> findByUserId(Long userId);

    boolean existsByRegistrationNumberIgnoreCase(String registrationNumber);

    java.util.List<NgoProfile> findAllByVerificationStatus(VerificationStatus status);

    long countByVerificationStatus(VerificationStatus status);

    @Query("""
            SELECT p FROM NgoProfile p JOIN FETCH p.user u
            WHERE (:status IS NULL OR p.verificationStatus = :status)
              AND (:search IS NULL
                   OR LOWER(p.organizationName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.registrationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<NgoProfile> search(@Param("status") VerificationStatus status,
                            @Param("search") String search,
                            Pageable pageable);
}
