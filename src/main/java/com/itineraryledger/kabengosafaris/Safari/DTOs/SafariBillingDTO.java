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
}
