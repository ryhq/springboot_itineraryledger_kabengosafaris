package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationServices;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationCategory;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationType;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * AccommodationSpecification - Provides reusable Specification objects for filtering Accommodation entities
 *
 * Specification allows for dynamic, type-safe query construction using the Criteria API
 * Each method returns a Specification<Accommodation> that can be combined with other specifications
 */
public class AccommodationSpecification {

    /**
     * Filter by accommodation name (case-insensitive partial match)
     */
    public static Specification<Accommodation> nameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isEmpty()) {
                return cb.conjunction(); // No filter applied
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    /**
     * Filter by slug (case-insensitive partial match)
     */
    public static Specification<Accommodation> slugLike(String slug) {
        return (root, query, cb) -> {
            if (slug == null || slug.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("slug")), "%" + slug.toLowerCase() + "%");
        };
    }

    /**
     * Filter by accommodation type
     */
    public static Specification<Accommodation> hasAccommodationType(AccommodationType accommodationType) {
        return (root, query, cb) -> {
            if (accommodationType == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("accommodationType"), accommodationType);
        };
    }

    /**
     * Filter by accommodation category
     */
    public static Specification<Accommodation> hasCategory(AccommodationCategory category) {
        return (root, query, cb) -> {
            if (category == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("category"), category);
        };
    }

    /**
     * Filter by region (case-insensitive partial match)
     */
    public static Specification<Accommodation> regionLike(String region) {
        return (root, query, cb) -> {
            if (region == null || region.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("region")), "%" + region.toLowerCase() + "%");
        };
    }

    /**
     * Filter by exact region (case-insensitive)
     */
    public static Specification<Accommodation> hasRegion(String region) {
        return (root, query, cb) -> {
            if (region == null || region.isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(cb.lower(root.get("region")), region.toLowerCase());
        };
    }

    /**
     * Filter by district (case-insensitive partial match)
     */
    public static Specification<Accommodation> districtLike(String district) {
        return (root, query, cb) -> {
            if (district == null || district.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("district")), "%" + district.toLowerCase() + "%");
        };
    }

    /**
     * Filter by exact district (case-insensitive)
     */
    public static Specification<Accommodation> hasDistrict(String district) {
        return (root, query, cb) -> {
            if (district == null || district.isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(cb.lower(root.get("district")), district.toLowerCase());
        };
    }

    /**
     * Filter by star rating
     */
    public static Specification<Accommodation> hasStarRating(Integer starRating) {
        return (root, query, cb) -> {
            if (starRating == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("starRating"), starRating);
        };
    }

    /**
     * Filter by minimum star rating (greater than or equal)
     */
    public static Specification<Accommodation> minStarRating(Integer minStarRating) {
        return (root, query, cb) -> {
            if (minStarRating == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("starRating"), minStarRating);
        };
    }

    /**
     * Filter by maximum star rating (less than or equal)
     */
    public static Specification<Accommodation> maxStarRating(Integer maxStarRating) {
        return (root, query, cb) -> {
            if (maxStarRating == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("starRating"), maxStarRating);
        };
    }

    /**
     * Filter by active status
     */
    public static Specification<Accommodation> isActive(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isActive"), isActive);
        };
    }

    /**
     * Filter by web active status
     */
    public static Specification<Accommodation> isWebActive(Boolean isWebActive) {
        return (root, query, cb) -> {
            if (isWebActive == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isWebActive"), isWebActive);
        };
    }

    /**
     * Filter by headquarters status
     */
    public static Specification<Accommodation> isHeadquarters(Boolean isHeadquarters) {
        return (root, query, cb) -> {
            if (isHeadquarters == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isHeadquarters"), isHeadquarters);
        };
    }

    /**
     * Filter by has branch status
     */
    public static Specification<Accommodation> hasBranch(Boolean hasBranch) {
        return (root, query, cb) -> {
            if (hasBranch == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("hasBranch"), hasBranch);
        };
    }

    /**
     * Filter by parent accommodation ID
     * Returns branches of a specific parent
     */
    public static Specification<Accommodation> hasParentId(Long parentId) {
        return (root, query, cb) -> {
            if (parentId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("parentAccommodation").get("id"), parentId);
        };
    }

    /**
     * Filter accommodations with no parent (headquarters only)
     */
    public static Specification<Accommodation> hasNoParent() {
        return (root, query, cb) -> cb.isNull(root.get("parentAccommodation"));
    }

    /**
     * Filter by minimum number of rooms
     */
    public static Specification<Accommodation> minTotalRooms(Integer minRooms) {
        return (root, query, cb) -> {
            if (minRooms == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("totalRooms"), minRooms);
        };
    }

    /**
     * Filter by maximum number of rooms
     */
    public static Specification<Accommodation> maxTotalRooms(Integer maxRooms) {
        return (root, query, cb) -> {
            if (maxRooms == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("totalRooms"), maxRooms);
        };
    }

    /**
     * Filter by minimum guest capacity
     */
    public static Specification<Accommodation> minMaxGuests(Integer minGuests) {
        return (root, query, cb) -> {
            if (minGuests == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("maxGuests"), minGuests);
        };
    }

    /**
     * Filter by maximum guest capacity
     */
    public static Specification<Accommodation> maxMaxGuests(Integer maxGuests) {
        return (root, query, cb) -> {
            if (maxGuests == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("maxGuests"), maxGuests);
        };
    }

    /**
     * Filter by location (case-insensitive partial match)
     */
    public static Specification<Accommodation> locationLike(String location) {
        return (root, query, cb) -> {
            if (location == null || location.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%");
        };
    }

    /**
     * Filter by tags (case-sensitive partial match)
     * Note: Uses LIKE without LOWER() because tags is @Lob field
     * Search is case-sensitive for CLOB compatibility
     */
    public static Specification<Accommodation> tagsLike(String tags) {
        return (root, query, cb) -> {
            if (tags == null || tags.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(root.get("tags"), "%" + tags + "%");
        };
    }

    /**
     * Filter by amenities (case-sensitive partial match)
     * Note: Uses LIKE without LOWER() because amenities is @Lob field
     * Search is case-sensitive for CLOB compatibility
     */
    public static Specification<Accommodation> amenitiesLike(String amenities) {
        return (root, query, cb) -> {
            if (amenities == null || amenities.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(root.get("amenities"), "%" + amenities + "%");
        };
    }

    /**
     * Filter accommodations within a geographic bounding box
     *
     * @param minLat Minimum latitude
     * @param maxLat Maximum latitude
     * @param minLon Minimum longitude
     * @param maxLon Maximum longitude
     */
    public static Specification<Accommodation> withinBounds(
        BigDecimal minLat, BigDecimal maxLat,
        BigDecimal minLon, BigDecimal maxLon
    ) {
        return (root, query, cb) -> {
            if (minLat == null || maxLat == null || minLon == null || maxLon == null) {
                return cb.conjunction();
            }
            return cb.and(
                cb.greaterThanOrEqualTo(root.get("latitude"), minLat),
                cb.lessThanOrEqualTo(root.get("latitude"), maxLat),
                cb.greaterThanOrEqualTo(root.get("longitude"), minLon),
                cb.lessThanOrEqualTo(root.get("longitude"), maxLon)
            );
        };
    }

    /**
     * Filter accommodations that have GPS coordinates
     */
    public static Specification<Accommodation> hasCoordinates() {
        return (root, query, cb) -> cb.and(
            cb.isNotNull(root.get("latitude")),
            cb.isNotNull(root.get("longitude"))
        );
    }

    /**
     * Search across multiple text fields (name, location, shortDescription)
     * Useful for general search functionality
     * Note: Excludes @Lob fields (details, amenities, tags, nearbyAttractions)
     * because MySQL doesn't support LOWER() function on CLOB types
     */
    public static Specification<Accommodation> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isEmpty()) {
                return cb.conjunction();
            }
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), likePattern),
                cb.like(cb.lower(root.get("location")), likePattern),
                cb.like(cb.lower(root.get("shortDescription")), likePattern),
                cb.like(cb.lower(root.get("region")), likePattern),
                cb.like(cb.lower(root.get("district")), likePattern)
            );
        };
    }

    /**
     * Filter accommodations with images
     * Joins with images collection and checks if it's not empty
     */
    public static Specification<Accommodation> hasImages() {
        return (root, query, cb) -> {
            var imagesJoin = root.join("images", JoinType.INNER);
            query.distinct(true);
            return cb.isNotNull(imagesJoin.get("id"));
        };
    }

    /**
     * Filter accommodations with rates
     * Joins with rates collection and checks if it's not empty
     */
    public static Specification<Accommodation> hasRates() {
        return (root, query, cb) -> {
            var ratesJoin = root.join("rates", JoinType.INNER);
            query.distinct(true);
            return cb.isNotNull(ratesJoin.get("id"));
        };
    }

    /**
     * Filter accommodations with specific TIN
     */
    public static Specification<Accommodation> hasTin(String tin) {
        return (root, query, cb) -> {
            if (tin == null || tin.isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("tin"), tin);
        };
    }

    /**
     * Filter accommodations with specific VRN
     */
    public static Specification<Accommodation> hasVrn(String vrn) {
        return (root, query, cb) -> {
            if (vrn == null || vrn.isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("vrn"), vrn);
        };
    }
}
