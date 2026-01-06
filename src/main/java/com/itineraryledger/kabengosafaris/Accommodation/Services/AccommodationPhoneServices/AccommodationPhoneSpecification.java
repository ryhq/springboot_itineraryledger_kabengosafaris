package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationPhoneServices;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationPhone;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationPhone.PhoneType;
import org.springframework.data.jpa.domain.Specification;

/**
 * AccommodationPhoneSpecification - Specifications for filtering accommodation phones
 */
public class AccommodationPhoneSpecification {

    /**
     * Filter by accommodation ID
     */
    public static Specification<AccommodationPhone> hasAccommodationId(Long accommodationId) {
        return (root, query, cb) -> {
            if (accommodationId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("accommodation").get("id"), accommodationId);
        };
    }

    /**
     * Filter by phone number (partial match)
     */
    public static Specification<AccommodationPhone> phoneNumberLike(String phoneNumber) {
        return (root, query, cb) -> {
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("phoneNumber")), "%" + phoneNumber.toLowerCase() + "%");
        };
    }

    /**
     * Filter by country code
     */
    public static Specification<AccommodationPhone> hasCountryCode(String countryCode) {
        return (root, query, cb) -> {
            if (countryCode == null || countryCode.isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("countryCode"), countryCode);
        };
    }

    /**
     * Filter by phone type
     */
    public static Specification<AccommodationPhone> hasPhoneType(PhoneType phoneType) {
        return (root, query, cb) -> {
            if (phoneType == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("phoneType"), phoneType);
        };
    }

    /**
     * Filter by primary status
     */
    public static Specification<AccommodationPhone> isPrimary(Boolean isPrimary) {
        return (root, query, cb) -> {
            if (isPrimary == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isPrimary"), isPrimary);
        };
    }

    /**
     * Filter by WhatsApp status
     */
    public static Specification<AccommodationPhone> isWhatsApp(Boolean isWhatsApp) {
        return (root, query, cb) -> {
            if (isWhatsApp == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isWhatsApp"), isWhatsApp);
        };
    }

    /**
     * Filter by active status
     */
    public static Specification<AccommodationPhone> isActive(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isActive"), isActive);
        };
    }

    /**
     * Filter by label (partial match)
     */
    public static Specification<AccommodationPhone> labelLike(String label) {
        return (root, query, cb) -> {
            if (label == null || label.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("label")), "%" + label.toLowerCase() + "%");
        };
    }

    /**
     * Search keyword across multiple fields (phoneNumber, label, operatingHours)
     */
    public static Specification<AccommodationPhone> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isEmpty()) {
                return cb.conjunction();
            }
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("phoneNumber")), likePattern),
                cb.like(cb.lower(root.get("label")), likePattern),
                cb.like(cb.lower(root.get("operatingHours")), likePattern)
            );
        };
    }
}
