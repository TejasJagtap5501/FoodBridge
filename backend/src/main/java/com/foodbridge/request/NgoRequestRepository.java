package com.foodbridge.request;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NgoRequestRepository extends JpaRepository<NgoRequest, Long> {

    @Query("""
            SELECT r FROM NgoRequest r LEFT JOIN FETCH r.category
             WHERE r.ngo.id = :ngoId
               AND (:status IS NULL OR r.status = :status)
             ORDER BY r.createdAt DESC
            """)
    Page<NgoRequest> findByNgo(@Param("ngoId") Long ngoId,
                               @Param("status") NgoRequestStatus status,
                               Pageable pageable);

    @Query("""
            SELECT r FROM NgoRequest r LEFT JOIN FETCH r.category JOIN FETCH r.ngo
             WHERE (:status IS NULL OR r.status = :status)
               AND (:city IS NULL OR LOWER(r.city) = LOWER(:city))
             ORDER BY r.neededBy ASC
            """)
    Page<NgoRequest> search(@Param("status") NgoRequestStatus status,
                            @Param("city") String city,
                            Pageable pageable);

    Optional<NgoRequest> findByIdAndNgoId(Long id, Long ngoId);

    long countByNgoIdAndStatus(Long ngoId, NgoRequestStatus status);
}
