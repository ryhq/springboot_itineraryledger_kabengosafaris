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

    private QuoteStatus status;

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
