package com.itineraryledger.kabengosafaris.Invoice.Services;

import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceStateTransitionDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices.InvoiceCreateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Invoice State Transition Service - Manages invoice workflow state transitions
 *
 * Simplified 6-state workflow:
 *   DRAFT → SENT → PARTIALLY_PAID → PAID
 *                        ↕
 *                     OVERDUE
 *
 * Exception State:
 *   CANCELLED (from any non-PAID state)
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class InvoiceStateTransitionService {

    private final InvoiceRepository invoiceRepository;
    private final IdObfuscator idObfuscator;
    private final InvoiceCreateService invoiceCreateService;
    private final InvoiceCustomerEmailService invoiceCustomerEmailService;

    // ========================
    // CORE JOURNEY - SENDING
    // ========================

    /**
     * Send invoice to customer (DRAFT → SENT)
     * Transitions status, then sends email with optional PDF attachment asynchronously.
     *
     * @param idObfuscated           Obfuscated invoice ID
     * @param language               Optional language code for translation
     * @param emailTemplateId        Optional email template ID (must belong to SEND_INVOICE event)
     * @param pdfTemplateIdObfuscated Optional PDF template ID for FULL_INVOICE
     * @param attachPdf              Whether to attach the invoice PDF
     */
    public ResponseEntity<ApiResponse<?>> sendInvoice(
            String idObfuscated, String language,
            Long emailTemplateId, String pdfTemplateIdObfuscated, boolean attachPdf) {
        log.info("Sending invoice: {} (language: {}, attachPdf: {})", idObfuscated, language, attachPdf);

        try {
            Invoice invoice = findInvoice(idObfuscated);
            if (invoice == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(404, "Invoice not found", "INVOICE_NOT_FOUND")
                );
            }

            if (invoice.getStatus() != InvoiceStatus.DRAFT) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("Cannot send invoice in state %s. Invoice must be in DRAFT state.",
                            invoice.getStatus().getDisplayName()),
                        "INVALID_STATE_TRANSITION")
                );
            }

            invoice.setStatus(InvoiceStatus.SENT);
            invoice.setSentDate(LocalDate.now());

            invoice = invoiceRepository.save(invoice);
            InvoiceDTO invoiceDTO = invoiceCreateService.convertToDTO(invoice);

            // Send email with PDF attachment asynchronously
            invoiceCustomerEmailService.sendInvoiceEmail(
                invoice, idObfuscated, language,
                emailTemplateId, pdfTemplateIdObfuscated, attachPdf
            );

            String customerEmail = invoice.getCustomer() != null ? invoice.getCustomer().getPrimaryEmail() : null;
            String message = customerEmail != null && !customerEmail.isBlank()
                ? "Invoice sent successfully. Email sent to " + customerEmail
                : "Invoice sent successfully";

            log.info("Invoice {} sent successfully", invoice.getInvoiceCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, message, invoiceDTO)
            );

        } catch (Exception e) {
            log.error("Error sending invoice", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to send invoice", "SEND_INVOICE_FAILED")
            );
        }
    }

    // ========================
    // RESEND EMAIL (no status change)
    // ========================

    /**
     * Resend invoice email to customer without changing status.
     * Allowed for any non-DRAFT, non-CANCELLED invoice (i.e., already sent at least once).
     */
    public ResponseEntity<ApiResponse<?>> resendInvoice(
            String idObfuscated, String language,
            Long emailTemplateId, String pdfTemplateIdObfuscated, boolean attachPdf) {
        log.info("Resending invoice email: {} (language: {}, attachPdf: {})", idObfuscated, language, attachPdf);

        try {
            Invoice invoice = findInvoice(idObfuscated);
            if (invoice == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(404, "Invoice not found", "INVOICE_NOT_FOUND")
                );
            }

            InvoiceStatus status = invoice.getStatus();
            if (status == InvoiceStatus.DRAFT || status == InvoiceStatus.CANCELLED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("Cannot resend invoice in state %s. Invoice must have been sent at least once.",
                            status.getDisplayName()),
                        "INVALID_STATE_FOR_RESEND")
                );
            }

            if (invoice.getCustomer() == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invoice has no customer linked", "NO_CUSTOMER")
                );
            }

            String customerEmail = invoice.getCustomer().getPrimaryEmail();
            if (customerEmail == null || customerEmail.isBlank()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Customer has no email address", "NO_CUSTOMER_EMAIL")
                );
            }

            // Send email asynchronously (no status change)
            invoiceCustomerEmailService.sendInvoiceEmail(
                invoice, idObfuscated, language,
                emailTemplateId, pdfTemplateIdObfuscated, attachPdf
            );

            log.info("Invoice {} email resent to {}", invoice.getInvoiceCode(), customerEmail);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Invoice email resent to " + customerEmail)
            );

        } catch (Exception e) {
            log.error("Error resending invoice email", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to resend invoice email", "RESEND_INVOICE_FAILED")
            );
        }
    }

    // ========================
    // CORE JOURNEY - PAYMENT
    // ========================

    /**
     * Record invoice payment (SENT/PARTIALLY_PAID/OVERDUE → PARTIALLY_PAID or PAID)
     */
    public ResponseEntity<ApiResponse<?>> recordPayment(String idObfuscated, InvoiceStateTransitionDTO dto) {
        log.info("Recording payment for invoice: {}", idObfuscated);

        try {
            if (dto == null || dto.getIsFullPayment() == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Payment details are required (isFullPayment field)", "PAYMENT_DETAILS_REQUIRED")
                );
            }

            Invoice invoice = findInvoice(idObfuscated);
            if (invoice == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(404, "Invoice not found", "INVOICE_NOT_FOUND")
                );
            }

            InvoiceStatus currentStatus = invoice.getStatus();
            if (currentStatus != InvoiceStatus.SENT &&
                currentStatus != InvoiceStatus.PARTIALLY_PAID &&
                currentStatus != InvoiceStatus.OVERDUE) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("Cannot record payment from state %s. Invoice must be SENT, PARTIALLY_PAID, or OVERDUE.",
                            currentStatus.getDisplayName()),
                        "INVALID_STATE_TRANSITION")
                );
            }

            if (dto.getIsFullPayment()) {
                invoice.setStatus(InvoiceStatus.PAID);
                invoice.setPaidDate(LocalDate.now());
                log.info("Invoice {} marked as PAID", invoice.getInvoiceCode());
            } else {
                invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
                log.info("Invoice {} marked as PARTIALLY_PAID", invoice.getInvoiceCode());
            }

            invoice = invoiceRepository.save(invoice);
            InvoiceDTO invoiceDTO = invoiceCreateService.convertToDTO(invoice);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Payment recorded successfully", invoiceDTO)
            );

        } catch (Exception e) {
            log.error("Error recording payment", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to record payment", "RECORD_PAYMENT_FAILED")
            );
        }
    }

    /**
     * Mark invoice as overdue (SENT/PARTIALLY_PAID → OVERDUE)
     */
    public ResponseEntity<ApiResponse<?>> markOverdue(String idObfuscated, InvoiceStateTransitionDTO dto) {
        log.info("Marking invoice as overdue: {}", idObfuscated);

        try {
            Invoice invoice = findInvoice(idObfuscated);
            if (invoice == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(404, "Invoice not found", "INVOICE_NOT_FOUND")
                );
            }

            InvoiceStatus currentStatus = invoice.getStatus();
            if (currentStatus != InvoiceStatus.SENT &&
                currentStatus != InvoiceStatus.PARTIALLY_PAID) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("Cannot mark as overdue from state %s. Invoice must be SENT or PARTIALLY_PAID.",
                            currentStatus.getDisplayName()),
                        "INVALID_STATE_TRANSITION")
                );
            }

            if (!invoice.getDueDate().isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "Cannot mark as overdue - due date has not passed yet",
                        "NOT_YET_OVERDUE")
                );
            }

            invoice.setStatus(InvoiceStatus.OVERDUE);

            invoice = invoiceRepository.save(invoice);
            InvoiceDTO invoiceDTO = invoiceCreateService.convertToDTO(invoice);

            log.info("Invoice {} marked as OVERDUE", invoice.getInvoiceCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Invoice marked as overdue", invoiceDTO)
            );

        } catch (Exception e) {
            log.error("Error marking invoice as overdue", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to mark invoice as overdue", "MARK_OVERDUE_FAILED")
            );
        }
    }

    // ========================
    // EXCEPTION STATE - CANCELLATION
    // ========================

    /**
     * Cancel invoice (any non-PAID/non-CANCELLED state → CANCELLED)
     */
    public ResponseEntity<ApiResponse<?>> cancelInvoice(String idObfuscated, InvoiceStateTransitionDTO dto) {
        log.info("Cancelling invoice: {}", idObfuscated);

        try {
            if (dto == null || dto.getReason() == null || dto.getReason().isBlank()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Cancellation reason is required", "REASON_REQUIRED")
                );
            }

            Invoice invoice = findInvoice(idObfuscated);
            if (invoice == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(404, "Invoice not found", "INVOICE_NOT_FOUND")
                );
            }

            InvoiceStatus currentStatus = invoice.getStatus();
            if (currentStatus == InvoiceStatus.PAID || currentStatus == InvoiceStatus.CANCELLED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("Cannot cancel invoice in state %s.",
                            currentStatus.getDisplayName()),
                        "INVALID_STATE_TRANSITION")
                );
            }

            invoice.setStatus(InvoiceStatus.CANCELLED);

            invoice = invoiceRepository.save(invoice);
            InvoiceDTO invoiceDTO = invoiceCreateService.convertToDTO(invoice);

            log.info("Invoice {} cancelled: {}", invoice.getInvoiceCode(), dto.getReason());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Invoice cancelled successfully", invoiceDTO)
            );

        } catch (Exception e) {
            log.error("Error cancelling invoice", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to cancel invoice", "CANCEL_INVOICE_FAILED")
            );
        }
    }

    // ========================
    // HELPER METHODS
    // ========================

    private Invoice findInvoice(String idObfuscated) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            return invoiceRepository.findById(id).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to decode invoice ID: {}", idObfuscated, e);
            return null;
        }
    }
}
