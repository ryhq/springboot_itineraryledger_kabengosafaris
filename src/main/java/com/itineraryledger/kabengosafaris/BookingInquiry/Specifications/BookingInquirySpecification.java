package com.itineraryledger.kabengosafaris.BookingInquiry.Specifications;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.BookingInquiry.Entity.BookingInquiry;
import com.itineraryledger.kabengosafaris.BookingInquiry.Entity.InquiryStatus;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;

public class BookingInquirySpecification {

    public static Specification<BookingInquiry> byStatus(InquiryStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<BookingInquiry> byBudgetCategory(BudgetCategory budgetCategory) {
        return (root, query, cb) -> budgetCategory == null ? cb.conjunction() : cb.equal(root.get("budgetCategory"), budgetCategory);
    }

    public static Specification<BookingInquiry> byTripType(TripType tripType) {
        return (root, query, cb) -> tripType == null ? cb.conjunction() : cb.equal(root.get("tripType"), tripType);
    }

    public static Specification<BookingInquiry> byEmail(String email) {
        return (root, query, cb) -> {
            if (email == null || email.trim().isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase().trim() + "%");
        };
    }

    public static Specification<BookingInquiry> byCountry(String country) {
        return (root, query, cb) -> {
            if (country == null || country.trim().isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("country")), "%" + country.toLowerCase().trim() + "%");
        };
    }

    public static Specification<BookingInquiry> createdAfter(LocalDateTime dateTime) {
        return (root, query, cb) -> dateTime == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), dateTime);
    }

    public static Specification<BookingInquiry> createdBefore(LocalDateTime dateTime) {
        return (root, query, cb) -> dateTime == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("createdAt"), dateTime);
    }

    public static Specification<BookingInquiry> byItineraryId(Long itineraryId) {
        return (root, query, cb) -> itineraryId == null ? cb.conjunction() : cb.equal(root.get("itinerary").get("id"), itineraryId);
    }

    public static Specification<BookingInquiry> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) return cb.conjunction();
            String pattern = "%" + keyword.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("firstName")), pattern),
                cb.like(cb.lower(root.get("lastName")), pattern),
                cb.like(cb.lower(root.get("email")), pattern),
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("itineraryName")), pattern)
            );
        };
    }

    /* Multi-value dimensions: OR inside, AND across. */

    public static Specification<BookingInquiry> byStatuses(java.util.List<InquiryStatus> statuses) {
        return (root, query, cb) -> statuses == null || statuses.isEmpty()
            ? cb.conjunction()
            : root.get("status").in(statuses);
    }

    public static Specification<BookingInquiry> byBudgetCategories(
            java.util.List<com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory> values) {
        return (root, query, cb) -> values == null || values.isEmpty()
            ? cb.conjunction()
            : root.get("budgetCategory").in(values);
    }

    public static Specification<BookingInquiry> byTripTypes(
            java.util.List<com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType> values) {
        return (root, query, cb) -> values == null || values.isEmpty()
            ? cb.conjunction()
            : root.get("tripType").in(values);
    }

    public static Specification<BookingInquiry> byCountries(java.util.List<String> countries) {
        return (root, query, cb) -> countries == null || countries.isEmpty()
            ? cb.conjunction()
            : cb.lower(root.get("country")).in(countries.stream().map(String::toLowerCase).toList());
    }

    public static Specification<BookingInquiry> byCustomerId(Long customerId) {
        return (root, query, cb) -> customerId == null
            ? cb.conjunction()
            : cb.equal(root.get("customer").get("id"), customerId);
    }

    /* The work queues — what the list is actually for. */

    /**
     * Nobody has replied yet.
     *
     * NEW only: once somebody has been contacted the ball is with the customer,
     * and a quoted inquiry is being worked whether or not it closes.
     */
    public static Specification<BookingInquiry> unanswered() {
        return (root, query, cb) -> cb.equal(root.get("status"), InquiryStatus.NEW);
    }

    /**
     * New, and older than the window. This is the one that costs bookings —
     * somebody asked for a safari and nobody has spoken to them since.
     */
    public static Specification<BookingInquiry> staleFor(int days) {
        return (root, query, cb) -> cb.and(
            cb.equal(root.get("status"), InquiryStatus.NEW),
            cb.lessThan(root.get("createdAt"), LocalDateTime.now().minusDays(days)));
    }

    /** No phone number: they can only be emailed, which is slower. */
    public static Specification<BookingInquiry> missingPhone() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("phone")),
            cb.equal(cb.trim(root.get("phone")), ""));
    }

    /**
     * Travelling soon and not yet converted — a slow reply here is worse than
     * usual, because the dates run out whether or not we answer.
     */
    public static Specification<BookingInquiry> travellingWithin(int days) {
        return (root, query, cb) -> cb.and(
            cb.isNotNull(root.get("preferredStartDate")),
            cb.between(root.get("preferredStartDate"),
                java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(days)),
            cb.not(root.get("status").in(InquiryStatus.CONVERTED, InquiryStatus.LOST)));
    }

    public static Specification<BookingInquiry> startingAfter(java.time.LocalDate date) {
        return (root, query, cb) -> date == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("preferredStartDate"), date);
    }

    public static Specification<BookingInquiry> startingBefore(java.time.LocalDate date) {
        return (root, query, cb) -> date == null
            ? cb.conjunction()
            : cb.lessThanOrEqualTo(root.get("preferredStartDate"), date);
    }

    /** Already a customer of ours — the inquiry did its job. */
    public static Specification<BookingInquiry> converted(boolean converted) {
        return (root, query, cb) -> converted
            ? cb.isNotNull(root.get("customer"))
            : cb.isNull(root.get("customer"));
    }
}
