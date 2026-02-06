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
    private BigDecimal discountPercentage;
    private String discountReason;

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
}
