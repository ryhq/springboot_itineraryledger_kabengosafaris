package com.itineraryledger.kabengosafaris.Invoice.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;
import com.itineraryledger.kabengosafaris.Invoice.Enums.PaymentStatus;

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

    private InvoiceStatus status;
    private PaymentStatus paymentStatus;

    private String internalNotes;
    private String customerNotes;
    private String paymentTerms;

    private Boolean isActive;
}
