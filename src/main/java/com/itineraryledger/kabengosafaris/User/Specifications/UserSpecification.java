package com.itineraryledger.kabengosafaris.User.Specifications;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.User.User;

import jakarta.persistence.criteria.JoinType;

/**
 * Filters for the people who can log in.
 *
 * Kept separate from Role/Services/UserSpecification, which exists to answer one
 * narrow question — who holds this role — for the role screen's assignment picker.
 * This is the list's own set, and the counters and the record paging are all built
 * from it so they cannot disagree with the rows.
 */
public class UserSpecification {

    /** Holders of any of the given roles. */
    public static Specification<User> byRoleIds(List<Long> roleIds) {
        return (root, query, cb) -> {
            if (roleIds == null || roleIds.isEmpty()) return cb.conjunction();
            if (query != null) query.distinct(true);
            var roles = root.join("roles", JoinType.LEFT);
            return roles.get("id").in(roleIds);
        };
    }

    public static Specification<User> byRoleNames(List<String> roleNames) {
        return (root, query, cb) -> {
            if (roleNames == null || roleNames.isEmpty()) return cb.conjunction();
            if (query != null) query.distinct(true);
            var roles = root.join("roles", JoinType.LEFT);
            return cb.upper(roles.get("name")).in(roleNames.stream().map(String::toUpperCase).toList());
        };
    }

    public static Specification<User> byRoleId(Long roleId) {
        return byRoleIds(roleId == null ? null : List.of(roleId));
    }

    public static Specification<User> isEnabled(Boolean enabled) {
        return (root, query, cb) -> enabled == null
            ? cb.conjunction()
            : cb.equal(root.get("enabled"), enabled);
    }

    public static Specification<User> isLocked(Boolean locked) {
        return (root, query, cb) -> locked == null
            ? cb.conjunction()
            : cb.equal(root.get("accountLocked"), locked);
    }

    public static Specification<User> hasMfa(Boolean mfa) {
        return (root, query, cb) -> {
            if (mfa == null) return cb.conjunction();
            /*
             * mfaEnabled alone is not the truth: setup starts by flipping it and only
             * finishes when the first code verifies. An account mid-setup has no second
             * factor yet, so it counts as off.
             */
            if (Boolean.TRUE.equals(mfa)) {
                return cb.and(
                    cb.isTrue(root.get("mfaEnabled")),
                    cb.isTrue(root.get("mfaConfirmed")));
            }
            return cb.or(
                cb.isFalse(root.get("mfaEnabled")),
                cb.isNull(root.get("mfaConfirmed")),
                cb.isFalse(root.get("mfaConfirmed")));
        };
    }

    /** No role at all — can sign in and then do nothing. */
    public static Specification<User> hasNoRoles() {
        return (root, query, cb) -> cb.isEmpty(root.get("roles"));
    }

    public static Specification<User> hasAnyRole() {
        return (root, query, cb) -> cb.isNotEmpty(root.get("roles"));
    }

    /**
     * Invited and never arrived.
     *
     * There is no last-login column on this table, so "never signed in" is read as
     * an account that is still disabled and has never had a failed attempt either —
     * nobody has tried to use it at all. Named for what it can actually prove.
     */
    public static Specification<User> neverSignedIn() {
        return (root, query, cb) -> cb.and(
            cb.isFalse(root.get("enabled")),
            cb.equal(root.get("failedAttempt"), 0));
    }

    /** Will be refused at the next login until the password is changed. */
    public static Specification<User> passwordExpired() {
        return (root, query, cb) -> cb.and(
            cb.isNotNull(root.get("passwordExpiryDate")),
            cb.lessThan(root.get("passwordExpiryDate"), LocalDateTime.now()));
    }

    /** Somebody is getting the password wrong — or guessing at it. */
    public static Specification<User> hasFailedAttempts() {
        return (root, query, cb) -> cb.greaterThan(root.get("failedAttempt"), 0);
    }

    public static Specification<User> createdAfter(LocalDateTime since) {
        return (root, query, cb) -> since == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("createdAt"), since);
    }

    public static Specification<User> createdBefore(LocalDateTime until) {
        return (root, query, cb) -> until == null
            ? cb.conjunction()
            : cb.lessThanOrEqualTo(root.get("createdAt"), until);
    }

    /**
     * The one search box: name, username, email and phone.
     *
     * A colleague is looked for by the name you call them, an account by the address
     * the invite went to, so both have to hit.
     */
    public static Specification<User> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("firstName")), like),
                cb.like(cb.lower(root.get("lastName")), like),
                cb.like(cb.lower(root.get("username")), like),
                cb.like(cb.lower(root.get("email")), like),
                cb.like(cb.lower(root.get("phoneNumber")), like),
                // "Wim Geeroms" typed in full must match a first/last split
                cb.like(
                    cb.lower(cb.concat(cb.concat(root.get("firstName"), " "), root.get("lastName"))),
                    like));
        };
    }
}
