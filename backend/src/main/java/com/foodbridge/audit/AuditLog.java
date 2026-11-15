package com.foodbridge.audit;

import com.foodbridge.common.CreatedOnlyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Append-only audit trail for sensitive actions (NGO verification decisions,
 * account activation/deactivation, donation moderation, category changes).
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog extends CreatedOnlyEntity {

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_email", length = 160)
    private String actorEmail;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 48)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(length = 1000)
    private String details;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;
}
