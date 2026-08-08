package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.DTOs;

import lombok.*;

/**
 * How to copy a day.
 *
 * Days 4 to 6 of a Serengeti stay differ by a sentence, so the copy exists to
 * save retyping the park visit, its fees, its game drives and the lodge. Every
 * flag defaults to true and `copies` to one, so a POST with no body copies the
 * day once, whole.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DuplicateItineraryDayDTO {

    /** How many copies, 1–20. Bounded by the itinerary's free day slots. */
    @Builder.Default
    private Integer copies = 1;

    /**
     * AFTER puts the copies directly behind the source and renumbers the rest —
     * which is what "three nights here" means. END appends them.
     */
    @Builder.Default
    private String placement = "AFTER";

    /** Copy the park visits, with the park activities chosen for them. */
    @Builder.Default
    private Boolean includeParks = true;

    /** Copy the fee categories picked for each park visit. Needs includeParks. */
    @Builder.Default
    private Boolean includeParkTariffs = true;

    /** Copy the day's standalone activities. */
    @Builder.Default
    private Boolean includeActivities = true;

    /** Copy where the guests sleep that night. */
    @Builder.Default
    private Boolean includeAccommodations = true;

    /* null means "not sent", which is the same as yes */

    public boolean parks() { return includeParks == null || includeParks; }
    public boolean parkTariffs() { return parks() && (includeParkTariffs == null || includeParkTariffs); }
    public boolean activities() { return includeActivities == null || includeActivities; }
    public boolean accommodations() { return includeAccommodations == null || includeAccommodations; }
    public int copyCount() { return copies == null ? 1 : copies; }
    public boolean after() { return placement == null || !"END".equalsIgnoreCase(placement); }
}
