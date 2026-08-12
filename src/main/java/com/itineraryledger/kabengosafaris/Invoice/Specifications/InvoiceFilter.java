package com.itineraryledger.kabengosafaris.Invoice.Specifications;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;

import lombok.Data;

/**
 * Everything a caller can narrow the invoice list by, in one object.
 *
 * The rows, the counters and the record walk are all built from this, so a card
 * cannot report a figure the table would contradict, and prev/next cannot wander
 * out of the set on screen. Spring binds it from the query string with
 * {@code @ModelAttribute}, so every parameter the old signature took is still
 * spelled the same on the wire.
 */
@Data
public class InvoiceFilter {

    /** Free text across the code, the title, the customer and the safari. */
    private String keyword;

    private String invoiceCode;
    private String title;

    private InvoiceStatus status;
    private List<InvoiceStatus> statuses;

    /**
     * Coarser than a status: what the office is actually asking for.
     *
     * draft · unpaid (sent, part paid, overdue) · paid · closed. Multi-value,
     * OR'd together, so "unpaid or overdue" is one question.
     */
    private String statusGroup;
    private List<String> statusGroups;

    /** Obfuscated ids, as the list page sends them. */
    private String customerId;
    private String safariId;
    private String createdById;
    private String updatedById;

    private Boolean isActive;

    /** Past its due date and neither paid nor cancelled. */
    private Boolean isOverdue;

    /** A second invoice raised because the trip changed after it was billed. */
    private Boolean isSupplement;

    /**
     * Actionable data quality, the same idea as the customer list's.
     *
     * dueSoon — falls due inside a week and is not settled. unsent — a draft
     * that has been sitting there. unpaid — money still outstanding.
     */
    private List<String> qualities;

    private LocalDate issueDateAfter;
    private LocalDate issueDateBefore;
    private LocalDate dueDateAfter;
    private LocalDate dueDateBefore;
    private LocalDate sentAfter;
    private LocalDate sentBefore;

    /** The statuses asked for, however they were spelled. */
    public List<InvoiceStatus> allStatuses() {
        List<InvoiceStatus> out = new ArrayList<>();
        if (statuses != null) statuses.stream().filter(Objects::nonNull).forEach(out::add);
        if (status != null && !out.contains(status)) out.add(status);
        return out;
    }

    /** The status groups asked for, however they were spelled. */
    public List<String> allStatusGroups() {
        List<String> out = new ArrayList<>();
        if (statusGroups != null) {
            statusGroups.stream().filter(g -> g != null && !g.isBlank()).forEach(out::add);
        }
        if (statusGroup != null && !statusGroup.isBlank() && !out.contains(statusGroup)) {
            out.add(statusGroup);
        }
        return out;
    }

    public boolean wants(String quality) {
        return qualities != null && qualities.contains(quality);
    }
}
