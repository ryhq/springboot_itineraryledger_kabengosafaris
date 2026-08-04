package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRoomTypeServices;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomType;
import org.springframework.data.jpa.domain.Specification;

/**
 * AccommodationRoomTypeSpecification - Specification for filtering accommodation room types
 */
public class AccommodationRoomTypeSpecification {

    /**
     * Filter by accommodation ID
     */
    public static Specification<AccommodationRoomType> hasAccommodationId(Long accommodationId) {
        return (root, query, cb) -> cb.equal(root.get("accommodation").get("id"), accommodationId);
    }

    /**
     * Filter by name (partial match, case-insensitive)
     */
    public static Specification<AccommodationRoomType> hasName(String name) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    /**
     * Filter by exact name
     */
    public static Specification<AccommodationRoomType> hasExactName(String name) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("name")), name.toLowerCase());
    }

    /**
     * Filter by minimum occupancy (greater than or equal to)
     */
    public static Specification<AccommodationRoomType> hasMinOccupancy(Integer minOccupancy) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("minOccupancy"), minOccupancy);
    }

    /**
     * Filter by maximum occupancy (less than or equal to)
     */
    public static Specification<AccommodationRoomType> hasMaxOccupancy(Integer maxOccupancy) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("maxOccupancy"), maxOccupancy);
    }

    /**
     * Filter by active status
     */
    public static Specification<AccommodationRoomType> isActive(Boolean isActive) {
        return (root, query, cb) -> cb.equal(root.get("isActive"), isActive);
    }

    /**
     * Search by keyword across name, description, and bed configuration
     */
    public static Specification<AccommodationRoomType> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(root.get("bedConfiguration")), pattern)
            );
        };
    }

    /** Recency, for the "new in the last N days" counters. */
    public static Specification<AccommodationRoomType> createdAfter(java.time.LocalDateTime moment) {
        return (root, query, cb) ->
            moment == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), moment);
    }
}
