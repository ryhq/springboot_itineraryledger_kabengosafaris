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

    // Markup (changing these re-derives all line items)
    private BigDecimal agentCommissionPercentage;
    private String agentCommissionReason;
    private BigDecimal marginUpliftPercentage;
    private String marginUpliftReason;

    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate sentDate;
    private LocalDate paidDate;

    // Note: status should generally be updated via workflow transitions
    // at /api/invoices/{id}/state/* endpoints, not via this update endpoint
    /*
     * Enums arrive as Strings so a blank can CLEAR the field; null still means
     * "leave unchanged". Bound as the enum itself, an empty value makes Jackson
     * reject the whole request body, which is how a set value became impossible
     * to unset (see the charging-basis fix).
     */
    private String status;

    private String internalNotes;
    private String customerNotes;
    private String paymentTerms;

    private Boolean isActive;
}
