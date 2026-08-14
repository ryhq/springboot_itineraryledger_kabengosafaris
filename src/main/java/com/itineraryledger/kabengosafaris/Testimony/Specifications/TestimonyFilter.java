package com.itineraryledger.kabengosafaris.Testimony.Specifications;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.itineraryledger.kabengosafaris.Testimony.Enums.TestimonySource;

import lombok.Data;

/**
 * Everything a caller can narrow the testimony list by, in one object.
 *
 * The rows, the stat cards and the record walk are all built from this, so a card cannot
 * report a figure the table would contradict, and prev/next cannot wander out of the set on
 * screen. Spring binds it with {@code @ModelAttribute}, so every parameter the old
 * signature took is still spelled the same on the wire — the v1 panel is still live against
 * this API.
 */
@Data
public class TestimonyFilter {

    /** Free text across author, title and the review itself. */
    private String keyword;

    private String authorName;
    private String authorCountry;

    private TestimonySource source;
    private List<TestimonySource> sources;

    private Integer rating;
    private List<Integer> ratings;
    private Integer minRating;
    private Integer maxRating;

    private Boolean isApproved;
    private Boolean isFeatured;
    private Boolean isVerifiedBooking;
    private Boolean isActive;

    /**
     * Where a review stands, as one dimension.
     *
     * "approved" / "pending" is the queue somebody actually works through; active/inactive
     * is whether it appears on the site at all. Contradictory pairs cancel to no
     * constraint, because selecting everything is how a filter gets cleared.
     */
    private List<String> statuses;

    /** featured · verified · answered — the flags a review can carry. */
    private List<String> flags;

    /**
     * Actionable gaps, each of which is also a card.
     *
     * A five-star review nobody approved is money left on the table; a one-star review
     * nobody answered is the one a prospective customer will read. Neither is visible in
     * any other column.
     */
    private List<String> qualities;

    private String sentimentTag;
    private String customerId;
    private String safariId;

    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;

    public List<TestimonySource> allSources() {
        List<TestimonySource> out = new ArrayList<>();
        if (sources != null) sources.stream().filter(Objects::nonNull).forEach(out::add);
        if (source != null && !out.contains(source)) out.add(source);
        return out;
    }

    public List<Integer> allRatings() {
        List<Integer> out = new ArrayList<>();
        if (ratings != null) ratings.stream().filter(Objects::nonNull).forEach(out::add);
        if (rating != null && !out.contains(rating)) out.add(rating);
        return out;
    }

    /** Approved, pending, or null when both or neither was asked for. */
    public Boolean resolvedApproved() {
        boolean yes = statuses != null && statuses.contains("approved");
        boolean no = statuses != null && statuses.contains("pending");
        if (yes ^ no) return yes;
        return isApproved;
    }

    public Boolean resolvedActive() {
        boolean yes = statuses != null && statuses.contains("active");
        boolean no = statuses != null && statuses.contains("inactive");
        if (yes ^ no) return yes;
        return isActive;
    }

    public boolean hasFlag(String flag) {
        return flags != null && flags.contains(flag);
    }

    public boolean wants(String quality) {
        return qualities != null && qualities.contains(quality);
    }
}
