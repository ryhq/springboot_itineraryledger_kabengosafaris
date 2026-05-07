package com.itineraryledger.kabengosafaris.Vendor.Specifications;

import com.itineraryledger.kabengosafaris.Vendor.Entity.Vendor;
import com.itineraryledger.kabengosafaris.Vendor.Enums.VendorType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/**
 * Mirrors the InvoiceSpecification idiom: each filter is a no-op when the
 * input is null/blank so callers can compose freely.
 */
public class VendorSpecification {

    private VendorSpecification() {}

    public static Specification<Vendor> byCode(String code) {
        return (root, query, cb) -> {
            if (code == null || code.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("code")), "%" + code.toLowerCase().trim() + "%");
        };
    }

    public static Specification<Vendor> byName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase().trim() + "%");
        };
    }

    public static Specification<Vendor> byType(VendorType type) {
        return (root, query, cb) -> type == null ? cb.conjunction() : cb.equal(root.get("type"), type);
    }

    public static Specification<Vendor> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null ? cb.conjunction() : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<Vendor> byCity(String city) {
        return (root, query, cb) -> {
            if (city == null || city.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("city")), "%" + city.toLowerCase().trim() + "%");
        };
    }

    public static Specification<Vendor> byCountry(String country) {
        return (root, query, cb) -> {
            if (country == null || country.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("country")), "%" + country.toLowerCase().trim() + "%");
        };
    }

    public static Specification<Vendor> searchByKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.toLowerCase().trim() + "%";
            Predicate name = cb.like(cb.lower(root.get("name")), like);
            Predicate code = cb.like(cb.lower(root.get("code")), like);
            Predicate contact = cb.like(cb.lower(root.get("contactPerson")), like);
            Predicate email = cb.like(cb.lower(root.get("email")), like);
            return cb.or(name, code, contact, email);
        };
    }
}
