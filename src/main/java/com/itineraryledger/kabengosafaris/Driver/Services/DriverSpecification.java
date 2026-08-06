package com.itineraryledger.kabengosafaris.Driver.Services;

import com.itineraryledger.kabengosafaris.Driver.Entity.Driver;
import com.itineraryledger.kabengosafaris.Driver.Enums.DriverStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class DriverSpecification {

    public static Specification<Driver> firstNameLike(String firstName) {
        return (root, query, cb) -> {
            if (firstName == null || firstName.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("firstName")), "%" + firstName.toLowerCase() + "%");
        };
    }

    public static Specification<Driver> lastNameLike(String lastName) {
        return (root, query, cb) -> {
            if (lastName == null || lastName.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("lastName")), "%" + lastName.toLowerCase() + "%");
        };
    }

    public static Specification<Driver> phoneLike(String phone) {
        return (root, query, cb) -> {
            if (phone == null || phone.isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("phone")), "%" + phone.toLowerCase() + "%");
        };
    }

    public static Specification<Driver> hasStatus(DriverStatus status) {
        return (root, query, cb) -> {
            if (status == null) return cb.conjunction();
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Driver> isActive(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) return cb.conjunction();
            return cb.equal(root.get("isActive"), isActive);
        };
    }

    public static Specification<Driver> licenseExpired(Boolean expired) {
        return (root, query, cb) -> {
            if (expired == null) return cb.conjunction();
            if (expired) {
                return cb.and(
                    cb.isNotNull(root.get("licenseExpiryDate")),
                    cb.lessThan(root.get("licenseExpiryDate"), LocalDate.now())
                );
            } else {
                return cb.or(
                    cb.isNull(root.get("licenseExpiryDate")),
                    cb.greaterThanOrEqualTo(root.get("licenseExpiryDate"), LocalDate.now())
                );
            }
        };
    }

    public static Specification<Driver> talaExpired(Boolean expired) {
        return (root, query, cb) -> {
            if (expired == null) return cb.conjunction();
            if (expired) {
                return cb.and(
                    cb.isNotNull(root.get("talaExpiryDate")),
                    cb.lessThan(root.get("talaExpiryDate"), LocalDate.now())
                );
            } else {
                return cb.or(
                    cb.isNull(root.get("talaExpiryDate")),
                    cb.greaterThanOrEqualTo(root.get("talaExpiryDate"), LocalDate.now())
                );
            }
        };
    }

    public static Specification<Driver> tourGuideIdExpired(Boolean expired) {
        return (root, query, cb) -> {
            if (expired == null) return cb.conjunction();
            if (expired) {
                return cb.and(
                    cb.isNotNull(root.get("tourGuideIdExpiryDate")),
                    cb.lessThan(root.get("tourGuideIdExpiryDate"), LocalDate.now())
                );
            } else {
                return cb.or(
                    cb.isNull(root.get("tourGuideIdExpiryDate")),
                    cb.greaterThanOrEqualTo(root.get("tourGuideIdExpiryDate"), LocalDate.now())
                );
            }
        };
    }

    public static Specification<Driver> keyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isEmpty()) return cb.conjunction();
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("firstName")), pattern),
                cb.like(cb.lower(root.get("lastName")), pattern),
                cb.like(cb.lower(root.get("phone")), pattern),
                cb.like(cb.lower(root.get("licenseNumber")), pattern),
                cb.like(cb.lower(root.get("talaLicenseNumber")), pattern),
                cb.like(cb.lower(root.get("tourGuideId")), pattern)
            );
        };
    }

    /** Rows created on or after `moment` — the recency counters. */
    public static Specification<Driver> createdAfter(java.time.LocalDateTime moment) {
        return (root, query, cb) ->
            moment == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), moment);
    }
}
