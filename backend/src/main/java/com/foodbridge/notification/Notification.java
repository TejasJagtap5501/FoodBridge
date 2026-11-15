package com.foodbridge.notification;

import com.foodbridge.common.CreatedOnlyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Persistent notification. Live delivery happens over Server-Sent Events; this
 * table is the durable history a client can fetch after reconnecting.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification extends CreatedOnlyEntity {

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    /** Frontend route to open when the notification is clicked. */
    @Column(length = 255)
    private String link;

    @Column(name = "read_flag", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private Instant readAt;
}
