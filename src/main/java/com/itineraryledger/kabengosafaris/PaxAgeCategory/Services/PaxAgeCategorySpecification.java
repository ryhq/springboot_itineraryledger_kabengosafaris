package com.itineraryledger.kabengosafaris.PaxAgeCategory.Services;

import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import org.springframework.data.jpa.domain.Specification;

/**
 * PaxAgeCategorySpecification - Provides reusable Specification objects for filtering PaxAgeCategory entities
 *
 * Specification allows for dynamic, type-safe query construction using the Criteria API
 * Each method returns a Specification<PaxAgeCategory> that can be combined with other specifications
 */
public class PaxAgeCategorySpecification {

    /**
     * Filter by category name (case-insensitive partial match)
     */
    public static Specification<PaxAgeCategory> nameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    /**
     * Filter by exact category name (case-insensitive)
     */
    public static Specification<PaxAgeCategory> hasName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(cb.lower(root.get("name")), name.toLowerCase());
        };
    }

    /**
     * Filter by category type
     */
    public static Specification<PaxAgeCategory> hasCategoryType(PaxAgeCategory.CategoryType categoryType) {
        return (root, query, cb) -> {
            if (categoryType == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("categoryType"), categoryType);
        };
    }

    /**
     * Filter by active status
     */
    public static Specification<PaxAgeCategory> isActive(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isActive"), isActive);
        };
    }

    /**
     * Filter by system status
     * TRUE = System categories (protected from deletion, created by initializer)
     * FALSE = User-created categories (can be deleted)
     */
    public static Specification<PaxAgeCategory> isSystem(Boolean isSystem) {
        return (root, query, cb) -> {
            if (isSystem == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isSystem"), isSystem);
        };
    }

    /**
     * Filter by minimum age (exact match)
     */
    public static Specification<PaxAgeCategory> hasMinAge(Integer minAge) {
        return (root, query, cb) -> {
            if (minAge == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("minAge"), minAge);
        };
    }

    /**
     * Filter by maximum age (exact match)
     */
    public static Specification<PaxAgeCategory> hasMaxAge(Integer maxAge) {
        return (root, query, cb) -> {
            if (maxAge == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("maxAge"), maxAge);
        };
    }

    /**
     * Filter categories where minAge is greater than or equal to a value
     */
    public static Specification<PaxAgeCategory> minAgeGreaterThanOrEqual(Integer age) {
        return (root, query, cb) -> {
            if (age == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("minAge"), age);
        };
    }

    /**
     * Filter categories where maxAge is less than or equal to a value
     */
    public static Specification<PaxAgeCategory> maxAgeLessThanOrEqual(Integer age) {
        return (root, query, cb) -> {
            if (age == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("maxAge"), age);
        };
    }

    /**
     * Filter categories that include a specific age in their range
     * The age must be >= minAge AND <= maxAge
     */
    public static Specification<PaxAgeCategory> includesAge(Integer age) {
        return (root, query, cb) -> {
            if (age == null) {
                return cb.conjunction();
            }
            return cb.and(
                cb.lessThanOrEqualTo(root.get("minAge"), age),
                cb.greaterThanOrEqualTo(root.get("maxAge"), age)
            );
        };
    }

    /**
     * Filter system categories only (protected from deletion)
     */
    public static Specification<PaxAgeCategory> systemCategoriesOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("isSystem"));
    }

    /**
     * Filter non-system categories only (can be deleted)
     */
    public static Specification<PaxAgeCategory> nonSystemCategoriesOnly() {
        return (root, query, cb) -> cb.isFalse(root.get("isSystem"));
    }

    /**
     * Filter active system categories
     */
    public static Specification<PaxAgeCategory> activeSystemCategories() {
        return (root, query, cb) -> cb.and(
            cb.isTrue(root.get("isSystem")),
            cb.isTrue(root.get("isActive"))
        );
    }

    /**
     * Filter by description (case-insensitive partial match)
     */
    public static Specification<PaxAgeCategory> descriptionLike(String description) {
        return (root, query, cb) -> {
            if (description == null || description.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%");
        };
    }

    /**
     * Search across name and description fields
     */
    public static Specification<PaxAgeCategory> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isEmpty()) {
                return cb.conjunction();
            }
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), likePattern),
                cb.like(cb.lower(root.get("description")), likePattern)
            );
        };
    }

    /** Rows created on or after `moment` — the recency counters. */
    public static Specification<PaxAgeCategory> createdAfter(java.time.LocalDateTime moment) {
        return (root, query, cb) ->
            moment == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), moment);
    }
}
