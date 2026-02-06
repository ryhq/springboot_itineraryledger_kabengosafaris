package com.itineraryledger.kabengosafaris.Invoice.Controller;

import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceStateTransitionDTO;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceStateTransitionService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * InvoiceStateTransitionController - REST API endpoints for Invoice state transitions
 *
 * Implements the simplified 8-state Invoice workflow:
 *
 * Core Journey (6 states):
 *   DRAFT → SENT → VIEWED → PARTIALLY_PAID → PAID
 *                                ↓
 *                            OVERDUE
 *
 * Exception States (2 states):
 *   CANCELLED, REFUNDED
 *
 * Each endpoint is protected by a specific permission that controls who can
 * perform that state transition. This enables fine-grained access control
 * over the invoice lifecycle.
 *
 * Base URL: /api/invoices/{id}/state
 */
@RestController
@RequestMapping("/api/invoices/{id}/state")
@Slf4j
@RequiredArgsConstructor
public class InvoiceStateTransitionController {

    private final InvoiceStateTransitionService stateTransitionService;

    // ========================
    // CORE JOURNEY - SENDING PHASE
    // ========================

    /**
     * Send invoice to customer (DRAFT → SENT)
     *
     * POST /api/invoices/{id}/state/send
     */
    @PostMapping("/send")
    @PreAuthorize("hasAuthority('PERM_SEND_INVOICE')")
    public ResponseEntity<ApiResponse<?>> sendInvoice(
            @PathVariable String id,
            @RequestBody(required = false) InvoiceStateTransitionDTO dto
    ) {
        log.info("POST /api/invoices/{}/state/send - Sending invoice", id);
        return stateTransitionService.sendInvoice(id, dto);
    }

    /**
     * Mark invoice as viewed by customer (SENT → VIEWED)
     *
     * POST /api/invoices/{id}/state/mark-viewed
     */
    @PostMapping("/mark-viewed")
    @PreAuthorize("hasAuthority('PERM_MARK_INVOICE_VIEWED')")
    public ResponseEntity<ApiResponse<?>> markAsViewed(
            @PathVariable String id,
            @RequestBody(required = false) InvoiceStateTransitionDTO dto
    ) {
        log.info("POST /api/invoices/{}/state/mark-viewed - Marking invoice as viewed", id);
        return stateTransitionService.markAsViewed(id, dto);
    }

    // ========================
    // CORE JOURNEY - PAYMENT PHASE
    // ========================

    /**
     * Record invoice payment (SENT/VIEWED/OVERDUE → PARTIALLY_PAID or PAID)
     *
     * POST /api/invoices/{id}/state/record-payment
     *
     * Request body:
     * - isFullPayment: (required) true for full payment, false for partial payment
     * - paymentReference: (optional) Payment reference or transaction ID
     * - notes: (optional) Additional payment notes
     */
    @PostMapping("/record-payment")
    @PreAuthorize("hasAuthority('PERM_RECORD_INVOICE_PAYMENT')")
    public ResponseEntity<ApiResponse<?>> recordPayment(
            @PathVariable String id,
            @Valid @RequestBody InvoiceStateTransitionDTO dto
    ) {
        log.info("POST /api/invoices/{}/state/record-payment - Recording payment", id);
        return stateTransitionService.recordPayment(id, dto);
    }

    /**
     * Mark invoice as overdue (SENT/VIEWED/PARTIALLY_PAID → OVERDUE)
     *
     * POST /api/invoices/{id}/state/mark-overdue
     */
    @PostMapping("/mark-overdue")
    @PreAuthorize("hasAuthority('PERM_MARK_INVOICE_OVERDUE')")
    public ResponseEntity<ApiResponse<?>> markOverdue(
            @PathVariable String id,
            @RequestBody(required = false) InvoiceStateTransitionDTO dto
    ) {
        log.info("POST /api/invoices/{}/state/mark-overdue - Marking invoice as overdue", id);
        return stateTransitionService.markOverdue(id, dto);
    }

    // ========================
    // EXCEPTION STATES - CANCELLATION
    // ========================

    /**
     * Cancel invoice (multiple states → CANCELLED)
     *
     * POST /api/invoices/{id}/state/cancel
     *
     * Request body:
     * - reason: (required) Detailed explanation for cancellation
     * - cancellationCategory: (optional) Category like "Payment failure", "Customer request", etc.
     * - notes: (optional) Additional context
     */
    @PostMapping("/cancel")
    @PreAuthorize("hasAuthority('PERM_CANCEL_INVOICE')")
    public ResponseEntity<ApiResponse<?>> cancelInvoice(
            @PathVariable String id,
            @Valid @RequestBody InvoiceStateTransitionDTO dto
    ) {
        log.info("POST /api/invoices/{}/state/cancel - Cancelling invoice", id);
        return stateTransitionService.cancelInvoice(id, dto);
    }

    // ========================
    // EXCEPTION STATES - REFUND
    // ========================

    /**
     * Initiate refund (PAID/PARTIALLY_PAID → REFUNDED)
     *
     * POST /api/invoices/{id}/state/initiate-refund
     *
     * Request body:
     * - reason: (required) Reason for refund
     * - isFullRefund: (optional) true for full refund, false for partial
     * - paymentReference: (optional) Refund reference or transaction ID
     * - notes: (optional) Additional refund notes
     */
    @PostMapping("/initiate-refund")
    @PreAuthorize("hasAuthority('PERM_INITIATE_INVOICE_REFUND')")
    public ResponseEntity<ApiResponse<?>> initiateRefund(
            @PathVariable String id,
            @Valid @RequestBody InvoiceStateTransitionDTO dto
    ) {
        log.info("POST /api/invoices/{}/state/initiate-refund - Initiating refund", id);
        return stateTransitionService.initiateRefund(id, dto);
    }
}
