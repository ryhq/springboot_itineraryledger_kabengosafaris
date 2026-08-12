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

    /**
     * Any of these types — the Type filter is multi-select, and a singular enum
     * param cannot answer "lodge or park authority".
     */
    public static Specification<Vendor> byTypes(java.util.List<VendorType> types) {
        return (root, query, cb) -> types == null || types.isEmpty()
            ? cb.conjunction()
            : root.get("type").in(types);
    }

    public static Specification<Vendor> byCities(java.util.List<String> cities) {
        return (root, query, cb) -> cities == null || cities.isEmpty()
            ? cb.conjunction()
            : cb.lower(root.get("city")).in(cities.stream().map(String::toLowerCase).toList());
    }

    public static Specification<Vendor> byCountries(java.util.List<String> countries) {
        return (root, query, cb) -> countries == null || countries.isEmpty()
            ? cb.conjunction()
            : cb.lower(root.get("country")).in(countries.stream().map(String::toLowerCase).toList());
    }

    public static Specification<Vendor> byCurrencies(java.util.List<String> currencies) {
        return (root, query, cb) -> currencies == null || currencies.isEmpty()
            ? cb.conjunction()
            : cb.upper(root.get("preferredCurrency")).in(
                currencies.stream().map(String::toUpperCase).toList());
    }

    /* Data-quality counters, each one clickable as a filter. */

    /** No email: nothing can be sent to them in writing. */
    public static Specification<Vendor> missingEmail() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("email")),
            cb.equal(cb.trim(root.get("email")), ""));
    }

    /** No phone: they cannot be reached when a booking changes today. */
    public static Specification<Vendor> missingPhone() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("phone")),
            cb.equal(cb.trim(root.get("phone")), ""));
    }

    /** No tax id: their invoices cannot be filed against a TIN. */
    public static Specification<Vendor> missingTaxId() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("taxId")),
            cb.equal(cb.trim(root.get("taxId")), ""));
    }

    public static Specification<Vendor> createdAfter(java.time.LocalDateTime since) {
        return (root, query, cb) -> since == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("createdAt"), since);
    }
}
