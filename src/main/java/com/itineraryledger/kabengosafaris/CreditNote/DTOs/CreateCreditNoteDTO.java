package com.itineraryledger.kabengosafaris.CreditNote.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new CreditNote
 *
 * Invoice ID is REQUIRED. Customer will be automatically derived from the Invoice.
 * Title defaults to "Credit Note - {invoiceCode}" if not provided.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCreditNoteDTO {

    @NotBlank(message = "Invoice ID is required")
    private String invoiceId;

    private String title;
    private String description;
    private String reason;

    private BigDecimal taxPercentage;

    @NotNull(message = "Issue date is required")
    private LocalDate issueDate;

    private String internalNotes;
    private String customerNotes;

    private Boolean isActive;
}
