package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogService;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.FullInvoiceShareDTO;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices.InvoiceShareFullGetService;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfDocumentRepository;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfTemplateRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Translation.Services.TranslationService;

import lombok.extern.slf4j.Slf4j;

/**
 * One payer's request for their share of a group invoice.
 *
 * <p>Registered in both dispatcher switches and on a route of its own from the start, because the
 * credit note taught us what a half-wired document type looks like: the schema, the template and
 * the seeded row all existed and were all unreachable, and the only symptom was a 404 nobody saw
 * until a customer asked for the document.
 */
@Service
@Slf4j
public class InvoiceSharePdfGenerationService extends PdfGenerationBaseService {

    private final InvoiceShareFullGetService shareFullGetService;

    public InvoiceSharePdfGenerationService(
            PdfDocumentRepository pdfDocumentRepository,
            PdfTemplateRepository pdfTemplateRepository,
            PdfTemplateRenderer renderer,
            PdfGenerator generator,
            PdfTemplateValidationService validationService,
            IdObfuscator idObfuscator,
            AuditLogService auditLogService,
            TranslationService translationService,
            InvoiceShareFullGetService shareFullGetService
    ) {
        super(pdfDocumentRepository, pdfTemplateRepository, renderer, generator, validationService,
              idObfuscator, auditLogService, translationService);
        this.shareFullGetService = shareFullGetService;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> generateInvoiceSharePdf(
            String shareIdObfuscated, String templateIdObfuscated, String language) {
        try {
            FullInvoiceShareDTO data = shareFullGetService.fetch(shareIdObfuscated);
            if (data == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Invoice share not found: " + shareIdObfuscated,
                        "INVOICE_SHARE_NOT_FOUND")
                );
            }

            return generatePdfInternal(
                "INVOICE_PAYMENT_REQUEST",
                data,
                templateIdObfuscated,
                language,
                d -> ((FullInvoiceShareDTO) d).getInvoiceCode()
            );

        } catch (Exception e) {
            log.error("Failed to generate payment request PDF: {}", shareIdObfuscated, e);
            logPdfError("GENERATE_PDF", "INVOICE_PAYMENT_REQUEST", shareIdObfuscated,
                templateIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate PDF: " + e.getMessage(),
                    "PDF_GENERATION_FAILED")
            );
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> previewInvoiceSharePdf(
            String shareIdObfuscated, String templateIdObfuscated, String language) {
        try {
            FullInvoiceShareDTO data = shareFullGetService.fetch(shareIdObfuscated);
            if (data == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Invoice share not found: " + shareIdObfuscated,
                        "INVOICE_SHARE_NOT_FOUND")
                );
            }

            return previewPdfInternal("INVOICE_PAYMENT_REQUEST", data, templateIdObfuscated, language);

        } catch (Exception e) {
            log.error("Failed to preview payment request PDF: {}", shareIdObfuscated, e);
            logPdfError("PREVIEW_PDF", "INVOICE_PAYMENT_REQUEST", shareIdObfuscated,
                templateIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to preview PDF: " + e.getMessage(),
                    "PDF_PREVIEW_FAILED")
            );
        }
    }
}
