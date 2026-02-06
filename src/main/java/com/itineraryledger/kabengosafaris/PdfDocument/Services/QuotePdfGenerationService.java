package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogService;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfDocumentRepository;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfTemplateRepository;
import com.itineraryledger.kabengosafaris.Quote.DTOs.FullQuoteDTO;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteDocumentDTOs.QuoteDocumentDTO;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteDocument;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteDocumentServices.QuoteDocumentCreateService;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteFullGetService;
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

/**
 * QuotePdfGenerationService - Handles PDF generation for quotes
 *
 * Responsibilities:
 * - Generate quote PDFs with optional translation
 * - Generate and save quote PDFs to QuoteDocuments
 * - Preview quote PDFs (HTML only)
 */
@Service
@Slf4j
public class QuotePdfGenerationService extends PdfGenerationBaseService {

    private final QuoteFullGetService quoteFullGetService;
    private final QuoteDocumentCreateService quoteDocumentCreateService;

    public QuotePdfGenerationService(
            PdfDocumentRepository pdfDocumentRepository,
            PdfTemplateRepository pdfTemplateRepository,
            PdfTemplateRenderer renderer,
            PdfGenerator generator,
            PdfTemplateValidationService validationService,
            IdObfuscator idObfuscator,
            AuditLogService auditLogService,
            TranslationService translationService,
            QuoteFullGetService quoteFullGetService,
            QuoteDocumentCreateService quoteDocumentCreateService
    ) {
        super(pdfDocumentRepository, pdfTemplateRepository, renderer, generator, validationService,
              idObfuscator, auditLogService, translationService);
        this.quoteFullGetService = quoteFullGetService;
        this.quoteDocumentCreateService = quoteDocumentCreateService;
    }

    /**
     * Generate PDF for quote with optional translation
     *
     * @param quoteIdObfuscated The obfuscated quote ID
     * @param templateIdObfuscated Optional template ID
     * @param language Optional target language code for translation
     * @return ResponseEntity with PDF bytes or error
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> generateQuotePdf(String quoteIdObfuscated, String templateIdObfuscated, String language) {
        try {
            // Fetch quote data
            FullQuoteDTO quoteData = fetchQuoteData(quoteIdObfuscated);
            if (quoteData == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found: " + quoteIdObfuscated, "QUOTE_NOT_FOUND")
                );
            }

            // Generate PDF using base service
            return generatePdfInternal(
                "FULL_QUOTE",
                quoteData,
                templateIdObfuscated,
                language,
                data -> extractQuoteCode((FullQuoteDTO) data)
            );

        } catch (Exception e) {
            log.error("Failed to generate quote PDF: {}", quoteIdObfuscated, e);
            logPdfError("GENERATE_PDF", "FULL_QUOTE", quoteIdObfuscated, templateIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate PDF: " + e.getMessage(), "PDF_GENERATION_FAILED")
            );
        }
    }

    /**
     * Generate and save quote PDF to QuoteDocuments
     *
     * @param quoteIdObfuscated The obfuscated quote ID
     * @param templateIdObfuscated Optional template ID
     * @param language Optional target language code for translation
     * @param quoteDocumentType The type of document to save as (e.g., QUOTATION, FINAL_QUOTE)
     * @param title Optional custom title (auto-generated if null)
     * @param version Optional version string
     * @param notes Optional notes
     * @return ResponseEntity with PDF bytes (document is also saved) or error
     */
    @Transactional
    public ResponseEntity<?> generateAndSaveQuotePdf(
            String quoteIdObfuscated,
            String templateIdObfuscated,
            String language,
            QuoteDocument.DocumentType quoteDocumentType,
            String title,
            String version,
            String notes
    ) {
        try {
            // 1. Fetch quote data first
            FullQuoteDTO quoteData = fetchQuoteData(quoteIdObfuscated);
            if (quoteData == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found: " + quoteIdObfuscated, "QUOTE_NOT_FOUND")
                );
            }

            // 2. Decode quote ID for saving
            Long quoteId;
            try {
                quoteId = idObfuscator.decodeId(quoteIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid quote ID", "INVALID_QUOTE_ID")
                );
            }

            // 3. Generate the PDF
            ResponseEntity<?> pdfResponse = generateQuotePdf(quoteIdObfuscated, templateIdObfuscated, language);

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
            String quoteCode = quoteData.getQuoteCode() != null ? quoteData.getQuoteCode() : "QTE";
            String quoteTitle = quoteData.getTitle() != null ? quoteData.getTitle() : "Quote";

            QuoteDocument.DocumentType docType = quoteDocumentType != null
                ? quoteDocumentType
                : QuoteDocument.DocumentType.QUOTE_PDF;

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String languageSuffix = (language != null && !language.isBlank() && !"en".equalsIgnoreCase(language))
                ? "_" + language.toLowerCase()
                : "";
            String fileName = String.format("%s_%s%s_%s.pdf",
                docType.name().toLowerCase(), quoteCode, languageSuffix, timestamp);

            String documentTitle = title != null && !title.isBlank()
                ? title
                : String.format("%s - %s", docType.getDisplayName(), quoteTitle);

            // 6. Save the document
            QuoteDocumentDTO savedDocument = quoteDocumentCreateService.saveGeneratedDocument(
                quoteId,
                pdfBytes,
                fileName,
                docType,
                documentTitle,
                null, // description
                version,
                notes
            );

            if (savedDocument == null) {
                log.warn("PDF generated but failed to save to documents for quote: {}", quoteIdObfuscated);
                return pdfResponse;
            }

            log.info("Generated and saved PDF document: {} for quote {} (document ID: {})",
                fileName, quoteIdObfuscated, savedDocument.getId());

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
            log.error("Failed to generate and save PDF for quote: {}", quoteIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate and save PDF: " + e.getMessage(), "PDF_SAVE_FAILED")
            );
        }
    }

    /**
     * Preview quote PDF (return rendered HTML instead of PDF)
     *
     * @param quoteIdObfuscated The obfuscated quote ID
     * @param templateIdObfuscated Optional template ID
     * @param language Optional target language code for translation
     * @return ResponseEntity with rendered HTML or error
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> previewQuotePdf(String quoteIdObfuscated, String templateIdObfuscated, String language) {
        try {
            FullQuoteDTO quoteData = fetchQuoteData(quoteIdObfuscated);
            if (quoteData == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
                );
            }

            return previewPdfInternal("FULL_QUOTE", quoteData, templateIdObfuscated, language);

        } catch (Exception e) {
            log.error("Failed to preview quote PDF: {}", quoteIdObfuscated, e);
            logPdfError("PREVIEW_PDF", "FULL_QUOTE", quoteIdObfuscated, templateIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate preview: " + e.getMessage(), "PREVIEW_FAILED")
            );
        }
    }

    /**
     * Fetch full quote data
     */
    private FullQuoteDTO fetchQuoteData(String quoteIdObfuscated) {
        try {
            ResponseEntity<ApiResponse<?>> response = quoteFullGetService.getFullQuote(quoteIdObfuscated);

            if (response.getStatusCode().is2xxSuccessful()) {
                ApiResponse<?> apiResponse = response.getBody();
                if (apiResponse != null && apiResponse.getData() instanceof FullQuoteDTO) {
                    return (FullQuoteDTO) apiResponse.getData();
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch quote data: {}", quoteIdObfuscated, e);
            return null;
        }
    }

    /**
     * Extract quote code from data
     */
    private String extractQuoteCode(FullQuoteDTO quote) {
        String code = quote.getQuoteCode();
        return (code != null && !code.isBlank()) ? code : "quote";
    }
}
