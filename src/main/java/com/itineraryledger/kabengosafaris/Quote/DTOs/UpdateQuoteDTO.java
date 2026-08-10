package com.itineraryledger.kabengosafaris.Quote.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating an existing Quote.
 * Note: quoteCode, sentDate, approvedAt, and version are system-managed and cannot be updated.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateQuoteDTO {

    private String title;
    private String description;

    /*
     * Who the quote is addressed to, and who has to approve it.
     *
     * The itinerary is deliberately NOT here: it is the basis of every line
     * item, so re-pointing it would leave prices belonging to another trip.
     * That change is a new quote, generated from the itinerary you want.
     */
    private String customerId;
    private String approverId;

    /*
     * Enums arrive as Strings so a blank can CLEAR the field; null still means
     * "leave unchanged". Bound as the enum itself, an empty value makes Jackson
     * reject the whole request body, which is how a set value became impossible
     * to unset (see the charging-basis fix).
     */
    private String status;

    private LocalDate safariStartDate;
    private Boolean isStoRate;
    private BigDecimal taxPercentage;
    private BigDecimal discountPercentage;
    private String discountReason;

    private BigDecimal agentCommissionPercentage;
    private String agentCommissionReason;
    private BigDecimal marginUpliftPercentage;
    private String marginUpliftReason;

    private Boolean condenseItems;

    private LocalDate validFrom;
    private LocalDate validTo;
    private Boolean isValid;

    private BigDecimal depositPercentage;
    private LocalDate depositDueDate;
    private LocalDate fullPaymentDueDate;

    private String internalNotes;
    private String customerNotes;

    private String approvalNotes;

    private String versionNotes;

    private Boolean isActive;
}
