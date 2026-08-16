package com.itineraryledger.kabengosafaris.AuditLog;

import org.springframework.data.jpa.domain.Specification;

/**
 * AuditLogSpecification - Provides reusable Specification objects for filtering AuditLog entities
 *
 * Specification allows for dynamic, type-safe query construction using the Criteria API
 * Each method returns a Specification<AuditLog> that can be combined with other specifications
 */
public class AuditLogSpecification {

    /**
     * Filter by name (case-insensitive partial match)
     */
    public static Specification<AuditLog> nameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isEmpty()) {
                return cb.conjunction(); // No filter applied
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    /**
     * Filter by userId
     */
    public static Specification<AuditLog> hasUserId(Long userId) {
        return (root, query, cb) -> {
            if (userId == null) {
                return cb.conjunction(); // No filter applied
            }
            return cb.equal(root.get("userId"), userId);
        };
    }

    /**
     * Filter by username (case-insensitive partial match)
     */
    public static Specification<AuditLog> usernameLike(String username) {
        return (root, query, cb) -> {
            if (username == null || username.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%");
        };
    }

    /**
     * Filter by action (case-insensitive partial match)
     */
    public static Specification<AuditLog> actionLike(String action) {
        return (root, query, cb) -> {
            if (action == null || action.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("action")), "%" + action.toLowerCase() + "%");
        };
    }

    /**
     * Filter by entityType (case-insensitive partial match)
     */
    public static Specification<AuditLog> entityTypeLike(String entityType) {
        return (root, query, cb) -> {
            if (entityType == null || entityType.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("entityType")), "%" + entityType.toLowerCase() + "%");
        };
    }

    /**
     * Filter by entityId
     */
    public static Specification<AuditLog> hasEntityId(Long entityId) {
        return (root, query, cb) -> {
            if (entityId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("entityId"), entityId);
        };
    }

    /**
     * Filter by description (case-insensitive partial match)
     */
    public static Specification<AuditLog> descriptionLike(String description) {
        return (root, query, cb) -> {
            if (description == null || description.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%");
        };
    }

    /**
     * Filter by ipAddress (exact match)
     */
    public static Specification<AuditLog> hasIpAddress(String ipAddress) {
        return (root, query, cb) -> {
            if (ipAddress == null || ipAddress.isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("ipAddress"), ipAddress);
        };
    }

    /**
     * Filter by userAgent (case-insensitive partial match)
     */
    public static Specification<AuditLog> userAgentLike(String userAgent) {
        return (root, query, cb) -> {
            if (userAgent == null || userAgent.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("userAgent")), "%" + userAgent.toLowerCase() + "%");
        };
    }

    /**
     * Filter by status (case-insensitive partial match)
     */
    public static Specification<AuditLog> statusLike(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("status")), "%" + status.toLowerCase() + "%");
        };
    }

    /**
     * Filter by errorMessage (case-insensitive partial match)
     */
    public static Specification<AuditLog> errorMessageLike(String errorMessage) {
        return (root, query, cb) -> {
            if (errorMessage == null || errorMessage.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("errorMessage")), "%" + errorMessage.toLowerCase() + "%");
        };
    }

    // ========================
    // MULTI-VALUE FACETS (OR inside a dimension, AND across dimensions)
    // ========================

    /** Any of these actions. An empty list is no constraint, never "nothing". */
    public static Specification<AuditLog> actionIn(java.util.List<String> actions) {
        return (root, query, cb) -> actions == null || actions.isEmpty()
            ? cb.conjunction()
            : root.get("action").in(actions);
    }

    public static Specification<AuditLog> entityTypeIn(java.util.List<String> entityTypes) {
        return (root, query, cb) -> entityTypes == null || entityTypes.isEmpty()
            ? cb.conjunction()
            : root.get("entityType").in(entityTypes);
    }

    public static Specification<AuditLog> statusIn(java.util.List<String> statuses) {
        return (root, query, cb) -> statuses == null || statuses.isEmpty()
            ? cb.conjunction()
            : root.get("status").in(statuses);
    }

    /**
     * The one search box: who did it, what they did, to which kind of record, and the
     * sentence describing it.
     *
     * Deliberately NOT oldValues/newValues. Those are @Lob LONGTEXT holding whole records as
     * JSON — LOWER() over a CLOB throws, and scanning every version of every row ever saved
     * is not a search anybody wants to wait for.
     */
    public static Specification<AuditLog> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("username")), like),
                cb.like(cb.lower(root.get("action")), like),
                cb.like(cb.lower(root.get("entityType")), like),
                cb.like(cb.lower(root.get("description")), like),
                cb.like(cb.lower(root.get("name")), like));
        };
    }

    // ========================
    // WORTH LOOKING AT — each of these is a card AND a filter
    // ========================

    /** Somebody tried to do something and did not get it. */
    public static Specification<AuditLog> isFailure() {
        return (root, query, cb) -> cb.or(
            cb.notEqual(root.get("status"), "SUCCESS"),
            cb.isNull(root.get("status")));
    }

    /**
     * The successes that cannot be undone.
     *
     * Every delete action is named DELETE_<ENTITY> by the annotation, so the prefix is a
     * reliable read rather than a guess.
     */
    public static Specification<AuditLog> isDeletion() {
        return (root, query, cb) -> cb.like(cb.upper(root.get("action")), "DELETE%");
    }

    public static Specification<AuditLog> createdAfter(java.time.LocalDateTime when) {
        return (root, query, cb) -> when == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("createdAt"), when);
    }

    public static Specification<AuditLog> createdBefore(java.time.LocalDateTime when) {
        return (root, query, cb) -> when == null
            ? cb.conjunction()
            : cb.lessThanOrEqualTo(root.get("createdAt"), when);
    }
}
