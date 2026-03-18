package com.itineraryledger.kabengosafaris.Safari.Specifications;

import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * SafariSpecification - JPA Specifications for Safari filtering
 */
public class SafariSpecification {

    public static Specification<Safari> nameLike(String name) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Safari> codeLike(String code) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("code")), "%" + code.toLowerCase() + "%");
    }

    public static Specification<Safari> hasState(SafariState state) {
        return (root, query, cb) -> cb.equal(root.get("state"), state);
    }

    public static Specification<Safari> startLocationLike(String startLocation) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("startLocation")), "%" + startLocation.toLowerCase() + "%");
    }

    public static Specification<Safari> endLocationLike(String endLocation) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("endLocation")), "%" + endLocation.toLowerCase() + "%");
    }

    public static Specification<Safari> startDateAfter(LocalDate date) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startDate"), date);
    }

    public static Specification<Safari> startDateBefore(LocalDate date) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("startDate"), date);
    }

    public static Specification<Safari> isActive(Boolean isActive) {
        return (root, query, cb) -> {
            query.distinct(true);
            return cb.equal(root.get("isActive"), isActive);
        };
    }

    public static Specification<Safari> searchKeyword(String keyword) {
        String lowerKeyword = "%" + keyword.toLowerCase() + "%";
        // Note: description is @Lob — LOWER() on CLOB types causes errors in Hibernate 6
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), lowerKeyword),
                cb.like(cb.lower(root.get("code")), lowerKeyword),
                cb.like(cb.lower(root.get("startLocation")), lowerKeyword),
                cb.like(cb.lower(root.get("endLocation")), lowerKeyword)
        );
    }

    public static Specification<Safari> hasItinerary(Long itineraryId) {
        return (root, query, cb) -> cb.equal(root.get("itinerary").get("id"), itineraryId);
    }
}
