package com.itineraryledger.kabengosafaris.Invoice.Services;

import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceStateTransitionDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices.InvoiceCreateService;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices.InvoicePaymentAggregationService;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Invoice State Transition Service - Manages invoice workflow state transitions
 *
 * Simplified 6-state workflow:
 *   DRAFT → SENT → PARTIALLY_PAID → PAID
 *                        ↕
 *                     OVERDUE
 *
 * Correction State:
 *   ON_HOLD (from SENT / PARTIALLY_PAID / OVERDUE / PAID, released back by what has been paid)
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
    private final InvoicePaymentAggregationService paymentAggregationService;

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
    // CORRECTION - HOLD AND RELEASE
    // ========================

    /**
     * Unlock an invoice for correction (SENT/PARTIALLY_PAID/OVERDUE/PAID → ON_HOLD)
     *
     * Nothing about the money moves. The payments stay exactly where they are and the status the
     * invoice came from is remembered, so the hold is reversible and visible rather than a quiet
     * reopening of a document the customer is already holding.
     */
    public ResponseEntity<ApiResponse<?>> holdInvoice(String idObfuscated, InvoiceStateTransitionDTO dto) {
        log.info("Holding invoice for correction: {}", idObfuscated);

        try {
            if (dto == null || dto.getReason() == null || dto.getReason().isBlank()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "A reason is required to unlock an invoice the customer already has",
                        "REASON_REQUIRED")
                );
            }

            Invoice invoice = findInvoice(idObfuscated);
            if (invoice == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(404, "Invoice not found", "INVOICE_NOT_FOUND")
                );
            }

            InvoiceStatus currentStatus = invoice.getStatus();
            if (currentStatus == InvoiceStatus.ON_HOLD) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invoice is already on hold", "ALREADY_ON_HOLD")
                );
            }

            if (!currentStatus.canBeHeld()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("Cannot hold an invoice in state %s. A %s invoice is edited directly.",
                            currentStatus.getDisplayName(), currentStatus.getDisplayName()),
                        "INVALID_STATE_TRANSITION")
                );
            }

            invoice.setStatusBeforeHold(currentStatus);
            invoice.setHoldReason(dto.getReason().trim());
            invoice.setStatus(InvoiceStatus.ON_HOLD);

            invoice = invoiceRepository.save(invoice);
            InvoiceDTO invoiceDTO = invoiceCreateService.convertToDTO(invoice);

            log.info("Invoice {} held for correction (was {}): {}",
                invoice.getInvoiceCode(), currentStatus, dto.getReason());

            return ResponseEntity.ok().body(
                ApiResponse.success(200,
                    String.format("Invoice unlocked for correction. It was %s and payments are untouched.",
                        currentStatus.getDisplayName()),
                    invoiceDTO)
            );

        } catch (Exception e) {
            log.error("Error holding invoice", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to hold invoice", "HOLD_INVOICE_FAILED")
            );
        }
    }

    /**
     * Put a corrected invoice back into the workflow (ON_HOLD → SENT/PARTIALLY_PAID/PAID/OVERDUE)
     *
     * The status is recomputed from what has actually been paid against the corrected total, never
     * restored from what it was. A correction usually changes the total, and the status it had
     * before the correction was a statement about a figure that no longer exists.
     */
    public ResponseEntity<ApiResponse<?>> releaseInvoice(String idObfuscated, InvoiceStateTransitionDTO dto) {
        log.info("Releasing invoice from hold: {}", idObfuscated);

        try {
            Invoice invoice = findInvoice(idObfuscated);
            if (invoice == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(404, "Invoice not found", "INVOICE_NOT_FOUND")
                );
            }

            if (invoice.getStatus() != InvoiceStatus.ON_HOLD) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("Cannot release an invoice in state %s. It must be on hold.",
                            invoice.getStatus().getDisplayName()),
                        "INVALID_STATE_TRANSITION")
                );
            }

            InvoiceStatus before = invoice.getStatusBeforeHold();
            InvoiceStatus released = statusFromPayments(invoice, before);

            /*
             * A correction that raises the total above what has been paid makes an invoice unpaid
             * again, so the paid date has to go with it or the document contradicts its own status.
             */
            if (released == InvoiceStatus.PAID) {
                if (invoice.getPaidDate() == null) {
                    invoice.setPaidDate(LocalDate.now());
                }
            } else {
                invoice.setPaidDate(null);
            }

            invoice.setStatus(released);
            invoice.setStatusBeforeHold(null);
            invoice.setHoldReason(null);

            invoice = invoiceRepository.save(invoice);
            InvoiceDTO invoiceDTO = invoiceCreateService.convertToDTO(invoice);

            String message = String.format("Invoice released as %s", released.getDisplayName());
            BigDecimal overpaid = overpayment(invoice);
            if (overpaid != null) {
                /* say it rather than silently marking it PAID: somebody is owed this back */
                message += String.format(". The correction leaves %s paid over the new total",
                    overpaid.toPlainString());
            }

            log.info("Invoice {} released from hold as {} (was {} before the hold)",
                invoice.getInvoiceCode(), released, before);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, message, invoiceDTO)
            );

        } catch (Exception e) {
            log.error("Error releasing invoice", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to release invoice", "RELEASE_INVOICE_FAILED")
            );
        }
    }

    /**
     * What the payments say this invoice now is.
     *
     * {@code before} only decides the untouched case: an invoice nobody has paid goes back to
     * whichever of SENT or OVERDUE it was, because both mean the same thing about the money.
     */
    private InvoiceStatus statusFromPayments(Invoice invoice, InvoiceStatus before) {
        List<Price> paidAmounts = paymentAggregationService.computeAmountsPaid(invoice);
        List<Price> balances = paymentAggregationService.computeBalances(invoice);

        boolean anyPaid = paidAmounts.stream()
            .anyMatch(p -> p.getTotalPrice() != null && p.getTotalPrice().compareTo(BigDecimal.ZERO) > 0);
        boolean settled = !balances.isEmpty() && balances.stream()
            .allMatch(b -> b.getTotalPrice() != null && b.getTotalPrice().compareTo(BigDecimal.ZERO) <= 0);

        if (settled) {
            return InvoiceStatus.PAID;
        }
        if (anyPaid) {
            return InvoiceStatus.PARTIALLY_PAID;
        }
        return before == InvoiceStatus.OVERDUE ? InvoiceStatus.OVERDUE : InvoiceStatus.SENT;
    }

    /** How much has been paid beyond the corrected total, or null when nothing has. */
    private BigDecimal overpayment(Invoice invoice) {
        BigDecimal worst = null;
        for (Price balance : paymentAggregationService.computeBalances(invoice)) {
            BigDecimal amount = balance.getTotalPrice();
            if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
                BigDecimal over = amount.negate();
                if (worst == null || over.compareTo(worst) > 0) {
                    worst = over;
                }
            }
        }
        return worst;
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
