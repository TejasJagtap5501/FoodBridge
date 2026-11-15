package com.foodbridge.notification;

import com.foodbridge.notification.dto.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * In-memory registry of open Server-Sent Event emitters, keyed by user id.
 *
 * <p>Authorization: an emitter can only be created for the authenticated user
 * id resolved from the JWT, and {@link #send} addresses exactly one user id, so
 * a client can never receive another user's notifications.</p>
 *
 * <p>Resilience: emits a heartbeat every 25 seconds so proxies do not idle the
 * connection out; dead emitters are removed on write failure or timeout, and
 * the browser's native {@code EventSource} reconnection + the persistent
 * notification history cover the gap.</p>
 */
@Component
@Slf4j
public class SseEmitterRegistry {

    /** 30 minutes; browsers reconnect automatically when this elapses. */
    private static final long EMITTER_TIMEOUT_MS = 30L * 60L * 1000L;

    private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(Long userId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        Set<SseEmitter> userEmitters = emitters.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>());
        userEmitters.add(emitter);

        Runnable cleanup = () -> remove(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("userId", userId, "at", Instant.now().toString())));
        } catch (IOException ex) {
            remove(userId, emitter);
        }
        log.debug("SSE client registered for user {} (total {} streams)", userId, userEmitters.size());
        return emitter;
    }

    public void send(Long userId, NotificationResponse payload) {
        Set<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").id(String.valueOf(payload.id())).data(payload));
            } catch (IOException | IllegalStateException ex) {
                remove(userId, emitter);
            }
        }
    }

    public int connectedUsers() {
        return emitters.size();
    }

    public int streamCount() {
        return emitters.values().stream().mapToInt(Set::size).sum();
    }

    private void remove(Long userId, SseEmitter emitter) {
        Set<SseEmitter> set = emitters.get(userId);
        if (set != null) {
            set.remove(emitter);
            if (set.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }

    @Scheduled(fixedDelay = 25_000)
    public void heartbeat() {
        emitters.forEach((userId, set) -> {
            for (SseEmitter emitter : set) {
                try {
                    emitter.send(SseEmitter.event().name("heartbeat").comment("keep-alive"));
                } catch (IOException | IllegalStateException ex) {
                    remove(userId, emitter);
                }
            }
        });
    }
}
