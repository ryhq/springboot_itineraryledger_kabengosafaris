package com.itineraryledger.kabengosafaris.PaxNationCategory.Specifications;

import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specifications for PaxNationCategory entity
 *
 * Provides reusable, composable query predicates for filtering pax nation categories
 */
public class PaxNationCategorySpecification {

    private PaxNationCategorySpecification() {
        // Private constructor to prevent instantiation
    }

    /**
     * Filter by name (partial match, case-insensitive)
     */
    public static Specification<PaxNationCategory> nameLike(String name) {
        return (root, query, criteriaBuilder) -> {
            if (name == null || name.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("name")),
                "%" + name.toLowerCase() + "%"
            );
        };
    }

    /**
     * Filter by category type
     */
    public static Specification<PaxNationCategory> hasCategoryType(PaxNationCategory.CategoryType categoryType) {
        return (root, query, criteriaBuilder) -> {
            if (categoryType == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("categoryType"), categoryType);
        };
    }

    /**
     * Filter by active status
     */
    public static Specification<PaxNationCategory> isActive(Boolean isActive) {
        return (root, query, criteriaBuilder) -> {
            if (isActive == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("isActive"), isActive);
        };
    }

    /**
     * Filter by system status
     */
    public static Specification<PaxNationCategory> isSystem(Boolean isSystem) {
        return (root, query, criteriaBuilder) -> {
            if (isSystem == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("isSystem"), isSystem);
        };
    }

    /**
     * Filter by exact priority factor
     */
    public static Specification<PaxNationCategory> hasPriorityFactor(Integer priorityFactor) {
        return (root, query, criteriaBuilder) -> {
            if (priorityFactor == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("priorityFactor"), priorityFactor);
        };
    }

    /**
     * Filter by minimum priority factor (inclusive)
     */
    public static Specification<PaxNationCategory> hasMinPriorityFactor(Integer minPriority) {
        return (root, query, criteriaBuilder) -> {
            if (minPriority == null) {
                return null;
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("priorityFactor"), minPriority);
        };
    }

    /**
     * Filter by maximum priority factor (inclusive)
     */
    public static Specification<PaxNationCategory> hasMaxPriorityFactor(Integer maxPriority) {
        return (root, query, criteriaBuilder) -> {
            if (maxPriority == null) {
                return null;
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("priorityFactor"), maxPriority);
        };
    }

    /**
     * Search keyword across name and description
     */
    public static Specification<PaxNationCategory> searchKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return null;
            }
            String lowerKeyword = "%" + keyword.toLowerCase() + "%";
            return criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), lowerKeyword),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), lowerKeyword)
            );
        };
    }

    /** Rows created on or after `moment` — the recency counters. */
    public static Specification<PaxNationCategory> createdAfter(java.time.LocalDateTime moment) {
        return (root, query, cb) ->
            moment == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), moment);
    }
}
