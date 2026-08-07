package com.itineraryledger.kabengosafaris.Itinerary.DTOs;

import lombok.*;

/**
 * How much of an itinerary to copy.
 *
 * Every flag defaults to true, so a POST with no body at all is a complete copy
 * — the common case stays the simplest call. The flags exist for the variants a
 * catalogue is actually built from: the same route with different lodges, or the
 * same trip priced for a different party.
 *
 * They are not a depth setting. "Keep the parks, drop the accommodations" is an
 * exclusion, not a level, and a level enum could not express it.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DuplicateItineraryDTO {

    /**
     * Name for the copy. Left out, the server takes '<name> (Copy)', then
     * '(Copy 2)' and up — names are unique on this table.
     */
    private String name;

    /**
     * Copy the days: their text, route, distance and meals, plus the standalone
     * activities on each. Off, the copy is a shell — its own fields only, and
     * everything below is moot.
     */
    @Builder.Default
    private Boolean includeDays = true;

    /** Copy each day's park visits, with the park activities chosen for them. */
    @Builder.Default
    private Boolean includeParks = true;

    /** Copy the park fee categories picked for each visit. Needs includeParks. */
    @Builder.Default
    private Boolean includeParkTariffs = true;

    /** Copy where the guests sleep. Off for a copy that swaps its lodges. */
    @Builder.Default
    private Boolean includeAccommodations = true;

    /** Copy the nationality × age bands the itinerary is priced for. */
    @Builder.Default
    private Boolean includePax = true;

    /* null means "not sent", which is the same as yes */

    public boolean days() { return includeDays == null || includeDays; }
    public boolean parks() { return days() && (includeParks == null || includeParks); }
    public boolean parkTariffs() { return parks() && (includeParkTariffs == null || includeParkTariffs); }
    public boolean accommodations() { return days() && (includeAccommodations == null || includeAccommodations); }
    public boolean pax() { return includePax == null || includePax; }
}
