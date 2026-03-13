package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogService;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.FullInvoiceDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceDocumentDTOs.InvoiceDocumentDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceDocument;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceDocumentServices.InvoiceDocumentCreateService;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices.InvoiceFullGetService;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfDocumentRepository;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfTemplateRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Translation.Services.TranslationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * InvoicePdfGenerationService - Handles PDF generation for invoices
 *
 * Responsibilities:
 * - Generate invoice PDFs with optional translation
 * - Generate and save invoice PDFs to InvoiceDocuments
 * - Preview invoice PDFs (HTML only)
 */
@Service
@Slf4j
public class InvoicePdfGenerationService extends PdfGenerationBaseService {

    private final InvoiceFullGetService invoiceFullGetService;
    private final InvoiceDocumentCreateService invoiceDocumentCreateService;

    public InvoicePdfGenerationService(
            PdfDocumentRepository pdfDocumentRepository,
            PdfTemplateRepository pdfTemplateRepository,
            PdfTemplateRenderer renderer,
            PdfGenerator generator,
            PdfTemplateValidationService validationService,
            IdObfuscator idObfuscator,
            AuditLogService auditLogService,
            TranslationService translationService,
            InvoiceFullGetService invoiceFullGetService,
            InvoiceDocumentCreateService invoiceDocumentCreateService
    ) {
        super(pdfDocumentRepository, pdfTemplateRepository, renderer, generator, validationService,
              idObfuscator, auditLogService, translationService);
        this.invoiceFullGetService = invoiceFullGetService;
        this.invoiceDocumentCreateService = invoiceDocumentCreateService;
    }

    /**
     * Generate PDF for invoice with optional translation
     *
     * @param invoiceIdObfuscated The obfuscated invoice ID
     * @param templateIdObfuscated Optional template ID
     * @param language Optional target language code for translation
     * @return ResponseEntity with PDF bytes or error
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> generateInvoicePdf(String invoiceIdObfuscated, String templateIdObfuscated, String language) {
        try {
            // Fetch invoice data
            FullInvoiceDTO invoiceData = fetchInvoiceData(invoiceIdObfuscated);
            if (invoiceData == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Invoice not found: " + invoiceIdObfuscated, "INVOICE_NOT_FOUND")
                );
            }

            // Generate PDF using base service
            return generatePdfInternal(
                "FULL_INVOICE",
                invoiceData,
                templateIdObfuscated,
                language,
                data -> extractInvoiceCode((FullInvoiceDTO) data)
            );

        } catch (Exception e) {
            log.error("Failed to generate invoice PDF: {}", invoiceIdObfuscated, e);
            logPdfError("GENERATE_PDF", "FULL_INVOICE", invoiceIdObfuscated, templateIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate PDF: " + e.getMessage(), "PDF_GENERATION_FAILED")
            );
        }
    }

    /**
     * Generate and save invoice PDF to InvoiceDocuments
     *
     * @param invoiceIdObfuscated The obfuscated invoice ID
     * @param templateIdObfuscated Optional template ID
     * @param language Optional target language code for translation
     * @param invoiceDocumentType The type of document to save as (e.g., INVOICE_PDF, TAX_INVOICE)
     * @param title Optional custom title (auto-generated if null)
     * @param version Optional version string
     * @param notes Optional notes
     * @return ResponseEntity with PDF bytes (document is also saved) or error
     */
    @Transactional
    public ResponseEntity<?> generateAndSaveInvoicePdf(
            String invoiceIdObfuscated,
            String templateIdObfuscated,
            String language,
            InvoiceDocument.DocumentType invoiceDocumentType,
            String title,
            String version,
            String notes
    ) {
        try {
            // 1. Fetch invoice data first
            FullInvoiceDTO invoiceData = fetchInvoiceData(invoiceIdObfuscated);
            if (invoiceData == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Invoice not found: " + invoiceIdObfuscated, "INVOICE_NOT_FOUND")
                );
            }

            // 2. Decode invoice ID for saving
            Long invoiceId;
            try {
                invoiceId = idObfuscator.decodeId(invoiceIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid invoice ID", "INVALID_INVOICE_ID")
                );
            }

            // 3. Generate the PDF
            ResponseEntity<?> pdfResponse = generateInvoicePdf(invoiceIdObfuscated, templateIdObfuscated, language);

            // If PDF generation failed, return the error response
            if (!pdfResponse.getStatusCode().is2xxSuccessful()) {
                return pdfResponse;
            }

            // 4. Extract PDF bytes from response
            byte[] pdfBytes = (byte[]) pdfResponse.getBody();
            if (pdfBytes == null || pdfBytes.length == 0) {
                return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Generated PDF is empty", "EMPTY_PDF")
                );
            }

