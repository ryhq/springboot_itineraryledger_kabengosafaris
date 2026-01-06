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
}
