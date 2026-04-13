package com.itineraryledger.kabengosafaris.Invoice.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.Invoice.DTOs.CreatePaymentDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.UpdatePaymentDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Payment;
import com.itineraryledger.kabengosafaris.Invoice.Repository.PaymentRepository;
import com.itineraryledger.kabengosafaris.Invoice.Services.PaymentServices.PaymentCreateService;
import com.itineraryledger.kabengosafaris.Invoice.Services.PaymentServices.PaymentCustomerEmailService;
import com.itineraryledger.kabengosafaris.Invoice.Services.PaymentServices.PaymentDeleteService;
import com.itineraryledger.kabengosafaris.Invoice.Services.PaymentServices.PaymentGetService;
import com.itineraryledger.kabengosafaris.Invoice.Services.PaymentServices.PaymentUpdateService;
import com.itineraryledger.kabengosafaris.PdfDocument.Services.PaymentReceiptPdfGenerationService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PaymentController - REST API endpoints for managing payments on invoices.
 *
 * Base URL: /api/invoices/{invoiceId}/payments
 *
 * The invoiceId is extracted from the path and injected into the CreatePaymentDTO
 * so the client does not need to duplicate it in the request body.
 */
@RestController
@RequestMapping("/api/invoices/{invoiceId}/payments")
@Slf4j
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentCreateService paymentCreateService;
    private final PaymentGetService paymentGetService;
    private final PaymentUpdateService paymentUpdateService;
    private final PaymentDeleteService paymentDeleteService;
    private final PaymentCustomerEmailService paymentCustomerEmailService;
    private final PaymentReceiptPdfGenerationService paymentReceiptPdfGenerationService;
    private final PaymentRepository paymentRepository;
    private final IdObfuscator idObfuscator;

    /**
     * Record a new payment against an invoice
     * POST /api/invoices/{invoiceId}/payments
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_RECORD_INVOICE_PAYMENT')")
    public ResponseEntity<ApiResponse<?>> createPayment(
        @PathVariable String invoiceId,
        @Valid @RequestBody CreatePaymentDTO createPaymentDTO
    ) {
        log.info("POST /api/invoices/{}/payments - Recording new payment", invoiceId);
        // Set invoiceId from path variable so the client doesn't need to duplicate it in the body
        createPaymentDTO.setInvoiceId(invoiceId);
        return paymentCreateService.createPayment(createPaymentDTO);
    }

    /**
     * Get all payments for an invoice
     * GET /api/invoices/{invoiceId}/payments
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_INVOICE')")
    public ResponseEntity<ApiResponse<?>> getPaymentsByInvoice(@PathVariable String invoiceId) {
        log.info("GET /api/invoices/{}/payments - Fetching payments", invoiceId);
        return paymentGetService.getPaymentsByInvoice(invoiceId);
    }

    /**
     * Get a single payment by ID
     * GET /api/invoices/{invoiceId}/payments/{paymentId}
     */
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAuthority('PERM_READ_INVOICE')")
    public ResponseEntity<ApiResponse<?>> getPaymentById(
        @PathVariable String invoiceId,
        @PathVariable String paymentId
    ) {
        log.info("GET /api/invoices/{}/payments/{} - Fetching payment by ID", invoiceId, paymentId);
        return paymentGetService.getPaymentById(paymentId);
    }

    /**
     * Update an existing payment
     * PUT /api/invoices/{invoiceId}/payments/{paymentId}
     */
    @PutMapping("/{paymentId}")
    @PreAuthorize("hasAuthority('PERM_RECORD_INVOICE_PAYMENT')")
    public ResponseEntity<ApiResponse<?>> updatePayment(
        @PathVariable String invoiceId,
        @PathVariable String paymentId,
        @Valid @RequestBody UpdatePaymentDTO updatePaymentDTO
    ) {
        log.info("PUT /api/invoices/{}/payments/{} - Updating payment", invoiceId, paymentId);
        return paymentUpdateService.updatePayment(paymentId, updatePaymentDTO);
    }

    /**
     * Resend payment receipt email to the customer
     * POST /api/invoices/{invoiceId}/payments/{paymentId}/resend-receipt
     *
     * @param invoiceId        Obfuscated invoice ID
     * @param paymentId        Obfuscated payment ID
     * @param language         Optional language code for translation (e.g., "fr", "sw")
     * @param emailTemplateId  Optional obfuscated email template ID (must belong to SEND_PAYMENT_RECEIPT event)
     * @param pdfTemplateId    Optional obfuscated PDF template ID (must belong to PAYMENT_RECEIPT document)
     * @param attachPdf        Whether to attach the payment receipt PDF (default true)
     */
    @PostMapping("/{paymentId}/resend-receipt")
    @PreAuthorize("hasAuthority('PERM_RECORD_INVOICE_PAYMENT')")
    public ResponseEntity<ApiResponse<?>> resendPaymentReceipt(
        @PathVariable String invoiceId,
        @PathVariable String paymentId,
        @RequestParam(required = false) String language,
        @RequestParam(required = false) String emailTemplateId,
        @RequestParam(required = false) String pdfTemplateId,
        @RequestParam(defaultValue = "true") boolean attachPdf
    ) {
        log.info("POST /api/invoices/{}/payments/{}/resend-receipt (language: {}, attachPdf: {})",
            invoiceId, paymentId, language, attachPdf);

        try {
            Long decodedPaymentId = idObfuscator.decodeId(paymentId);
            Payment payment = paymentRepository.findById(decodedPaymentId).orElse(null);

            if (payment == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Payment not found", "PAYMENT_NOT_FOUND")
                );
            }

            if (payment.getInvoice() == null || payment.getInvoice().getCustomer() == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No customer linked to this invoice", "NO_CUSTOMER")
                );
            }

            String customerEmail = payment.getInvoice().getCustomer().getPrimaryEmail();
            if (customerEmail == null || customerEmail.isBlank()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Customer has no email address", "NO_CUSTOMER_EMAIL")
                );
            }

            Long decodedEmailTemplateId = null;
            if (emailTemplateId != null && !emailTemplateId.isBlank()) {
                decodedEmailTemplateId = idObfuscator.decodeId(emailTemplateId);
            }

            paymentCustomerEmailService.sendPaymentReceiptEmail(payment, language, decodedEmailTemplateId, pdfTemplateId, attachPdf);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Payment receipt email resent to " + customerEmail)
            );

        } catch (Exception e) {
            log.error("Error resending payment receipt", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to resend payment receipt", "RESEND_RECEIPT_FAILED")
            );
        }
    }

    /**
     * Download the payment receipt PDF for a recorded payment.
     * GET /api/invoices/{invoiceId}/payments/{paymentId}/receipt-pdf
     *
     * Returns the PDF bytes with Content-Type application/pdf and a
     * Content-Disposition attachment header so browsers trigger a download.
     *
     * @param invoiceId      Obfuscated invoice ID
     * @param paymentId      Obfuscated payment ID
     * @param language       Optional language code for translation (e.g., "fr", "sw")
     * @param pdfTemplateId  Optional obfuscated PDF template ID (must belong to PAYMENT_RECEIPT document).
     *                       When omitted, the system default PAYMENT_RECEIPT template is used.
     */
    @GetMapping("/{paymentId}/receipt-pdf")
    @PreAuthorize("hasAuthority('PERM_READ_INVOICE')")
    public ResponseEntity<?> downloadPaymentReceiptPdf(
        @PathVariable String invoiceId,
        @PathVariable String paymentId,
        @RequestParam(required = false) String language,
        @RequestParam(required = false) String pdfTemplateId
    ) {
        log.info("GET /api/invoices/{}/payments/{}/receipt-pdf (language: {})", invoiceId, paymentId, language);

        try {
            Long decodedPaymentId = idObfuscator.decodeId(paymentId);
            Payment payment = paymentRepository.findById(decodedPaymentId).orElse(null);

            if (payment == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Payment not found", "PAYMENT_NOT_FOUND")
                );
            }

            return paymentReceiptPdfGenerationService.generatePaymentReceiptPdf(payment, language, pdfTemplateId);

        } catch (Exception e) {
            log.error("Error generating payment receipt PDF", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to generate payment receipt PDF", "PDF_GENERATION_FAILED")
            );
        }
    }

    /**
     * Delete a payment
     * DELETE /api/invoices/{invoiceId}/payments/{paymentId}
     *
     * @param force If true, allows deleting even if it would break the fully-paid
     *              status on a safari that has progressed past FULLY_PAID
     */
    @DeleteMapping("/{paymentId}")
    @PreAuthorize("hasAuthority('PERM_RECORD_INVOICE_PAYMENT')")
    public ResponseEntity<ApiResponse<?>> deletePayment(
        @PathVariable String invoiceId,
        @PathVariable String paymentId,
        @RequestParam(defaultValue = "false") boolean force
    ) {
        log.info("DELETE /api/invoices/{}/payments/{} - Deleting payment (force: {})", invoiceId, paymentId, force);
        return paymentDeleteService.deletePayment(paymentId, force);
    }
}
