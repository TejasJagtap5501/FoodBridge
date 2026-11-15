package com.foodbridge.notification;

import com.foodbridge.common.PageResponse;
import com.foodbridge.exception.ApiException;
import com.foodbridge.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Creates persistent notifications and pushes them to connected SSE clients.
 *
 * <p>Every notification is written to the database first, so a client that was
 * offline during a push still sees it in its history when it reconnects. The
 * live push is best-effort and never fails the business operation.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository repository;
    private final SseEmitterRegistry emitterRegistry;

    /**
     * Persist and live-push a notification.
     *
     * <p>{@code REQUIRES_NEW} would be wrong here: the notification must be part
     * of the surrounding business transaction so it is rolled back together with
     * a failed operation. The SSE push is therefore dispatched only after the
     * record is safely persisted in the same transaction (the emitter receives it
     * immediately, which is acceptable because a rollback after commit-time
     * failure is extremely unlikely and the history remains authoritative).</p>
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Notification create(Long recipientId, NotificationType type, String title, String message, String link) {
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLink(link);
        Notification saved = repository.save(notification);
        push(saved);
        return saved;
    }

    /**
     * Variant usable outside an existing transaction (e.g. the expiry sweep).
     */
    @Transactional
    public Notification createStandalone(Long recipientId, NotificationType type, String title,
                                         String message, String link) {
        return create(recipientId, type, title, message, link);
    }

    private void push(Notification notification) {
        try {
            emitterRegistry.send(notification.getRecipientId(), NotificationResponse.from(notification));
        } catch (RuntimeException ex) {
            log.debug("SSE push failed for user {}: {}", notification.getRecipientId(), ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Long recipientId, Pageable pageable) {
        Page<Notification> page = repository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable);
        return PageResponse.of(page, NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long recipientId) {
        return repository.countByRecipientIdAndReadFalse(recipientId);
    }

    @Transactional
    public void markRead(Long recipientId, Long notificationId) {
        Notification notification = repository.findByIdAndRecipientId(notificationId, recipientId)
                .orElseThrow(() -> ApiException.notFound("NOTIFICATION_NOT_FOUND", "Notification not found"));
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            repository.save(notification);
        }
    }

    @Transactional
    public int markAllRead(Long recipientId) {
        return repository.markAllRead(recipientId, Instant.now());
    }

    /**
     * Non-blocking fan-out helper used by the demo seeder and admin broadcast.
     */
    @Async
    public void broadcastToAll(Iterable<Long> recipientIds, NotificationType type,
                               String title, String message, String link) {
        recipientIds.forEach(id -> createStandalone(id, type, title, message, link));
    }
}
