package com.itineraryledger.kabengosafaris.Safari.DTOs;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What a safari owes, read from its invoices.
 *
 * Every figure is derived. Nothing here is stored on the safari, so nothing here
 * can contradict the invoice it came from — which is the whole reason it exists.
 * Amounts are per currency and are never added across them.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SafariBillingDTO {

    /** Live invoices only; a cancelled one is kept for audit but owes nothing. */
    private Integer invoiceCount;

    private String latestInvoiceId;
    private String latestInvoiceCode;
    private String latestInvoiceStatus;
    private LocalDate dueDate;

    /** NOT_INVOICED · AWAITING_PAYMENT · PARTIALLY_PAID · OVERDUE · PAID */
    private String paymentStatus;
    private String paymentStatusDisplayName;

    private List<Price> invoiced;
    private List<Price> paid;
    private List<Price> balance;

    /**
     * Every live invoice against this safari, oldest first.
     *
     * A safari with two invoices is the ordinary case once a trip changes after
     * it was billed, and a page that showed only a count could not say which was
     * the original and which the supplement, or why the second exists.
     */
    private List<InvoiceLine> invoices;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InvoiceLine {
        private String id;
        private String invoiceCode;
        private String title;
        private String status;
        private String statusDisplayName;
        private LocalDate issueDate;
        private LocalDate dueDate;
        private Boolean isSupplement;
        /** why this second invoice exists — the answer to the customer's question */
        private String supplementReason;
        private List<Price> total;
        private List<Price> paid;
        private List<Price> balance;
    }
}
