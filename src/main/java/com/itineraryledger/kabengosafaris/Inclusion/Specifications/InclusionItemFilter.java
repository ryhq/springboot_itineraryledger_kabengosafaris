package com.itineraryledger.kabengosafaris.Inclusion.Specifications;

import java.util.List;

import lombok.Data;

/**
 * Everything a caller can narrow the inclusion catalogue by.
 *
 * Every dimension here has a counter on the list page and every counter is reachable as a filter —
 * a figure nobody can click is decoration, and a filter nothing counts is a guess.
 */
@Data
public class InclusionItemFilter {

    /** Free text across the label, the category and the internal note. */
    private String keyword;

    private String category;
    private List<String> categories;

    private List<String> statuses;
    private Boolean isActive;

    /** standard · optional · included-by-default · excluded-by-default */
    private List<String> defaults;

    /** claims · no-claim — whether the line says something a priced document can be checked against */
    private List<String> claims;

    /** in-use · unused — whether any itinerary has a row for it */
    private List<String> usage;
}
