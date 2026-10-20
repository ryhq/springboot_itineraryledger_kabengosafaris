package com.itineraryledger.kabengosafaris.Invoice.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Inclusion.DTOs.InclusionLineDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One payer's own copy of what they owe: the model the payment-request PDF is rendered from.
 *
 * <p>Deliberately NOT the invoice. A member of a group who is paying their own share should be
 * told their amount, their reference and the deadline, and should not be handed a document
 * itemising what the other five are each contributing. The group total is on it because the share
 * is meaningless without the context, and nothing below that line is.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FullInvoiceShareDTO {

    private String id;

    // ---- who is paying
    private String payerName;
    private String payerEmail;

    // ---- what they owe
    private BigDecimal amount;
    private String currency;
    private String formattedAmount;
    private BigDecimal amountPaid;
    private String formattedAmountPaid;
    private BigDecimal balance;
    private String formattedBalance;
    /** UNPAID · PARTIALLY_PAID · PAID */
    private String status;
    private String statusDisplayName;

    /** What to quote on the transfer so an unmatched arrival can be identified. */
    private String shareReference;
    private String notes;

    // ---- the booking it belongs to
    private String invoiceCode;
    private String invoiceTitle;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private String paymentTerms;
    /** The lead traveller the invoice itself is addressed to. */
    private String billedToName;
    private String safariName;

    /** The whole trip, so the payer can see their share in proportion. */
    private BigDecimal invoiceTotal;
    private String formattedInvoiceTotal;

    /** Every payer on this invoice, named with their amount, for a group that wants the picture. */
    private List<PayerLineDTO> payers;
    private Integer payerCount;

    /**
     * What the invoice says its price covers, snapshotted when the invoice was raised.
     *
     * <p>Carried on this document specifically because the person reading it is usually NOT the
     * person who booked. A member of a group is being asked for a four-figure sum against an
     * invoice they have never seen; if the request does not say what it buys, their only way to
     * find out is to go back and ask the organiser, which is the errand this whole feature
     * exists to remove.
     *
     * <p>Never read live from the catalogue: the client holds a copy, and a catalogue edit that
     * changed what our copy says would make ours the one that looked altered.
     */
    private List<InclusionLineDTO> inclusions;

    private LocalDateTime generatedAt;

    /**
     * True when this payer has settled, so the template can print a receipt-toned page instead of
     * a demand. Sending "please pay" to somebody who already has is the fastest way to be chased
     * by a client who owes you nothing.
     */
    public boolean isSettled() {
        return balance != null && balance.compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * What is included, as plain lines.
     *
     * <p>A getter rather than a stored field, the same shape as every other client document:
     * SpEL resolves getters, so a template can iterate a value nothing has to store or keep in
     * step.
     */
    public List<String> getInclusionsList() {
        return linesWhere(true);
    }

    /** What is not included. Stated on the document, never derived from the above. */
    public List<String> getExclusionsList() {
        return linesWhere(false);
    }

    private List<String> linesWhere(boolean included) {
        if (inclusions == null || inclusions.isEmpty()) return List.of();
        return inclusions.stream()
            .filter(line -> line != null && line.isIncluded() == included)
            .map(InclusionLineDTO::getLabel)
            .filter(label -> label != null && !label.isBlank())
            .map(String::trim)
            .toList();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PayerLineDTO {
        private String payerName;
        private BigDecimal amount;
        private String currency;
        private String formattedAmount;
        private String status;
        /** True for the payer this document is addressed to, so the template can mark their row. */
        private Boolean isThisPayer;
    }
}
