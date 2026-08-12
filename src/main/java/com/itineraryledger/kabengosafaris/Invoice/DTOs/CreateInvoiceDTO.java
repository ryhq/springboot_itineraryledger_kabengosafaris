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
 * DTO for creating a new Invoice
 *
 * Safari ID is REQUIRED. Customer will be automatically derived from the Safari.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInvoiceDTO {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Safari ID is required")
    private String safariId;

    /**
     * A second demand against a safari that already has one.
     *
     * A trip changes after it has been invoiced and part-paid — a lodge is
     * swapped, a night is added — and the customer owes the difference. The
     * original invoice cannot answer for it: they hold a copy of it and have
     * paid against it, so it is read-only by design. The honest instrument is a
     * separate invoice carrying only the difference.
     *
     * <p>It is explicit rather than inferred so that the ordinary mistake — the
     * same trip invoiced twice by two people — is still refused. Saying
     * "supplement" is saying "I know there is already one".
     */
    private Boolean isSupplement;

    /** Required with isSupplement: what changed, and why the customer owes more. */
    private String supplementReason;

    private BigDecimal taxPercentage;
    private BigDecimal discountPercentage;
    private String discountReason;

    // Markup (bake into per-line-item unit price before discount/tax)
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
}
