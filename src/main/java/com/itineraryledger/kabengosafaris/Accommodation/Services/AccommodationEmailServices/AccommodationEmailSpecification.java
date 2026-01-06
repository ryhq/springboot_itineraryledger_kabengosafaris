package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationEmailServices;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationEmail;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationEmail.EmailType;
import org.springframework.data.jpa.domain.Specification;

/**
 * AccommodationEmailSpecification - Provides reusable Specification objects for filtering AccommodationEmail entities
 */
public class AccommodationEmailSpecification {

    /**
     * Filter by accommodation ID
     */
    public static Specification<AccommodationEmail> hasAccommodationId(Long accommodationId) {
        return (root, query, cb) -> {
            if (accommodationId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("accommodation").get("id"), accommodationId);
        };
    }

    /**
     * Filter by email address (case-insensitive partial match)
     */
    public static Specification<AccommodationEmail> emailLike(String email) {
        return (root, query, cb) -> {
            if (email == null || email.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
        };
    }

    /**
     * Filter by email type
     */
    public static Specification<AccommodationEmail> hasEmailType(EmailType emailType) {
        return (root, query, cb) -> {
            if (emailType == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("emailType"), emailType);
        };
    }

    /**
     * Filter by primary status
     */
    public static Specification<AccommodationEmail> isPrimary(Boolean isPrimary) {
        return (root, query, cb) -> {
            if (isPrimary == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isPrimary"), isPrimary);
        };
    }

    /**
     * Filter by active status
     */
    public static Specification<AccommodationEmail> isActive(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isActive"), isActive);
        };
    }

    /**
     * Filter by label (case-insensitive partial match)
     */
    public static Specification<AccommodationEmail> labelLike(String label) {
        return (root, query, cb) -> {
            if (label == null || label.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("label")), "%" + label.toLowerCase() + "%");
        };
    }

    /**
     * Search across email and label fields
     */
    public static Specification<AccommodationEmail> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isEmpty()) {
                return cb.conjunction();
            }
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("email")), likePattern),
                cb.like(cb.lower(root.get("label")), likePattern)
            );
        };
    }
}
