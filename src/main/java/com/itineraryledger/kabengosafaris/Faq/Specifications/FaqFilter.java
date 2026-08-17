package com.itineraryledger.kabengosafaris.Faq.Specifications;

import java.util.List;

import lombok.Data;

/**
 * Everything a caller can narrow the FAQ list by.
 *
 * Every dimension here has a counter on the list page and every counter here is reachable as a
 * filter — a figure nobody can click is decoration, and a filter nothing counts is a guess.
 */
@Data
public class FaqFilter {
    /** Free text across the question, the answer and the category. */
    private String keyword;

    private String category;

    private List<String> statuses;
    private Boolean isActive;

    /** Data quality: no-category · has-category · thin-answer */
    private List<String> qualities;

    /** Freshness: added-7 · added-30 · updated-7 · stale-90 */
    private List<String> recency;
}
