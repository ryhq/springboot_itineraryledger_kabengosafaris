package com.itineraryledger.kabengosafaris.Quote.Specifications;

import java.time.LocalDate;
import java.util.List;

import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteStatus;

import lombok.Data;

/**
 * Everything a caller can narrow the quote list by, in one object.
 *
 * It exists so the list, the counters and the record walk are asked the same
 * question. They used to be given the filters as eighteen positional arguments,
 * forwarded twice; the detail endpoint passed eighteen nulls to say "no
 * filters", and any new filter meant editing four signatures in step. Spring
 * binds this straight from the query string with {@code @ModelAttribute}, so
 * the parameter names on the wire are unchanged.
 *
 * <p>Both the singular and plural forms of the enum facets are accepted: the
 * list page sends {@code statuses=SENT,ACCEPTED} because a stat card can be
 * shift-clicked onto another, while older callers send {@code status=SENT}.
 * They combine as one OR, which is what "any of these" means.
 */
@Data
public class QuoteFilter {

    /** Free text across code, title, description, customer and itinerary. */
    private String keyword;

    private String quoteCode;
    private String title;

    private QuoteStatus status;
    private List<QuoteStatus> statuses;

    /** draft | pending | active | closed */
    private String statusGroup;
    private List<String> statusGroups;

    private String itineraryId;
    private String customerId;
    private String approverId;
    private String approvedById;
    private String createdById;
    private String updatedById;

    private Boolean isStoRate;
    private Boolean isActive;

    private LocalDate validOn;
    private LocalDate sentAfter;
    private LocalDate sentBefore;

    private Integer version;

    /** The statuses asked for, however they were spelled. */
    public List<QuoteStatus> allStatuses() {
        List<QuoteStatus> out = new java.util.ArrayList<>();
        if (statuses != null) statuses.stream().filter(java.util.Objects::nonNull).forEach(out::add);
        if (status != null && !out.contains(status)) out.add(status);
        return out;
    }

    /** The stages asked for, however they were spelled. */
    public List<String> allStatusGroups() {
        List<String> out = new java.util.ArrayList<>();
        if (statusGroups != null) statusGroups.stream()
            .filter(g -> g != null && !g.isBlank()).forEach(out::add);
        if (statusGroup != null && !statusGroup.isBlank() && !out.contains(statusGroup)) out.add(statusGroup);
        return out;
    }
}
