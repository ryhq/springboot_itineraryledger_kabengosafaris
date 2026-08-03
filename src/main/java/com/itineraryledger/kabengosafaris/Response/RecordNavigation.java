package com.itineraryledger.kabengosafaris.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * RecordNavigation — the "3 of 6" readout beside a record's prev/next arrows.
 *
 * Next/previous ids alone leave the user unable to tell where they are in the set
 * or that paging wrapped around. Detail endpoints add position + total with this,
 * scoped to a parent when the caller passed one so a child record pages within
 * its own parent.
 */
@Component
public class RecordNavigation {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * @param entityClass the child entity
     * @param parentPath  dotted path to the parent id, e.g. "customer.id"; null for a global walk
     * @param parentId    the parent, or null
     * @param id          the record being viewed
     */
    public Map<String, Object> positionOf(Class<?> entityClass, String parentPath, Long parentId, Long id) {
        Map<String, Object> nav = new HashMap<>();
        try {
            String jpql = "SELECT e.id FROM " + entityClass.getName() + " e"
                + (parentId != null && parentPath != null ? " WHERE e." + parentPath + " = :parentId" : "")
                + " ORDER BY e.id";
            var query = entityManager.createQuery(jpql, Long.class);
            if (parentId != null && parentPath != null) query.setParameter("parentId", parentId);

            List<Long> ids = query.getResultList();
            int index = ids.indexOf(id);
            nav.put("position", index >= 0 ? index + 1 : null);
            nav.put("total", ids.size());
        } catch (Exception e) {
            // a readout is a nicety; never fail the record fetch over it
            nav.put("position", null);
            nav.put("total", null);
        }
        return nav;
    }
}
