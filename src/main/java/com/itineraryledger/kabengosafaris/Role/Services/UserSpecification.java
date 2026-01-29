package com.itineraryledger.kabengosafaris.Role.Services;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.User.User;

/**
 * UserSpecification - Provides reusable Specification objects for filtering User entities
 * for role-user assignment operations.
 */
public class UserSpecification {

    /**
     * Filter by keyword (searches in username, email, firstName, lastName)
     * Case-insensitive partial match.
     */
    public static Specification<User> keywordSearch(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("username")), pattern),
                cb.like(cb.lower(root.get("email")), pattern),
                cb.like(cb.lower(root.get("firstName")), pattern),
                cb.like(cb.lower(root.get("lastName")), pattern)
            );
        };
    }

    /**
     * Filter by enabled status.
     */
    public static Specification<User> isEnabled(Boolean enabled) {
        return (root, query, cb) -> {
            if (enabled == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("enabled"), enabled);
        };
    }

    /**
     * Filter users that have a specific role.
     *
     * @param roleId The role's ID to filter by
     * @return Specification that filters users by role
     */
    public static Specification<User> hasRole(Long roleId) {
        return (root, query, cb) -> {
            if (roleId == null) {
                return cb.conjunction();
            }
            var rolesJoin = root.join("roles", JoinType.INNER);
            return cb.equal(rolesJoin.get("id"), roleId);
        };
    }

    /**
     * Filter users that do NOT have a specific role.
     *
     * @param roleId The role's ID to exclude
     * @return Specification that filters users without the role
     */
    public static Specification<User> doesNotHaveRole(Long roleId) {
        return (root, query, cb) -> {
            if (roleId == null) {
                return cb.conjunction();
            }
            // Create a subquery to find user IDs that have the role
            var subquery = query.subquery(Long.class);
            var userRoot = subquery.from(User.class);
            var rolesJoin = userRoot.join("roles", JoinType.INNER);

            subquery.select(userRoot.get("id"))
                .where(cb.equal(rolesJoin.get("id"), roleId));

            // Return users whose ID is NOT in the subquery
            return cb.not(root.get("id").in(subquery));
        };
    }
}
