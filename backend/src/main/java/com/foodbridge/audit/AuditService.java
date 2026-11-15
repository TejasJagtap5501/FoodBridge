package com.foodbridge.audit;

import com.foodbridge.common.PageResponse;
import com.foodbridge.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Writes the audit trail. Every administrative mutation calls this so that
 * "who changed what, when" can be reconstructed later.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuthenticatedUser actor, AuditAction action, String entityType, Long entityId,
                       String details, String ipAddress) {
        AuditLog entry = new AuditLog();
        if (actor != null) {
            entry.setActorId(actor.id());
            entry.setActorEmail(actor.email());
        }
        entry.setAction(action.getCode());
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(truncate(details));
        entry.setIpAddress(ipAddress);
        repository.save(entry);
        log.info("AUDIT {} {} #{} by {}", action.getCode(), entityType, entityId,
                actor == null ? "system" : actor.email());
    }

    @Transactional
    public void recordStandalone(AuthenticatedUser actor, AuditAction action, String entityType,
                                 Long entityId, String details, String ipAddress) {
        record(actor, action, entityType, entityId, details, ipAddress);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLog> search(String action, String search, Pageable pageable) {
        return PageResponse.of(repository.search(action, search, pageable));
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 1000 ? value : value.substring(0, 997) + "...";
    }

    /** Convenience for scheduled jobs that run without a request context. */
    public void recordSystem(AuditAction action, String entityType, Long entityId, String details) {
        recordStandalone(null, action, entityType, entityId, details, null);
    }

    public Instant now() {
        return Instant.now();
    }
}
