package com.foodbridge.impact;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only aggregate queries for the reports page.
 *
 * <p>Implemented with JPQL/typed native queries aggregated in the database
 * rather than loading rows into memory - reporting stays fast as the platform
 * grows. All values are parameterised (no string concatenation), so SQL
 * injection is impossible.</p>
 */
@Repository
@RequiredArgsConstructor
public class ImpactQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /** Donations created per day for the last {@code days} days. */
    public Map<LocalDate, Long> donationsPerDay(int days) {
        Instant since = Instant.now().minusSeconds(days * 86_400L);
        List<?> rows = entityManager.createNativeQuery("""
                        SELECT DATE(created_at) AS d, COUNT(*) AS c
                          FROM donations
                         WHERE created_at >= :since
                         GROUP BY DATE(created_at)
                         ORDER BY d
                        """)
                .setParameter("since", Timestamp.from(since))
                .getResultList();
        Map<LocalDate, Long> result = new LinkedHashMap<>();
        for (Object row : rows) {
            Object[] cells = (Object[]) row;
            result.put(toLocalDate(cells[0]), ((Number) cells[1]).longValue());
        }
        return result;
    }

    /** Meals rescued per day for the last {@code days} days. */
    public Map<LocalDate, Long> mealsPerDay(int days) {
        Instant since = Instant.now().minusSeconds(days * 86_400L);
        List<?> rows = entityManager.createNativeQuery("""
                        SELECT DATE(completed_at) AS d, COALESCE(SUM(meals_served), 0) AS m
                          FROM impact_logs
                         WHERE completed_at >= :since
                         GROUP BY DATE(completed_at)
                         ORDER BY d
                        """)
                .setParameter("since", Timestamp.from(since))
                .getResultList();
        Map<LocalDate, Long> result = new LinkedHashMap<>();
        for (Object row : rows) {
            Object[] cells = (Object[]) row;
            result.put(toLocalDate(cells[0]), ((Number) cells[1]).longValue());
        }
        return result;
    }

    /** Completed-donation breakdown per category. */
    public List<Object[]> categoryBreakdown() {
        List<?> rows = entityManager.createNativeQuery("""
                        SELECT c.name, COUNT(d.id) AS donations, COALESCE(SUM(d.estimated_meals), 0) AS meals
                          FROM donations d
                          JOIN food_categories c ON c.id = d.category_id
                         WHERE d.status = 'COMPLETED'
                         GROUP BY c.id, c.name
                         ORDER BY meals DESC
                        """).getResultList();
        List<Object[]> typed = new ArrayList<>();
        rows.forEach(r -> typed.add((Object[]) r));
        return typed;
    }

    /** Completed-donation breakdown per city. */
    public List<Object[]> cityBreakdown(int limit) {
        List<?> rows = entityManager.createNativeQuery("""
                        SELECT d.city, COUNT(d.id) AS donations, COALESCE(SUM(d.estimated_meals), 0) AS meals
                          FROM donations d
                         WHERE d.status = 'COMPLETED'
                         GROUP BY d.city
                         ORDER BY meals DESC
                         LIMIT :lim
                        """)
                .setParameter("lim", limit)
                .getResultList();
        List<Object[]> typed = new ArrayList<>();
        rows.forEach(r -> typed.add((Object[]) r));
        return typed;
    }

    /** Donors ranked by meals donated. */
    public List<Object[]> topDonors(int limit) {
        List<?> rows = entityManager.createNativeQuery("""
                        SELECT u.id, u.full_name, COUNT(d.id) AS completed, COALESCE(SUM(d.estimated_meals), 0) AS meals
                          FROM donations d
                          JOIN users u ON u.id = d.donor_id
                         WHERE d.status = 'COMPLETED'
                         GROUP BY u.id, u.full_name
                         ORDER BY meals DESC
                         LIMIT :lim
                        """)
                .setParameter("lim", limit)
                .getResultList();
        List<Object[]> typed = new ArrayList<>();
        rows.forEach(r -> typed.add((Object[]) r));
        return typed;
    }

    public long countNewUsersSince(Instant since) {
        return ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM users WHERE created_at >= :since")
                .setParameter("since", Timestamp.from(since))
                .getSingleResult()).longValue();
    }

    public long countCompletedSince(Instant since) {
        return ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM donations WHERE status='COMPLETED' AND updated_at >= :since")
                .setParameter("since", Timestamp.from(since))
                .getSingleResult()).longValue();
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        return LocalDate.parse(String.valueOf(value));
    }
}
