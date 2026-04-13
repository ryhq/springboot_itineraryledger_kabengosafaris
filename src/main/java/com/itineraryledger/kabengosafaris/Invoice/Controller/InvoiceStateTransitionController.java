package com.itineraryledger.kabengosafaris.Invoice.Controller;

import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceStateTransitionDTO;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceStateTransitionService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * InvoiceStateTransitionController - REST API endpoints for Invoice state transitions
 *
 * Simplified 6-state workflow:
 *   DRAFT → SENT → PARTIALLY_PAID → PAID
 *                        ↕
 *                     OVERDUE
 *
 * Exception State:
 *   CANCELLED (from any non-PAID state)
 *
 * Base URL: /api/invoices/{id}/state
 */
@RestController
@RequestMapping("/api/invoices/{id}/state")
@Slf4j
@RequiredArgsConstructor
public class InvoiceStateTransitionController {

    private final InvoiceStateTransitionService stateTransitionService;
    private final IdObfuscator idObfuscator;

    /**
     * Send invoice to customer (DRAFT → SENT)
     * Transitions status, generates PDF, and sends email with attachment.
     *
     * POST /api/invoices/{id}/state/send
     *
     * @param id             Obfuscated invoice ID
     * @param language       Optional language code for translation (e.g., "fr", "sw")
     * @param emailTemplateId Optional obfuscated email template ID (must belong to SEND_INVOICE event)
     * @param pdfTemplateId  Optional obfuscated PDF template ID for FULL_INVOICE
     * @param attachPdf      Whether to attach the invoice PDF (default true)
     */
    @PostMapping("/send")
    @PreAuthorize("hasAuthority('PERM_SEND_INVOICE')")
    public ResponseEntity<ApiResponse<?>> sendInvoice(
            @PathVariable String id,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String emailTemplateId,
            @RequestParam(required = false) String pdfTemplateId,
            @RequestParam(defaultValue = "true") boolean attachPdf
    ) {
        log.info("POST /api/invoices/{}/state/send (language: {}, attachPdf: {})", id, language, attachPdf);

        Long decodedEmailTemplateId = null;
        if (emailTemplateId != null && !emailTemplateId.isBlank()) {
            decodedEmailTemplateId = idObfuscator.decodeId(emailTemplateId);
        }

        return stateTransitionService.sendInvoice(id, language, decodedEmailTemplateId, pdfTemplateId, attachPdf);
    }

    /**
     * Resend invoice email to customer (no status change)
     * Available for any non-DRAFT, non-CANCELLED invoice.
     *
     * POST /api/invoices/{id}/state/resend
     */
    @PostMapping("/resend")
    @PreAuthorize("hasAuthority('PERM_SEND_INVOICE')")
    public ResponseEntity<ApiResponse<?>> resendInvoice(
            @PathVariable String id,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String emailTemplateId,
            @RequestParam(required = false) String pdfTemplateId,
            @RequestParam(defaultValue = "true") boolean attachPdf
    ) {
        log.info("POST /api/invoices/{}/state/resend (language: {}, attachPdf: {})", id, language, attachPdf);

        Long decodedEmailTemplateId = null;
        if (emailTemplateId != null && !emailTemplateId.isBlank()) {
            decodedEmailTemplateId = idObfuscator.decodeId(emailTemplateId);
        }

        return stateTransitionService.resendInvoice(id, language, decodedEmailTemplateId, pdfTemplateId, attachPdf);
    }

    /**
     * Record invoice payment (SENT/PARTIALLY_PAID/OVERDUE → PARTIALLY_PAID or PAID)
     * POST /api/invoices/{id}/state/record-payment
     */
    @PostMapping("/record-payment")
    @PreAuthorize("hasAuthority('PERM_RECORD_INVOICE_PAYMENT')")
    public ResponseEntity<ApiResponse<?>> recordPayment(
            @PathVariable String id,
            @Valid @RequestBody InvoiceStateTransitionDTO dto
    ) {
        log.info("POST /api/invoices/{}/state/record-payment", id);
        return stateTransitionService.recordPayment(id, dto);
    }

    /**
     * Mark invoice as overdue (SENT/PARTIALLY_PAID → OVERDUE)
     * POST /api/invoices/{id}/state/mark-overdue
     */
    @PostMapping("/mark-overdue")
    @PreAuthorize("hasAuthority('PERM_MARK_INVOICE_OVERDUE')")
    public ResponseEntity<ApiResponse<?>> markOverdue(
            @PathVariable String id,
            @RequestBody(required = false) InvoiceStateTransitionDTO dto
    ) {
        log.info("POST /api/invoices/{}/state/mark-overdue", id);
        return stateTransitionService.markOverdue(id, dto);
    }

    /**
     * Cancel invoice (any non-PAID/non-CANCELLED → CANCELLED)
     * POST /api/invoices/{id}/state/cancel
     */
    @PostMapping("/cancel")
    @PreAuthorize("hasAuthority('PERM_CANCEL_INVOICE')")
    public ResponseEntity<ApiResponse<?>> cancelInvoice(
            @PathVariable String id,
            @Valid @RequestBody InvoiceStateTransitionDTO dto
    ) {
        log.info("POST /api/invoices/{}/state/cancel", id);
        return stateTransitionService.cancelInvoice(id, dto);
    }
}
