package com.foodbridge.notification;

import com.foodbridge.common.PageResponse;
import com.foodbridge.notification.dto.NotificationResponse;
import com.foodbridge.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * Notification history and the live SSE stream.
 *
 * <p>Every history method derives the recipient from the JWT - there is no user
 * id parameter anywhere, which structurally prevents cross-user access.</p>
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Persistent notification history and live SSE stream")
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterRegistry emitterRegistry;
    private final SseTokenValidator sseTokenValidator;

    @GetMapping
    @Operation(summary = "List my notifications (paginated)")
    public PageResponse<NotificationResponse> list(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return notificationService.list(CurrentUser.requireId(), pageable);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Number of unread notifications")
    public Map<String, Long> unreadCount() {
        return Map.of("count", notificationService.unreadCount(CurrentUser.requireId()));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark one notification as read")
    public void markRead(@PathVariable Long id) {
        notificationService.markRead(CurrentUser.requireId(), id);
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all my notifications as read")
    public Map<String, Integer> markAllRead() {
        return Map.of("updated", notificationService.markAllRead(CurrentUser.requireId()));
    }

    /**
     * Live stream. The access token is passed as a query parameter because the
     * native {@code EventSource} API cannot set an Authorization header. The URL
     * is permitted by the filter chain and the token is validated here.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Server-Sent Events stream of live notifications")
    public SseEmitter stream(@RequestParam("access_token") String accessToken) {
        Long userId = sseTokenValidator.userIdFor(accessToken);
        return emitterRegistry.register(userId);
    }
}
