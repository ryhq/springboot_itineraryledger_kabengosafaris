package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRoomStandardServices;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomStandard;
import org.springframework.data.jpa.domain.Specification;

/**
 * AccommodationRoomStandardSpecification - Specification for filtering accommodation room standards
 */
public class AccommodationRoomStandardSpecification {

    /**
     * Filter by accommodation ID
     */
    public static Specification<AccommodationRoomStandard> hasAccommodationId(Long accommodationId) {
        return (root, query, cb) -> cb.equal(root.get("accommodation").get("id"), accommodationId);
    }

    /**
     * Filter by name (partial match, case-insensitive)
     */
    public static Specification<AccommodationRoomStandard> hasName(String name) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    /**
     * Filter by view type (partial match, case-insensitive)
     */
    public static Specification<AccommodationRoomStandard> hasViewType(String viewType) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("viewType")), "%" + viewType.toLowerCase() + "%");
    }

    /**
     * Filter by floor level (partial match, case-insensitive)
     */
    public static Specification<AccommodationRoomStandard> hasFloorLevel(String floorLevel) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("floorLevel")), "%" + floorLevel.toLowerCase() + "%");
    }

    /**
     * Filter by minimum occupancy
     */
    public static Specification<AccommodationRoomStandard> hasMinOccupancy(Integer minOccupancy) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("maxOccupancy"), minOccupancy);
    }

    /**
     * Filter by maximum occupancy
     */
    public static Specification<AccommodationRoomStandard> hasMaxOccupancy(Integer maxOccupancy) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("maxOccupancy"), maxOccupancy);
    }

    /**
     * Filter by active status
     */
    public static Specification<AccommodationRoomStandard> isActive(Boolean isActive) {
        return (root, query, cb) -> cb.equal(root.get("isActive"), isActive);
    }

    /**
     * Search by keyword across name, description, amenities, and view type
     */
    public static Specification<AccommodationRoomStandard> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(root.get("amenities")), pattern),
                cb.like(cb.lower(root.get("viewType")), pattern)
            );
        };
    }
}
