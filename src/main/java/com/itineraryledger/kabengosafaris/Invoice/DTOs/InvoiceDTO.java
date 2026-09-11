package com.itineraryledger.kabengosafaris.Invoice.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Invoice responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDTO {

    private String id;
    private String invoiceCode;
    private String title;
    private String description;
    /** true when this is a second invoice for a change made after the first */
    private Boolean isSupplement;
    /** what changed, on a supplement */
    private String supplementReason;

    // Relationships
    private String customerId;
    private String customerName;
    private String customerEmail;

    private String safariId;
    private String safariCode;
    private String safariName;

    // Multi-currency totals
    private List<Price> subtotals;
    private List<Price> taxes;
    private List<Price> discounts;
    private List<Price> grandTotals;
    private List<Price> amountsPaid;
    private List<Price> balances;

    // Pricing details
    private BigDecimal taxPercentage;

    /** Which line categories the tax applies to. Null = all of them. */
    private String taxAppliesTo;
    private BigDecimal discountPercentage;
    private String discountReason;

    /** Which line categories the discount comes off ("ACCOMMODATION,ACTIVITY"). Null = all. */
    private String discountAppliesTo;

    // Markup (bakes into per-line-item unit prices; never a separate line)
    private BigDecimal agentCommissionPercentage;
    private String agentCommissionReason;
    private BigDecimal marginUpliftPercentage;
    private String marginUpliftReason;

    // Dates
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate sentDate;
    private LocalDate paidDate;

    // Status - using single InvoiceStatus enum for both workflow and payment tracking
    private InvoiceStatus status;
    private String statusDisplayName;

    // Additional
    private String internalNotes;
    private String customerNotes;
    private String paymentTerms;
    private Boolean isActive;
    private Boolean isOverdue;

    // Counts
    private Long lineItemCount;

    // Audit
    private String createdById;
    private String createdByName;
    private String updatedById;
    private String updatedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * The tax scope in words, for the document the client reads.
     *
     * <p>A scoped tax does not divide into the subtotal: 18% on the beds alone is 9.7% of the
     * trip, and a client checking the arithmetic finds a percentage that is not there. The enum
     * names are for us; this is the sentence for them.
     */
    public String getTaxAppliesToLabel() {
        return com.itineraryledger.kabengosafaris.GlobalEnums.LineCategoryScope.describe(
            taxAppliesTo, com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceItemType.class);
    }

    /**
     * The discount scope in words, for the document the client reads.
     *
     * <p>Same problem as the tax and worse, because a discount is a promise: 10% off the parts we
     * sell is 7.92% of an invoice carrying park fees, and a client who works out the percentage and
     * finds 7.92 has been given no way to see that the 10% was kept. This says what it came off.
     */
    public String getDiscountAppliesToLabel() {
        return com.itineraryledger.kabengosafaris.GlobalEnums.LineCategoryScope.describe(
            discountAppliesTo, com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceItemType.class);
    }
}
