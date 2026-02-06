package com.itineraryledger.kabengosafaris.Invoice.DTOs;

import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Invoice state transition requests
 *
 * Used for 8-state workflow transitions with context-specific fields
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceStateTransitionDTO {

    /**
     * Reason for the state transition (required for some transitions)
     */
    @NotBlank(message = "Reason is required")
    private String reason;

    /**
     * Target state for explicit state transitions
     * Used in workflows where the next state is not predetermined
     */
    private InvoiceStatus targetState;

    /**
     * Additional notes for the transition
     */
    private String notes;

    // ========================
    // SIMPLIFIED WORKFLOW FIELDS
    // ========================

    /**
     * Indicates if payment being recorded is full payment (true) or partial (false)
     * Used in recordPayment to determine PARTIALLY_PAID vs PAID
     */
    private Boolean isFullPayment;

    /**
     * Indicates if refund being recorded is full refund (true) or partial (false)
     * Used in recordRefund to determine if invoice remains REFUNDED or not
     */
    private Boolean isFullRefund;

    /**
     * Payment reference number or transaction ID
     * Used when recording payments or refunds
     */
    private String paymentReference;

    /**
     * Cancellation reason category
     * Examples: "Payment failure", "Customer request", "Operational issue"
     */
    private String cancellationCategory;
}
