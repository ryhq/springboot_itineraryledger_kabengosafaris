package com.itineraryledger.kabengosafaris.Response;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * ListStats — the dashboard counters that ride inside every list response.
 *
 * Every list endpoint carries stat cards, and each card must be reachable as a
 * filter. Writing a bespoke computeStats() per module would be ~40 copies of the
 * same code, so modules instead declare their figures against the SAME
 * Specification the rows were fetched with:
 *
 *   return listStats.of(Park.class, spec)
 *       .total()
 *       .count("active", ParkSpecification.isActive(true))
 *       .complement("inactive", "active")
 *       .breakdown("byType", ParkType.values(), ParkSpecification::hasParkType)
 *       .window("newLast7Days", 7, ParkSpecification::createdAfter)
 *       .build();
 *
 * Sharing the base Specification is what guarantees a card and the table it
 * heads can never disagree.
 */
@Component
public class ListStats {

    @PersistenceContext
    private EntityManager entityManager;

    public <T> Builder<T> of(Class<T> entityClass, Specification<T> base) {
        return new Builder<>(entityClass, base);
    }

    /** Count rows matching the given specification. */
    public <T> long count(Class<T> entityClass, Specification<T> spec) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var root = query.from(entityClass);
        // countDistinct: specs that join child tables would otherwise multiply rows
        query.select(cb.countDistinct(root));

        if (spec != null) {
            var predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) query.where(predicate);
        }
        return entityManager.createQuery(query).getSingleResult();
    }

    /** Fluent stats assembly for one entity + one base filter. */
    public class Builder<T> {
        private final Class<T> entityClass;
        private final Specification<T> base;
        private final Map<String, Object> stats = new LinkedHashMap<>();

        private Builder(Class<T> entityClass, Specification<T> base) {
            this.entityClass = entityClass;
            this.base = base == null ? Specification.unrestricted() : base;
        }

        /** Row count for the current filter set — the leading "All X" card. */
        public Builder<T> total() {
            stats.put("total", ListStats.this.count(entityClass, base));
            return this;
        }

        /** A named counter for one extra constraint. */
        public Builder<T> count(String key, Specification<T> extra) {
            stats.put(key, ListStats.this.count(entityClass, base.and(extra)));
            return this;
        }

        /**
         * `total - other`, without a second query. Use for the inverse of a
         * boolean card (inactive = total - active) so the pair always sums to the
         * total the user sees.
         */
        public Builder<T> complement(String key, String other) {
            long total = asLong(stats.get("total"));
            stats.put(key, total - asLong(stats.get(other)));
            return this;
        }

        /** One counter per enum constant, nested under `key`. */
        public <E> Builder<T> breakdown(String key, E[] values, Function<E, Specification<T>> spec) {
            Map<String, Object> byValue = new HashMap<>();
            for (E value : values) {
                byValue.put(value.toString(), ListStats.this.count(entityClass, base.and(spec.apply(value))));
            }
            stats.put(key, byValue);
            return this;
        }

        /** "Added in the last N days", using the module's createdAfter spec. */
        public Builder<T> window(String key, int days, Function<LocalDateTime, Specification<T>> createdAfter) {
            stats.put(key, ListStats.this.count(entityClass, base.and(createdAfter.apply(LocalDateTime.now().minusDays(days)))));
            return this;
        }

        /** The conventional recency pair, so every list reports it the same way. */
        public Builder<T> recency(Function<LocalDateTime, Specification<T>> createdAfter) {
            return window("newLast7Days", 7, createdAfter).window("newLast30Days", 30, createdAfter);
        }

        public Map<String, Object> build() {
            return stats;
        }

        private long asLong(Object value) {
            return value instanceof Number number ? number.longValue() : 0L;
        }
    }
}
