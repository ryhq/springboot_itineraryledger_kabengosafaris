package com.itineraryledger.kabengosafaris.Invoice.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating an existing Invoice
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateInvoiceDTO {

    private String title;
    private String description;

    private BigDecimal taxPercentage;
    private BigDecimal discountPercentage;
    private String discountReason;

    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate sentDate;
    private LocalDate paidDate;

    // Note: status should generally be updated via workflow transitions
    // at /api/invoices/{id}/state/* endpoints, not via this update endpoint
    private InvoiceStatus status;

    private String internalNotes;
    private String customerNotes;
    private String paymentTerms;

    private Boolean isActive;
}
