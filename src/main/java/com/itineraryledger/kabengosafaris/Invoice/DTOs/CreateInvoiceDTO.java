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
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInvoiceDTO {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String customerId; // Nullable

    private String safariId; // Nullable

    private BigDecimal taxPercentage;
    private BigDecimal discountPercentage;
    private String discountReason;

    @NotNull(message = "Issue date is required")
    private LocalDate issueDate;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private String internalNotes;
    private String customerNotes;
    private String paymentTerms;

    private Boolean isActive;
}
