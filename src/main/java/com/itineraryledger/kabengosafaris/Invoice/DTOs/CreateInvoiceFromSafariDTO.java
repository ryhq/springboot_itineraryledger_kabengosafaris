package com.itineraryledger.kabengosafaris.Invoice.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating an Invoice from an existing Safari with cost estimation
 *
 * Both Safari and Customer must be explicitly provided to ensure proper linking
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInvoiceFromSafariDTO {

    @NotBlank(message = "Safari ID is required")
    private String safariId;

    // Optional: if not provided, defaults will be used
    private String title;
    private String description;

    // Cost estimation parameters
    private Boolean useStoRate; // Default: false (RACK rates), true for STO rates
    private String currency; // Default: USD

    // Invoice pricing
    private BigDecimal taxPercentage;
    private BigDecimal discountPercentage;
    private String discountReason;

    // Markup (commission + uplift bake into per-line-item unit prices before
    // discount/tax). Customer never sees a separate markup line.
    private BigDecimal agentCommissionPercentage;
    private String agentCommissionReason;
    private BigDecimal marginUpliftPercentage;
    private String marginUpliftReason;

    @NotNull(message = "Issue date is required")
    private LocalDate issueDate;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private String internalNotes;
    private String customerNotes;
    private String paymentTerms;

    private Boolean isActive;

    /**
     * When true, line items are collapsed by InvoiceItemType so the resulting
     * invoice has one billable line per type (Accommodation, Park Fees,
     * Activities) instead of one per day per resource. The merged row's
     * description preserves the underlying breakdown for the PDF. Default false.
     */
    private Boolean condense;
}
