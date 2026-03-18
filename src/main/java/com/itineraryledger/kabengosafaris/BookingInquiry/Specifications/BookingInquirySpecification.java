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
}