            // 5. Generate filename and title
            String invoiceCode = invoiceData.getInvoiceCode() != null ? invoiceData.getInvoiceCode() : "INV";
            String invoiceTitle = invoiceData.getTitle() != null ? invoiceData.getTitle() : "Invoice";

            InvoiceDocument.DocumentType docType = invoiceDocumentType != null
                ? invoiceDocumentType
                : InvoiceDocument.DocumentType.INVOICE_PDF;

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String languageSuffix = (language != null && !language.isBlank() && !"en".equalsIgnoreCase(language))
                ? "_" + language.toLowerCase()
                : "";
            String fileName = String.format("%s_%s%s_%s.pdf",
                docType.name().toLowerCase(), invoiceCode, languageSuffix, timestamp);

            String documentTitle = title != null && !title.isBlank()
                ? title
                : String.format("%s - %s", docType.getDisplayName(), invoiceTitle);

            // 6. Save the document
            InvoiceDocumentDTO savedDocument = invoiceDocumentCreateService.saveGeneratedDocument(
                invoiceId,
                pdfBytes,
                fileName,
                docType,
                documentTitle,
                null, // description
                version,
                notes
            );

            if (savedDocument == null) {
                log.warn("PDF generated but failed to save to documents for invoice: {}", invoiceIdObfuscated);
                return pdfResponse;
            }

            log.info("PDF generated and saved for invoice: {}, document ID: {}", invoiceIdObfuscated, savedDocument.getId());

            // 7. Return the PDF response with additional headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(pdfBytes.length);
            headers.set("X-Document-Saved", "true");
            headers.set("X-Document-Id", savedDocument.getId());
            headers.set("X-Document-Url", savedDocument.getDocumentUrl());

            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);

        } catch (Exception e) {
            log.error("Failed to generate and save PDF for invoice: {}", invoiceIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate and save PDF: " + e.getMessage(), "PDF_SAVE_FAILED")
            );
        }
    }

    /**
     * Preview invoice PDF (return rendered HTML instead of PDF)
     *
     * @param invoiceIdObfuscated The obfuscated invoice ID
     * @param templateIdObfuscated Optional template ID
     * @param language Optional target language code for translation
     * @return ResponseEntity with rendered HTML or error
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> previewInvoicePdf(String invoiceIdObfuscated, String templateIdObfuscated, String language) {
        try {
            FullInvoiceDTO invoiceData = fetchInvoiceData(invoiceIdObfuscated);
            if (invoiceData == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Invoice not found", "INVOICE_NOT_FOUND")
                );
            }

            return previewPdfInternal("FULL_INVOICE", invoiceData, templateIdObfuscated, language);

        } catch (Exception e) {
            log.error("Failed to preview invoice PDF: {}", invoiceIdObfuscated, e);
            logPdfError("PREVIEW_PDF", "FULL_INVOICE", invoiceIdObfuscated, templateIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate preview: " + e.getMessage(), "PREVIEW_FAILED")
            );
        }
    }

    /**
     * Fetch full invoice data
     */
    private FullInvoiceDTO fetchInvoiceData(String invoiceIdObfuscated) {
        try {
            ResponseEntity<ApiResponse<?>> response = invoiceFullGetService.getFullInvoice(invoiceIdObfuscated);

            if (response.getStatusCode().is2xxSuccessful()) {
                ApiResponse<?> apiResponse = response.getBody();
                if (apiResponse != null) {
                    Object data = apiResponse.getData();
                    if (data instanceof FullInvoiceDTO) {
                        return (FullInvoiceDTO) data;
                    } else if (data instanceof Map) {
                        Object invoiceObj = ((Map<?, ?>) data).get("invoice");
                        if (invoiceObj instanceof FullInvoiceDTO) {
                            return (FullInvoiceDTO) invoiceObj;
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch invoice data: {}", invoiceIdObfuscated, e);
            return null;
        }
    }

    /**
     * Extract invoice code from data
     */
    private String extractInvoiceCode(FullInvoiceDTO invoice) {
        String code = invoice.getInvoiceCode();
        return (code != null && !code.isBlank()) ? code : "invoice";
    }
}
