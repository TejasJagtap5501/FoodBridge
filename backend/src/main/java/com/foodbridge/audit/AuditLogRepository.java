package com.foodbridge.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            SELECT a FROM AuditLog a
             WHERE (:action IS NULL OR a.action = :action)
               AND (:search IS NULL
                    OR LOWER(a.actorEmail) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(a.entityType) LIKE LOWER(CONCAT('%', :search, '%')))
             ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> search(@Param("action") String action,
                          @Param("search") String search,
                          Pageable pageable);
}
