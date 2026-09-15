package com.itineraryledger.kabengosafaris.BookingInquiry.Specifications;

import com.itineraryledger.kabengosafaris.Attribution.AcquisitionChannel;

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

    /* ---------- where the lead came from ---------- */

    public static Specification<BookingInquiry> byChannel(AcquisitionChannel channel) {
        return (root, query, cb) -> channel == null
            ? cb.conjunction()
            : cb.equal(root.get("attribution").get("channel"), channel);
    }

    public static Specification<BookingInquiry> byChannels(java.util.List<AcquisitionChannel> channels) {
        return (root, query, cb) -> channels == null || channels.isEmpty()
            ? cb.conjunction()
            : root.get("attribution").get("channel").in(channels);
    }

    /**
     * The campaign is matched whole, not by prefix: a campaign name is an exact
     * label chosen when the ad was set up, and a LIKE would fold "serengeti-jul"
     * into "serengeti-july-retarget" and quietly merge two budgets.
     */
    public static Specification<BookingInquiry> byCampaigns(java.util.List<String> campaigns) {
        return (root, query, cb) -> campaigns == null || campaigns.isEmpty()
            ? cb.conjunction()
            : root.get("attribution").get("campaign").in(campaigns);
    }

    public static Specification<BookingInquiry> bySources(java.util.List<String> sources) {
        return (root, query, cb) -> sources == null || sources.isEmpty()
            ? cb.conjunction()
            : cb.lower(root.get("attribution").get("source")).in(
                sources.stream().map(v -> v.toLowerCase().trim()).toList());
    }

    /** Everything that cost money, so the spend question can be asked in one click. */
    public static Specification<BookingInquiry> paidChannels() {
        java.util.List<AcquisitionChannel> paid = java.util.Arrays.stream(AcquisitionChannel.values())
            .filter(AcquisitionChannel::isPaid)
            .toList();
        return (root, query, cb) -> root.get("attribution").get("channel").in(paid);
    }

    /**
     * "paid" and "untracked", OR'd with each other and AND'd with everything else.
     *
     * An unknown value narrows nothing rather than erroring: a bookmark from an older
     * panel should show a list, not a 400.
     */
    public static Specification<BookingInquiry> byTracking(java.util.List<String> tracking) {
        if (tracking == null || tracking.isEmpty()) return (root, query, cb) -> cb.conjunction();

        boolean paid = tracking.contains("paid");
        boolean untracked = tracking.contains("untracked");
        if (paid && untracked) {
            /* Every lead is one or the other or neither, so asking for both narrows nothing. */
            return (root, query, cb) -> cb.conjunction();
        }
        if (paid) return paidChannels();
        if (untracked) return untracked();
        return (root, query, cb) -> cb.conjunction();
    }

    /**
     * Leads with no arrival recorded at all: taken before this shipped, entered by
     * hand, or from a browser with storage blocked. Counted separately so a channel
     * breakdown is never mistaken for a complete one.
     */
    public static Specification<BookingInquiry> untracked() {
        return (root, query, cb) -> cb.isNull(root.get("attribution").get("channel"));
    }

}
