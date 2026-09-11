package com.itineraryledger.kabengosafaris.Quote.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new Quote
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQuoteDTO {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Itinerary ID is required")
    private String itineraryId;

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    private LocalDate safariStartDate;
    private Boolean isStoRate;
    /** Vehicles this trip is priced for. Snapshotted, so a later itinerary edit cannot reprice it. */
    private Integer carCount;

    private BigDecimal taxPercentage;

    /** Which line categories the tax applies to ("ACCOMMODATION" / "ACCOMMODATION,ACTIVITY"). Null = all. */
    private String taxAppliesTo;

    private BigDecimal discountPercentage;
    private String discountReason;

    // Agent commission + margin uplift — applied multiplicatively to every
    // QuoteItem's unit price when items are derived from the Quote tree.
    // Customer never sees a separate line; the inflated price IS the
    // line-item price on the PDF.
    private BigDecimal agentCommissionPercentage;
    private String agentCommissionReason;
    private BigDecimal marginUpliftPercentage;
    private String marginUpliftReason;

    /** Which line categories the margin uplift applies to. Null = all. The commission is never scoped. */
    private String marginUpliftAppliesTo;


    /**
     * When true, derived QuoteItem rows are rolled up by category (one row
     * per type per currency). When false (default), the per-line breakdown
     * from cost estimation is preserved.
     */
    private Boolean condenseItems;

    @NotNull(message = "Valid from date is required")
    private LocalDate validFrom;

    @NotNull(message = "Valid to date is required")
    private LocalDate validTo;

    private BigDecimal depositPercentage;
    private LocalDate depositDueDate;
    private LocalDate fullPaymentDueDate;

    private String internalNotes;
    private String customerNotes;

    private String approverId;

    private Boolean isActive;
}
