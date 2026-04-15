package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogService;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.PaymentReceiptDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Payment;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfDocumentRepository;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfTemplateRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Translation.Services.TranslationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PaymentReceiptWordGenerationService - Generates payment receipt .docx files.
 *
 * Mirrors {@link PaymentReceiptPdfGenerationService} but emits .docx via {@link WordGenerator}.
 * Reuses the PaymentReceiptDTO already built by the PDF service to avoid duplicating
 * the (non-trivial) receipt aggregation logic.
 */
@Service
@Slf4j
public class PaymentReceiptWordGenerationService extends WordGenerationBaseService {

    private final PaymentReceiptPdfGenerationService paymentReceiptPdfGenerationService;

    public PaymentReceiptWordGenerationService(
            PdfDocumentRepository pdfDocumentRepository,
            PdfTemplateRepository pdfTemplateRepository,
            PdfTemplateRenderer renderer,
            WordGenerator wordGenerator,
            PdfTemplateValidationService validationService,
            IdObfuscator idObfuscator,
            AuditLogService auditLogService,
            TranslationService translationService,
            PaymentReceiptPdfGenerationService paymentReceiptPdfGenerationService
    ) {
        super(pdfDocumentRepository, pdfTemplateRepository, renderer, wordGenerator, validationService,
              idObfuscator, auditLogService, translationService);
        this.paymentReceiptPdfGenerationService = paymentReceiptPdfGenerationService;
    }

    /**
     * Generate payment receipt DOCX for a recorded payment (global default engine).
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> generatePaymentReceiptDocx(Payment payment, String language) {
        return generatePaymentReceiptDocx(payment, language, null, null);
    }

    /**
     * Generate payment receipt DOCX with an explicit template override.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> generatePaymentReceiptDocx(Payment payment, String language, String pdfTemplateId) {
        return generatePaymentReceiptDocx(payment, language, pdfTemplateId, null);
    }

    /**
     * Generate payment receipt DOCX with an explicit template override and engine override.
     *
     * @param engine Optional engine id ("docx4j" or "libreoffice"). Null = global default.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> generatePaymentReceiptDocx(Payment payment, String language, String pdfTemplateId, String engine) {
        try {
            // Reuse the PDF service's DTO builder so receipt data stays in one place
            PaymentReceiptDTO receiptData = paymentReceiptPdfGenerationService.buildReceiptDTO(payment);

            return generateDocxInternal(
                "PAYMENT_RECEIPT",
                receiptData,
                pdfTemplateId,
                language,
                engine,
                data -> extractReceiptCode((PaymentReceiptDTO) data)
            );

        } catch (Exception e) {
            log.error("Failed to generate payment receipt DOCX for payment ID: {}", payment.getId(), e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate payment receipt DOCX: " + e.getMessage(), "DOCX_GENERATION_FAILED")
            );
        }
    }

    private String extractReceiptCode(PaymentReceiptDTO receipt) {
        return receipt.getInvoiceCode() != null ? "RCPT_" + receipt.getInvoiceCode() : "receipt";
    }
}
