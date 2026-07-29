package com.itineraryledger.kabengosafaris.Customer.Specifications;

import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerSource;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CustomerSpecification - JPA Specifications for Customer filtering
 *
 * Provides reusable Specification objects for filtering Customer entities.
 * Each method returns a Specification<Customer> that can be combined with other specifications.
 */
public class CustomerSpecification {

    // ========================
    // TEXT SEARCH
    // ========================

    public static Specification<Customer> codeLike(String code) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("code")), "%" + code.toLowerCase() + "%");
    }

    public static Specification<Customer> firstNameLike(String firstName) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("firstName")), "%" + firstName.toLowerCase() + "%");
    }

    public static Specification<Customer> lastNameLike(String lastName) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("lastName")), "%" + lastName.toLowerCase() + "%");
    }

    public static Specification<Customer> companyNameLike(String companyName) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("companyName")), "%" + companyName.toLowerCase() + "%");
    }

    // email/phone live on child tables (customer_emails / customer_phones) — join, and
    // deduplicate the customer rows the join multiplies
    public static Specification<Customer> emailLike(String email) {
        return (root, query, cb) -> {
            query.distinct(true);
            return cb.like(
                cb.lower(root.join("emails", jakarta.persistence.criteria.JoinType.LEFT).get("email")),
                "%" + email.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Customer> phoneLike(String phone) {
        return (root, query, cb) -> {
            query.distinct(true);
            return cb.like(
                root.join("phones", jakarta.persistence.criteria.JoinType.LEFT).get("phoneNumber"),
                "%" + phone + "%"
            );
        };
    }

    // ========================
    // COMBINED NAME SEARCH
    // ========================

    public static Specification<Customer> nameLike(String name) {
        String lowerName = "%" + name.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("firstName")), lowerName),
                cb.like(cb.lower(root.get("lastName")), lowerName),
                cb.like(cb.lower(root.get("companyName")), lowerName)
        );
    }

    // ========================
    // KEYWORD SEARCH (ALL FIELDS)
    // ========================

    public static Specification<Customer> searchKeyword(String keyword) {
        String lowerKeyword = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> {
            query.distinct(true);
            var emailJoin = root.join("emails", jakarta.persistence.criteria.JoinType.LEFT);
            var phoneJoin = root.join("phones", jakarta.persistence.criteria.JoinType.LEFT);
            return cb.or(
                cb.like(cb.lower(root.get("code")), lowerKeyword),
                cb.like(cb.lower(root.get("firstName")), lowerKeyword),
                cb.like(cb.lower(root.get("lastName")), lowerKeyword),
                cb.like(cb.lower(root.get("companyName")), lowerKeyword),
                cb.like(cb.lower(emailJoin.get("email")), lowerKeyword),
                cb.like(phoneJoin.get("phoneNumber"), lowerKeyword)
            );
        };
    }

    // ========================
    // CUSTOMER TYPE
    // ========================

    public static Specification<Customer> hasCustomerType(CustomerType customerType) {
        return (root, query, cb) -> cb.equal(root.get("customerType"), customerType);
    }

    // ========================
    // SOURCE
    // ========================

    public static Specification<Customer> hasSource(CustomerSource source) {
        return (root, query, cb) -> cb.equal(root.get("source"), source);
    }

    // ========================
    // LOCATION FILTERS
    // ========================

    public static Specification<Customer> nationalityLike(String nationality) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("nationality")), "%" + nationality.toLowerCase() + "%");
    }

    public static Specification<Customer> countryLike(String country) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("country")), "%" + country.toLowerCase() + "%");
    }

    public static Specification<Customer> cityLike(String city) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("city")), "%" + city.toLowerCase() + "%");
    }

    // ========================
    // STATUS FLAGS
    // ========================

    public static Specification<Customer> isActive(Boolean isActive) {
        return (root, query, cb) -> cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<Customer> isVip(Boolean isVip) {
        return (root, query, cb) -> cb.equal(root.get("isVip"), isVip);
    }

    public static Specification<Customer> isBlacklisted(Boolean isBlacklisted) {
        return (root, query, cb) -> cb.equal(root.get("isBlacklisted"), isBlacklisted);
    }

    // ========================
    // BOOKING STATISTICS
    // ========================

    public static Specification<Customer> minTotalBookings(Integer minBookings) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("totalBookings"), minBookings);
    }

    public static Specification<Customer> maxTotalBookings(Integer maxBookings) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("totalBookings"), maxBookings);
    }

    public static Specification<Customer> minTotalSpent(BigDecimal minSpent) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("totalSpent"), minSpent);
    }

    public static Specification<Customer> maxTotalSpent(BigDecimal maxSpent) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("totalSpent"), maxSpent);
    }

    // ========================
    // DATE FILTERS
    // ========================

    public static Specification<Customer> createdAfter(LocalDateTime date) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), date);
    }

    public static Specification<Customer> createdBefore(LocalDateTime date) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), date);
    }

    public static Specification<Customer> lastBookingAfter(LocalDateTime date) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("lastBookingDate"), date);
    }

    public static Specification<Customer> lastBookingBefore(LocalDateTime date) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("lastBookingDate"), date);
    }

    // ========================
    // NULL CHECKS
    // ========================

    public static Specification<Customer> hasBookings() {
        return (root, query, cb) -> cb.greaterThan(root.get("totalBookings"), 0);
    }

    public static Specification<Customer> hasNoBookings() {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("totalBookings")),
                cb.equal(root.get("totalBookings"), 0)
        );
    }

    // ========================
    // PREFERENCES
    // ========================

    public static Specification<Customer> hasPreferredLanguage(String language) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("preferredLanguage")), language.toLowerCase());
    }

    public static Specification<Customer> hasPreferredCurrency(String currency) {
        return (root, query, cb) -> cb.equal(cb.upper(root.get("preferredCurrency")), currency.toUpperCase());
    }
}
