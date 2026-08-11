package com.itineraryledger.kabengosafaris.Safari.Specifications;

import java.time.LocalDate;
import java.util.List;

import com.itineraryledger.kabengosafaris.Safari.Enums.SafariPhase;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;

import lombok.Data;

/**
 * Everything a caller can narrow the safari list by, in one object.
 *
 * It exists so the rows, the counters and the record walk are asked the same
 * question — and so adding a filter is one field rather than four signatures.
 * Spring binds it straight from the query string with {@code @ModelAttribute},
 * so the parameter names on the wire are unchanged.
 */
@Data
public class SafariFilter {

    /** Free text across name, code and the start and end points. */
    private String keyword;

    private String name;
    private String code;

    private SafariState state;
    private List<SafariState> states;

    /** Where the safari is in time. Derived from its dates, not stored. */
    private SafariPhase phase;

    private String startLocation;
    private String endLocation;

    private LocalDate startDateFrom;
    private LocalDate startDateTo;

    private Boolean isActive;

    /**
     * Obfuscated ids, as the list page sends them.
     *
     * They were missing, so the Customer and Itinerary filters posted a
     * parameter nothing read: the drawer showed "Customer: Claire Bagnols" as an
     * active filter while the list went on showing everybody's safaris.
     */
    private String customerId;
    private String itineraryId;

    /** The states asked for, however they were spelled. */
    public List<SafariState> allStates() {
        List<SafariState> out = new java.util.ArrayList<>();
        if (states != null) states.stream().filter(java.util.Objects::nonNull).forEach(out::add);
        if (state != null && !out.contains(state)) out.add(state);
        return out;
    }
}
