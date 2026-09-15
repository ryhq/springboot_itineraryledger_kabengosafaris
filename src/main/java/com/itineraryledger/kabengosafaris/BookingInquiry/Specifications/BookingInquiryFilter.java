package com.itineraryledger.kabengosafaris.BookingInquiry.Specifications;

import com.itineraryledger.kabengosafaris.Attribution.AcquisitionChannel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.itineraryledger.kabengosafaris.BookingInquiry.Entity.InquiryStatus;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;

import lombok.Data;

/**
 * Everything a caller can narrow the inquiry list by, in one object.
 *
 * The rows, the stat cards and the record walk are all built from this, so a card
 * cannot report a figure the table would contradict. Bound from the query string
 * with {@code @ModelAttribute}, so every parameter the old signature took is
 * still spelled the same on the wire.
 */
@Data
public class BookingInquiryFilter {

    /** Free text across the name, the email, the country and the message. */
    private String keyword;

    private String email;
    private String country;
    private List<String> countries;

    private InquiryStatus status;
    private List<InquiryStatus> statuses;

    private BudgetCategory budgetCategory;
    private List<BudgetCategory> budgetCategories;

    private TripType tripType;
    private List<TripType> tripTypes;

    /** Obfuscated ids, as the list page sends them. */
    private String itineraryId;
    private String customerId;

    /**
     * What needs doing, which is what the list is actually for.
     *
     * unanswered — nobody has replied yet. stale — new and more than three days
     * old, which is the one that costs bookings. noPhone — nothing to call them
     * on. travellingSoon — their preferred start is inside a month, so a slow
     * reply is worse than usual.
     */
    private List<String> queues;

    /* Where they came from. Multi-value like every other dimension: OR inside, AND across. */
    private AcquisitionChannel channel;
    private List<AcquisitionChannel> channels;

    /** Exact campaign labels, so one ad's leads can be counted against that ad's spend. */
    private List<String> campaigns;

    /** Exact utm_source values, for when a platform runs several campaigns. */
    private List<String> sources;

    /**
     * Whether the click was bought, and whether anything was recorded at all.
     *
     * A dimension of its own rather than another work queue: the queues are OR'd
     * together, so "waiting over 3 days" plus "came from an ad" would return the
     * union of the two and answer neither question. Here it ANDs, and "paid" and
     * "untracked" cannot both be true, so OR inside the dimension is free.
     */
    private List<String> tracking;

    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;

    /** When they want to travel, rather than when they asked. */
    private LocalDate startingAfter;
    private LocalDate startingBefore;

    public List<InquiryStatus> allStatuses() {
        List<InquiryStatus> out = new ArrayList<>();
        if (statuses != null) statuses.stream().filter(Objects::nonNull).forEach(out::add);
        if (status != null && !out.contains(status)) out.add(status);
        return out;
    }

    public List<BudgetCategory> allBudgetCategories() {
        List<BudgetCategory> out = new ArrayList<>();
        if (budgetCategories != null) budgetCategories.stream().filter(Objects::nonNull).forEach(out::add);
        if (budgetCategory != null && !out.contains(budgetCategory)) out.add(budgetCategory);
        return out;
    }

    public List<TripType> allTripTypes() {
        List<TripType> out = new ArrayList<>();
        if (tripTypes != null) tripTypes.stream().filter(Objects::nonNull).forEach(out::add);
        if (tripType != null && !out.contains(tripType)) out.add(tripType);
        return out;
    }

    public List<String> allCountries() {
        List<String> out = new ArrayList<>();
        if (countries != null) countries.stream().filter(c -> c != null && !c.isBlank()).forEach(out::add);
        if (country != null && !country.isBlank() && !out.contains(country)) out.add(country);
        return out;
    }

    public List<AcquisitionChannel> allChannels() {
        List<AcquisitionChannel> out = new ArrayList<>();
        if (channels != null) channels.stream().filter(Objects::nonNull).forEach(out::add);
        if (channel != null && !out.contains(channel)) out.add(channel);
        return out;
    }

    public boolean wants(String queue) {
        return queues != null && queues.contains(queue);
    }
}
