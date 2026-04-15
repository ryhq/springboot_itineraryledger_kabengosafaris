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
 * InvoiceWordGenerationService - Handles Word (.docx) generation for invoices.
 *
 * Mirrors {@link InvoicePdfGenerationService} but emits .docx via {@link WordGenerator}.
 * Uses the same FullInvoiceDTO data and the same pdf-templates (no new templates).
 */
@Service
@Slf4j
public class InvoiceWordGenerationService extends WordGenerationBaseService {

    private final InvoiceFullGetService invoiceFullGetService;
    private final InvoiceDocumentCreateService invoiceDocumentCreateService;

    public InvoiceWordGenerationService(
            PdfDocumentRepository pdfDocumentRepository,
            PdfTemplateRepository pdfTemplateRepository,
            PdfTemplateRenderer renderer,
            WordGenerator wordGenerator,
            PdfTemplateValidationService validationService,
            IdObfuscator idObfuscator,
            AuditLogService auditLogService,
            TranslationService translationService,
            InvoiceFullGetService invoiceFullGetService,
            InvoiceDocumentCreateService invoiceDocumentCreateService
    ) {
        super(pdfDocumentRepository, pdfTemplateRepository, renderer, wordGenerator, validationService,
              idObfuscator, auditLogService, translationService);
        this.invoiceFullGetService = invoiceFullGetService;
        this.invoiceDocumentCreateService = invoiceDocumentCreateService;
    }

    /**
     * Generate DOCX for invoice with optional translation.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> generateInvoiceDocx(String invoiceIdObfuscated, String templateIdObfuscated, String language) {
        return generateInvoiceDocx(invoiceIdObfuscated, templateIdObfuscated, language, null);
    }

    /**
     * Generate DOCX for invoice with optional translation and engine override.
     *
     * @param engine Optional engine id ("docx4j" or "libreoffice"). Null = global default.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> generateInvoiceDocx(
            String invoiceIdObfuscated,
            String templateIdObfuscated,
            String language,
            String engine
    ) {
        try {
            FullInvoiceDTO invoiceData = fetchInvoiceData(invoiceIdObfuscated);
            if (invoiceData == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Invoice not found: " + invoiceIdObfuscated, "INVOICE_NOT_FOUND")
                );
            }

            return generateDocxInternal(
                "FULL_INVOICE",
                invoiceData,
                templateIdObfuscated,
                language,
                engine,
                data -> extractInvoiceCode((FullInvoiceDTO) data)
            );

        } catch (Exception e) {
            log.error("Failed to generate invoice DOCX: {}", invoiceIdObfuscated, e);
            logDocxError("GENERATE_DOCX", "FULL_INVOICE", invoiceIdObfuscated, templateIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate DOCX: " + e.getMessage(), "DOCX_GENERATION_FAILED")
            );
        }
    }

    /**
     * Generate and save invoice DOCX to InvoiceDocuments (global default engine).
     */
    @Transactional
    public ResponseEntity<?> generateAndSaveInvoiceDocx(
            String invoiceIdObfuscated,
            String templateIdObfuscated,
            String language,
            InvoiceDocument.DocumentType invoiceDocumentType,
            String title,
            String version,
            String notes
    ) {
        return generateAndSaveInvoiceDocx(invoiceIdObfuscated, templateIdObfuscated, language,
            invoiceDocumentType, title, version, notes, null);
    }

    /**
     * Generate and save invoice DOCX with engine override.
     * Reuses the existing InvoiceDocumentCreateService (already accepts .docx MIME).
     */
    @Transactional
    public ResponseEntity<?> generateAndSaveInvoiceDocx(
            String invoiceIdObfuscated,
            String templateIdObfuscated,
            String language,
            InvoiceDocument.DocumentType invoiceDocumentType,
            String title,
            String version,
            String notes,
            String engine
    ) {
        try {
            FullInvoiceDTO invoiceData = fetchInvoiceData(invoiceIdObfuscated);
            if (invoiceData == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Invoice not found: " + invoiceIdObfuscated, "INVOICE_NOT_FOUND")
                );
            }

            Long invoiceId;
            try {
                invoiceId = idObfuscator.decodeId(invoiceIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid invoice ID", "INVALID_INVOICE_ID")
                );
            }

            ResponseEntity<?> docxResponse = generateInvoiceDocx(invoiceIdObfuscated, templateIdObfuscated, language, engine);
            if (!docxResponse.getStatusCode().is2xxSuccessful()) {
                return docxResponse;
            }

            byte[] docxBytes = (byte[]) docxResponse.getBody();
            if (docxBytes == null || docxBytes.length == 0) {
                return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Generated DOCX is empty", "EMPTY_DOCX")
                );
            }

            String invoiceCode = invoiceData.getInvoiceCode() != null ? invoiceData.getInvoiceCode() : "INV";
            String invoiceTitle = invoiceData.getTitle() != null ? invoiceData.getTitle() : "Invoice";

            InvoiceDocument.DocumentType docType = invoiceDocumentType != null
                ? invoiceDocumentType
                : InvoiceDocument.DocumentType.INVOICE_PDF;

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String languageSuffix = (language != null && !language.isBlank() && !"en".equalsIgnoreCase(language))
                ? "_" + language.toLowerCase()
                : "";
            String fileName = String.format("%s_%s%s_%s.docx",
                docType.name().toLowerCase(), invoiceCode, languageSuffix, timestamp);

            String documentTitle = title != null && !title.isBlank()
                ? title
                : String.format("%s - %s", docType.getDisplayName(), invoiceTitle);

            InvoiceDocumentDTO savedDocument = invoiceDocumentCreateService.saveGeneratedDocument(
                invoiceId,
                docxBytes,
                fileName,
                docType,
                documentTitle,
                null,
                version,
                notes
            );

            if (savedDocument == null) {
                log.warn("DOCX generated but failed to save to documents for invoice: {}", invoiceIdObfuscated);
                return docxResponse;
            }

            log.info("DOCX generated and saved for invoice: {}, document ID: {}", invoiceIdObfuscated, savedDocument.getId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(DOCX_MEDIA_TYPE));
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(docxBytes.length);
            headers.set("X-Document-Saved", "true");
            headers.set("X-Document-Id", savedDocument.getId());
            headers.set("X-Document-Url", savedDocument.getDocumentUrl());
            // Carry the engine id forward from the upstream response so save-and-download clients
            // can tell which engine produced the bytes.
            String engineHeader = docxResponse.getHeaders().getFirst("X-Docx-Engine");
            if (engineHeader != null) {
                headers.set("X-Docx-Engine", engineHeader);
            }

            return ResponseEntity.ok()
                .headers(headers)
                .body(docxBytes);

        } catch (Exception e) {
            log.error("Failed to generate and save DOCX for invoice: {}", invoiceIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate and save DOCX: " + e.getMessage(), "DOCX_SAVE_FAILED")
            );
        }
    }

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

    private String extractInvoiceCode(FullInvoiceDTO invoice) {
        String code = invoice.getInvoiceCode();
        return (code != null && !code.isBlank()) ? code : "invoice";
    }
}
