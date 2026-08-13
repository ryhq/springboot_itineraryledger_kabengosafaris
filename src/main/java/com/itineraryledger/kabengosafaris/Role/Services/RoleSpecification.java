package com.itineraryledger.kabengosafaris.Role.Services;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Role.Role;
import com.itineraryledger.kabengosafaris.User.User;

/**
 * RoleSpecification - Provides reusable Specification objects for filtering Role entities
 *
 * Specification allows for dynamic, type-safe query construction using the Criteria API
 * Each method returns a Specification<Role> that can be combined with other specifications
 */
public class RoleSpecification {

    /**
     * Filter by role name (case-insensitive)
     */
    public static Specification<Role> nameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isEmpty()) {
                return cb.conjunction(); // No filter applied
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    /**
     * Filter by display name (case-insensitive)
     */
    public static Specification<Role> displayNameLike(String displayName) {
        return (root, query, cb) -> {
            if (displayName == null || displayName.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("displayName")), "%" + displayName.toLowerCase() + "%");
        };
    }

    /**
     * Filter by active status
     */
    public static Specification<Role> isActive(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("active"), active);
        };
    }

    /**
     * Filter by system role status
     */
    public static Specification<Role> isSystemRole(Boolean isSystemRole) {
        return (root, query, cb) -> {
            if (isSystemRole == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isSystemRole"), isSystemRole);
        };
    }

    /**
     * Filter roles assigned to a specific user
     *
     * This uses a subquery to find roles that are associated with the user
     * through the user_roles join table (User has ManyToMany with Role)
     *
     * @param userId The user's ID to filter by
     * @return Specification that filters roles by user
     */
    public static Specification<Role> hasUser(Long userId) {
        return (root, query, cb) -> {
            if (userId == null) {
                return cb.conjunction();
            }

            // Create a subquery that checks if the role is in the user's roles
            // Since the relationship is from User -> Role, we need to query from User side
            var subquery = query.subquery(Long.class);
            var userRoot = subquery.from(User.class);
            var rolesJoin = userRoot.join("roles", JoinType.INNER);

            subquery.select(rolesJoin.get("id"))
                .where(cb.equal(userRoot.get("id"), userId));

            return root.get("id").in(subquery);
        };
    }

    /**
     * The one search box: the name, the display name and the description.
     *
     * A role is looked for by what it is called on screen ("Finance Officer") as often
     * as by its key ("finance_officer"), so both have to hit.
     */
    public static Specification<Role> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), like),
                cb.like(cb.lower(root.get("displayName")), like),
                cb.like(cb.lower(root.get("description")), like));
        };
    }

    /** Grants nothing — anybody holding it can sign in and do nothing. */
    public static Specification<Role> hasNoPermissions() {
        return (root, query, cb) -> cb.isEmpty(root.get("permissions"));
    }

    public static Specification<Role> hasAnyPermission() {
        return (root, query, cb) -> cb.isNotEmpty(root.get("permissions"));
    }

    /**
     * Nobody holds it.
     *
     * The join runs from User, because the many-to-many is owned there — a role has no
     * `users` collection to test for emptiness.
     */
    public static Specification<Role> hasNoUsers() {
        return (root, query, cb) -> {
            var subquery = query.subquery(Long.class);
            var userRoot = subquery.from(User.class);
            var rolesJoin = userRoot.join("roles", JoinType.INNER);
            subquery.select(rolesJoin.get("id"));
            return cb.not(root.get("id").in(subquery));
        };
    }

    public static Specification<Role> createdAfter(java.time.LocalDateTime since) {
        return (root, query, cb) -> since == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("createdAt"), since);
    }

    public static Specification<Role> createdBefore(java.time.LocalDateTime until) {
        return (root, query, cb) -> until == null
            ? cb.conjunction()
            : cb.lessThanOrEqualTo(root.get("createdAt"), until);
    }

    /**
     * Filter roles that have a specific permission
     *
     * This joins the role_permissions table to find roles that contain
     * the specified permission
     *
     * @param permissionId The permission's ID to filter by
     * @return Specification that filters roles by permission
     */
    public static Specification<Role> hasPermission(Long permissionId) {
        return (root, query, cb) -> {
            if (permissionId == null) {
                return cb.conjunction();
            }

            // Join to permissions collection and filter by permission ID
            var permissionsJoin = root.join("permissions", JoinType.INNER);
            return cb.equal(permissionsJoin.get("id"), permissionId);
        };
    }
}
