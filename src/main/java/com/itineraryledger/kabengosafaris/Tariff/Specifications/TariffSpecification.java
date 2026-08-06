package com.itineraryledger.kabengosafaris.Tariff.Specifications;

import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import com.itineraryledger.kabengosafaris.Tariff.Tariff;
import org.springframework.data.jpa.domain.Specification;

/**
 * TariffSpecification - Dynamic JPA Specifications for filtering Tariff entities
 *
 * Provides reusable, composable specifications for building complex queries.
 * Use with JpaSpecificationExecutor in TariffRepository.
 */
public class TariffSpecification {

    private TariffSpecification() {
        // Utility class - prevent instantiation
    }

    // ========================
    // NAME SPECIFICATIONS
    // ========================

    /**
     * Filter by name containing (case-insensitive partial match)
     */
    public static Specification<Tariff> nameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase().trim() + "%");
        };
    }

    /**
     * Filter by exact name (case-insensitive)
     */
    public static Specification<Tariff> nameEquals(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(cb.lower(root.get("name")), name.toLowerCase().trim());
        };
    }

    // ========================
    // SLUG SPECIFICATIONS
    // ========================

    /**
     * Filter by slug (exact match)
     */
    public static Specification<Tariff> hasSlug(String slug) {
        return (root, query, cb) -> {
            if (slug == null || slug.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("slug"), slug.trim());
        };
    }

    /**
     * Filter by slug containing (partial match)
     */
    public static Specification<Tariff> hasSlugLike(String slug) {
        return (root, query, cb) -> {
            if (slug == null || slug.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("slug")), "%" + slug.toLowerCase().trim() + "%");
        };
    }

    // ========================
    // CHARGING BASIS SPECIFICATIONS
    // ========================

    /**
     * Filter by charging basis enum value
     */
    public static Specification<Tariff> hasChargingBasis(ChargingBasis chargingBasis) {
        return (root, query, cb) -> {
            if (chargingBasis == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("chargingBasis"), chargingBasis);
        };
    }

    /**
     * Filter by charging basis string (case-insensitive)
     * Converts string to enum and filters
     */
    public static Specification<Tariff> hasChargingBasis(String chargingBasisStr) {
        return (root, query, cb) -> {
            if (chargingBasisStr == null || chargingBasisStr.trim().isEmpty()) {
                return cb.conjunction();
            }
            try {
                ChargingBasis basis = ChargingBasis.valueOf(chargingBasisStr.toUpperCase().trim());
                return cb.equal(root.get("chargingBasis"), basis);
            } catch (IllegalArgumentException e) {
                // Invalid enum value - return no results
                return cb.disjunction();
            }
        };
    }

    /**
     * Filter tariffs that require age category (PER_PERSON charging basis)
     */
    public static Specification<Tariff> requiresAgeCategory() {
        return (root, query, cb) -> cb.equal(root.get("chargingBasis"), ChargingBasis.PER_PERSON);
    }

    /**
     * Filter tariffs that don't require age category
     */
    public static Specification<Tariff> doesNotRequireAgeCategory() {
        return (root, query, cb) -> cb.notEqual(root.get("chargingBasis"), ChargingBasis.PER_PERSON);
    }

    // ========================
    // STATUS SPECIFICATIONS
    // ========================

    /**
     * Filter by active status
     */
    public static Specification<Tariff> isActive(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isActive"), isActive);
        };
    }

    /**
     * Filter by system status
     */
    public static Specification<Tariff> isSystem(Boolean isSystem) {
        return (root, query, cb) -> {
            if (isSystem == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isSystem"), isSystem);
        };
    }

    // ========================
    // TEXT SEARCH SPECIFICATIONS
    // ========================

    /**
     * Keyword search across name and description (case-insensitive)
     */
    public static Specification<Tariff> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + keyword.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    /**
     * Filter by description containing (case-insensitive)
     */
    public static Specification<Tariff> descriptionLike(String description) {
        return (root, query, cb) -> {
            if (description == null || description.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase().trim() + "%");
        };
    }

    // ========================
    // PARK RELATIONSHIP SPECIFICATIONS
    // ========================

    /**
     * Filter tariffs assigned to a specific park
     */
    public static Specification<Tariff> byParkId(Long parkId) {
        return (root, query, cb) -> {
            if (parkId == null) {
                return cb.conjunction();
            }
            var parkTariffs = root.join("parkTariffs");
            return cb.equal(parkTariffs.get("park").get("id"), parkId);
        };
    }

    /**
     * Filter tariffs NOT assigned to a specific park
     */
    public static Specification<Tariff> notByParkId(Long parkId) {
        return (root, query, cb) -> {
            if (parkId == null) {
                return cb.conjunction();
            }
            var subquery = query.subquery(Long.class);
            var subRoot = subquery.from(Tariff.class);
            var parkTariffs = subRoot.join("parkTariffs");
            subquery.select(subRoot.get("id"))
                    .where(cb.equal(parkTariffs.get("park").get("id"), parkId));
            return cb.not(root.get("id").in(subquery));
        };
    }

    /** Rows created on or after `moment` — the recency counters. */
    public static Specification<Tariff> createdAfter(java.time.LocalDateTime moment) {
        return (root, query, cb) ->
            moment == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), moment);
    }
}
