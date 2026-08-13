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

    /** Exact entities, OR'd — the dimension the catalogue is actually read by. */
    public static Specification<Permission> byEntities(java.util.List<String> entities) {
        return (root, query, cb) -> entities == null || entities.isEmpty()
            ? cb.conjunction()
            : cb.upper(root.get("entity")).in(entities.stream().map(String::toUpperCase).toList());
    }

    public static Specification<Permission> byActions(java.util.List<PermissionAction> actions) {
        return (root, query, cb) -> actions == null || actions.isEmpty()
            ? cb.conjunction()
            : root.get("action").in(actions);
    }

    /**
     * The one search box: the name, the entity and the description.
     *
     * Names are shouty and underscored (UPDATE_SAFARI_DAY_PARK), so somebody looking
     * for "safari day park" has to hit as well — the description is what carries the
     * words a person would actually type.
     */
    public static Specification<Permission> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.toLowerCase().trim().replace(' ', '_') + "%";
            String plain = "%" + keyword.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), like),
                cb.like(cb.lower(root.get("entity")), like),
                cb.like(cb.lower(root.get("description")), plain));
        };
    }

    /**
     * Granted by no role at all.
     *
     * Worth being able to list: it means nobody in the company can do that thing, and
     * the endpoint behind it answers 403 to every person who tries. Usually a module
     * that was built and never assigned to anybody.
     */
    public static Specification<Permission> hasNoRoles() {
        return (root, query, cb) -> {
            var subquery = query.subquery(Long.class);
            var roleRoot = subquery.from(com.itineraryledger.kabengosafaris.Role.Role.class);
            var permissionsJoin = roleRoot.join("permissions", jakarta.persistence.criteria.JoinType.INNER);
            subquery.select(permissionsJoin.get("id"));
            return cb.not(root.get("id").in(subquery));
        };
    }

    /**
     * A named capability rather than one of the generated four.
     *
     * The catalogue is mostly action × entity (CREATE_PARK, READ_PARK, …). The rest are
     * hand-written for things that are not CRUD — SEND_INVOICE, APPROVE_SAFARI — and
     * they are the ones worth reviewing when deciding what a role may do, because they
     * are where the workflow lives.
     */
    public static Specification<Permission> isCustom() {
        return (root, query, cb) -> cb.notEqual(
            root.get("name"),
            cb.concat(cb.concat(root.get("action").as(String.class), "_"), root.get("entity")));
    }
}
