package com.foodbridge.admin.dto;

import java.time.Instant;

/**
 * A single entry in the platform activity feed.
 */
public record ActivityItem(
        String type,
        String description,
        String actor,
        Instant occurredAt,
        String link
) {
}
