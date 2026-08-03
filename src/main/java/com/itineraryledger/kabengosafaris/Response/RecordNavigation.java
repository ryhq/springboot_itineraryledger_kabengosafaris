package com.itineraryledger.kabengosafaris.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * RecordNavigation — prev/next plus the "3 of 6" readout on a record page.
 *
 * Paging must walk the SAME set the list was showing, so callers pass the very
 * Specification their list built (see CustomerGetService.navigationIds): page into
 * a record from a "Blacklisted" list and next/previous stay inside blacklisted
 * records. Position and total make the wraparound visible.
 *
 * Without the shared Specification the arrows silently traverse a different set
 * from the one on screen, which is worse than having no arrows.
 */
@Component
public class RecordNavigation {

    /** Ceiling on the id walk, so a huge filter can't exhaust memory. */
    private static final int NAV_ID_LIMIT = 20_000;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Ordered ids for a filtered set.
     *
     * @param sortBy    a sortable field on the entity; falls back to id when null/unknown
     * @param ascending sort direction, matching the list's
     */
    public <T> List<Long> orderedIds(
        Class<T> entityClass,
        Specification<T> spec,
        String sortBy,
        boolean ascending
    ) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var root = query.from(entityClass);
        query.select(root.get("id"));

        if (spec != null) {
            var predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) query.where(predicate);
        }

        // id is the tiebreaker so the order is stable and matches the list page
        var sortPath = resolveSortPath(root, sortBy);
        query.orderBy(
            ascending ? cb.asc(sortPath) : cb.desc(sortPath),
            ascending ? cb.asc(root.get("id")) : cb.desc(root.get("id"))
        );

        return entityManager.createQuery(query).setMaxResults(NAV_ID_LIMIT).getResultList();
    }

    /** nextId / previousId / position / total for one record inside a filtered set. */
    public <T> Map<String, Object> navigate(
        Class<T> entityClass,
        Specification<T> spec,
        String sortBy,
        boolean ascending,
        Long id
    ) {
        Map<String, Object> nav = new HashMap<>();
        try {
            List<Long> ids = orderedIds(entityClass, spec, sortBy, ascending);
            int index = ids.indexOf(id);

            Long nextId = null;
            Long previousId = null;
            if (index >= 0 && ids.size() > 1) {
                nextId = ids.get((index + 1) % ids.size());
                previousId = ids.get((index - 1 + ids.size()) % ids.size());
            }

            nav.put("nextRawId", nextId);
            nav.put("previousRawId", previousId);
            nav.put("position", index >= 0 ? index + 1 : null);
            nav.put("total", ids.size());
        } catch (Exception e) {
            // paging is a nicety; never fail the record fetch over it
            nav.put("position", null);
            nav.put("total", null);
        }
        return nav;
    }

    /** Just the readout, for callers that already have their next/previous ids. */
    public <T> Map<String, Object> positionOf(
        Class<T> entityClass,
        Specification<T> spec,
        String sortBy,
        boolean ascending,
        Long id
    ) {
        Map<String, Object> nav = navigate(entityClass, spec, sortBy, ascending, id);
        nav.remove("nextRawId");
        nav.remove("previousRawId");
        return nav;
    }

    private jakarta.persistence.criteria.Path<?> resolveSortPath(
        jakarta.persistence.criteria.Root<?> root,
        String sortBy
    ) {
        if (sortBy == null || sortBy.isBlank()) return root.get("id");
        try {
            return root.get(sortBy);
        } catch (IllegalArgumentException e) {
            // an unknown field must not 500 the record page
            return root.get("id");
        }
    }
}
