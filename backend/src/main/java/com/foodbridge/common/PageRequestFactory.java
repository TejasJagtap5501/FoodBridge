package com.foodbridge.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * Builds safe {@link Pageable} instances from untrusted query parameters.
 *
 * <p>Whitelisting sort fields prevents callers from sorting on arbitrary (or
 * non-existent) columns, which would otherwise leak schema information or
 * trigger expensive queries.</p>
 */
public final class PageRequestFactory {

    public static final int MAX_PAGE_SIZE = 100;

    private PageRequestFactory() {
    }

    public static Pageable of(int page, int size, String sortBy, String direction, Set<String> allowed) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String field = (sortBy == null || sortBy.isBlank() || !allowed.contains(sortBy))
                ? null
                : sortBy;
        if (field == null) {
            return PageRequest.of(safePage, safeSize);
        }
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(dir, field));
    }
}
