package com.itineraryledger.kabengosafaris.Faq.Specifications;

import java.util.List;

import lombok.Data;

/** Everything a caller can narrow the FAQ list by. */
@Data
public class FaqFilter {
    /** Free text across the question and the answer. */
    private String keyword;
    private String category;
    private List<String> statuses;
    private Boolean isActive;
}
