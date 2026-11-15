package com.foodbridge.security;

import com.foodbridge.exception.ApiException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight in-process sliding-window rate limiter for sensitive endpoints
 * (login, registration, password reset).
 *
 * <p>Deliberately dependency-free: for a single-node deployment this is enough
 * and avoids dragging Redis into the stack. The class is isolated behind one
 * method so it can be swapped for a Redis-backed bucket when the platform is
 * horizontally scaled (documented in docs/ARCHITECTURE.md).</p>
 */
@Service
public class RateLimitService {

    private record Bucket(int count, Instant windowStart) {
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * @param key      logical bucket, e.g. {@code "login:1.2.3.4"}
     * @param limit    allowed attempts per window
     * @param window   window length
     */
    public void check(String key, int limit, Duration window) {
        Instant now = Instant.now();
        Bucket bucket = buckets.compute(key, (k, existing) -> {
            if (existing == null || Duration.between(existing.windowStart(), now).compareTo(window) > 0) {
                return new Bucket(1, now);
            }
            return new Bucket(existing.count() + 1, existing.windowStart());
        });
        if (bucket.count() > limit) {
            throw ApiException.tooManyRequests("RATE_LIMITED",
                    "Too many attempts. Please wait a moment and try again.");
        }
    }

    /** Test hook. */
    public void reset() {
        buckets.clear();
    }

    @Scheduled(fixedDelay = 600_000)
    public void evictStale() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(1));
        buckets.entrySet().removeIf(e -> e.getValue().windowStart().isBefore(cutoff));
    }
}
