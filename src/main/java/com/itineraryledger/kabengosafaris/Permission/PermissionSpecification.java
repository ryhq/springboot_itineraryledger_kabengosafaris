package com.itineraryledger.kabengosafaris.Permission;

import org.springframework.data.jpa.domain.Specification;

/**
 * PermissionSpecification - Provides reusable Specification objects for filtering Permission entities
 *
 * Specification allows for dynamic, type-safe query construction using the Criteria API
 * Each method returns a Specification<Permission> that can be combined with other specifications
 */
public class PermissionSpecification {

    /**
     * Filter by permission name (case-insensitive)
     */
    public static Specification<Permission> nameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isEmpty()) {
                return cb.conjunction(); // No filter applied
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    /**
     * Filter by entity (case-insensitive)
     */
    public static Specification<Permission> entityLike(String entity) {
        return (root, query, cb) -> {
            if (entity == null || entity.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("entity")), "%" + entity.toLowerCase() + "%");
        };
    }

    /**
     * Filter by permission action
     */
    public static Specification<Permission> hasAction(PermissionAction action) {
        return (root, query, cb) -> {
            if (action == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("action"), action);
        };
    }

    /**
     * Filter by active status
     */
    public static Specification<Permission> isActive(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("active"), active);
        };
    }
}
