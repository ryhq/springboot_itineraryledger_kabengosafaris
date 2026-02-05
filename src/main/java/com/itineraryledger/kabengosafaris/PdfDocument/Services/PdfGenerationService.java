package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLog;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogService;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryDocumentDTOs.ItineraryDocumentDTO;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.ItineraryDocument;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryDocumentServices.ItineraryDocumentCreateService;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryFullGetService;
import com.itineraryledger.kabengosafaris.Quote.DTOs.FullQuoteDTO;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteDocumentDTOs.QuoteDocumentDTO;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteDocument;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteDocumentServices.QuoteDocumentCreateService;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteFullGetService;
import com.itineraryledger.kabengosafaris.Safari.DTOs.FullSafariDTO;
import com.itineraryledger.kabengosafaris.Safari.DTOs.SafariDocumentDTOs.SafariDocumentDTO;
import com.itineraryledger.kabengosafaris.Safari.Entity.SafariDocument;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariDocumentServices.SafariDocumentCreateService;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariFullGetService;
import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfDocument;
import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfTemplate;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfDocumentRepository;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfTemplateRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Translation.Services.TranslationService;
import com.itineraryledger.kabengosafaris.User.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * PdfGenerationService - Orchestrates PDF generation from templates and data
 *
 * Responsibilities:
 * - Fetch data based on document type and data ID
 * - Select appropriate template
 * - Render template with data
 * - Generate PDF and return as bytes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfGenerationService {

    private final PdfDocumentRepository pdfDocumentRepository;
    private final PdfTemplateRepository pdfTemplateRepository;
    private final PdfTemplateRenderer renderer;
    private final PdfGenerator generator;
    private final PdfTemplateValidationService validationService;
    private final IdObfuscator idObfuscator;
    private final AuditLogService auditLogService;

    // Data providers
    private final ItineraryFullGetService itineraryFullGetService;
    private final QuoteFullGetService quoteFullGetService;
    private final SafariFullGetService safariFullGetService;

    // Translation service
    private final TranslationService translationService;

    // Document storage services
    private final ItineraryDocumentCreateService itineraryDocumentCreateService;
    private final QuoteDocumentCreateService quoteDocumentCreateService;
    private final SafariDocumentCreateService safariDocumentCreateService;

    /**
     * Generate PDF for a specific document type and data ID
     *
     * @param documentName The PDF document type name (e.g., "FULL_ITINERARY")
     * @param dataId The ID of the data source (e.g., itinerary ID)
     * @param templateIdObfuscated Optional specific template ID (uses default if null)
     * @return ResponseEntity with PDF bytes or error
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> generatePdf(String documentName, String dataId, String templateIdObfuscated) {
        return generatePdf(documentName, dataId, templateIdObfuscated, null);
    }

    /**
     * Generate PDF for a specific document type and data ID with optional translation
     *
     * @param documentName The PDF document type name (e.g., "FULL_ITINERARY")
     * @param dataId The ID of the data source (e.g., itinerary ID)
     * @param templateIdObfuscated Optional specific template ID (uses default if null)
     * @param language Optional target language code for translation (e.g., "fr", "de")
     * @return ResponseEntity with PDF bytes or error
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> generatePdf(String documentName, String dataId, String templateIdObfuscated, String language) {
        try {
            // 1. Find document type
            PdfDocument document = pdfDocumentRepository.findByName(documentName).orElse(null);
            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "PDF document type not found: " + documentName, "DOCUMENT_TYPE_NOT_FOUND")
                );
            }

            if (!document.getEnabled()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "PDF document type is disabled", "DOCUMENT_TYPE_DISABLED")
                );
            }

            // 2. Find template
            PdfTemplate template;
            if (templateIdObfuscated != null && !templateIdObfuscated.isBlank()) {
                Long templateId = idObfuscator.decodeId(templateIdObfuscated);
                if (templateId == null) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid template ID", "INVALID_TEMPLATE_ID")
                    );
                }
                template = pdfTemplateRepository.findById(templateId).orElse(null);
                if (template == null) {
                    return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Template not found", "TEMPLATE_NOT_FOUND")
                    );
                }
                // Verify template belongs to this document type
                if (!template.getPdfDocument().getId().equals(document.getId())) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Template does not belong to this document type", "TEMPLATE_MISMATCH")
                    );
                }
            } else {
                // Use default template
                template = pdfTemplateRepository.findByPdfDocumentIdAndIsDefaultAndEnabled(
                    document.getId(), true, true
                ).orElse(null);

                if (template == null) {
                    return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "No default template found for document type", "NO_DEFAULT_TEMPLATE")
                    );
                }
            }

            if (!template.getEnabled()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Template is disabled", "TEMPLATE_DISABLED")
                );
            }

            // 3. Fetch data based on document type
            Object data = fetchData(documentName, dataId);
            if (data == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Data not found for ID: " + dataId, "DATA_NOT_FOUND")
                );
            }

            // 4. Build data model
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put(document.getRootVariableName(), data);

            // 5. Render template
            String renderedHtml = renderer.renderTemplate(template.getFileName(), dataModel);

            // 6. Validate rendered HTML for XHTML compliance BEFORE translation
            // This throws PdfTemplateValidationException with detailed errors if validation fails
            validationService.validateForPdf(renderedHtml, template.getName());

            // 7. Translate HTML if language is specified and not English
            String finalHtml = renderedHtml;
            boolean translated = false;
            if (language != null && !language.isBlank() && !"en".equalsIgnoreCase(language)) {
                try {
                    log.info("Translating PDF content to language: {}", language);
                    String translatedHtml = translationService.translateHtml(renderedHtml, language);

                    // Verify translated HTML is still valid XHTML
                    // If translation corrupted the HTML, fall back to English
                    try {
                        validationService.validateForPdf(translatedHtml, template.getName() + " (translated)");
                        finalHtml = translatedHtml;
                        translated = !finalHtml.equals(renderedHtml);
                        if (translated) {
                            log.info("PDF content translated successfully to: {}", language);
                        }
                    } catch (Exception validationEx) {
                        log.warn("Translated HTML validation failed, using original English content: {}", validationEx.getMessage());
                        finalHtml = renderedHtml;
                    }
                } catch (Exception e) {
                    log.warn("Translation failed, using original English content: {}", e.getMessage());
                    // Fallback to original HTML on any translation error
                    finalHtml = renderedHtml;
                }
            }

            // 8. Generate PDF
            byte[] pdfBytes = generator.generatePdf(finalHtml, template);

            // 9. Generate filename using document code (include language suffix if translated)
            String fileName = generateFileName(documentName, data, translated ? language : null);

            // 10. Return PDF response
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(pdfBytes.length);

            log.info("Generated PDF: {} for {} (ID: {}), size: {} bytes{}",
                fileName, documentName, dataId, pdfBytes.length,
                translated ? ", language: " + language : "");

            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);

        } catch (Exception e) {
            log.error("PDF generation failed for {} / {}", documentName, dataId, e);

            // Log error to audit log
            logPdfError("GENERATE_PDF", documentName, dataId, templateIdObfuscated, e);

            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate PDF: " + e.getMessage(), "PDF_GENERATION_FAILED")
            );
        }
    }

    /**
     * Generate PDF for itinerary with optional translation
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param templateIdObfuscated Optional template ID
     * @param language Optional target language code for translation (e.g., "fr", "de")
     *                 If null, empty, or "en", PDF is generated in English
     * @return ResponseEntity with PDF bytes or error
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> generateItineraryPdf(String itineraryIdObfuscated, String templateIdObfuscated, String language) {
        return generatePdf("FULL_ITINERARY", itineraryIdObfuscated, templateIdObfuscated, language);
    }

    /**
     * Generate PDF for itinerary and save it to ItineraryDocuments.
     * Returns the PDF bytes and also stores it as a system-generated document.
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param templateIdObfuscated Optional template ID
     * @param language Optional target language code for translation
     * @param itineraryDocumentType The type of document to save as (e.g., QUOTATION, FINAL_ITINERARY)
     * @param title Optional custom title (auto-generated if null)
     * @param version Optional version string
     * @param notes Optional notes
     * @return ResponseEntity with PDF bytes (document is also saved) or error
     */
    @Transactional
    public ResponseEntity<?> generateAndSaveItineraryPdf(
            String itineraryIdObfuscated,
            String templateIdObfuscated,
            String language,
            ItineraryDocument.DocumentType itineraryDocumentType,
            String title,
            String version,
            String notes
    ) {
        try {
            // 1. Fetch itinerary data first to get code and name for title generation
            FullItineraryDTO itineraryData = fetchItineraryData(itineraryIdObfuscated);
            if (itineraryData == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary not found: " + itineraryIdObfuscated, "ITINERARY_NOT_FOUND")
                );
            }

            // 2. Decode itinerary ID for saving
            Long itineraryId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid itinerary ID", "INVALID_ITINERARY_ID")
                );
            }

            // 3. Generate the PDF using existing method
            ResponseEntity<?> pdfResponse = generatePdf("FULL_ITINERARY", itineraryIdObfuscated, templateIdObfuscated, language);

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
            String itineraryCode = itineraryData.getCode() != null ? itineraryData.getCode() : "ITI";
            String itineraryName = itineraryData.getName() != null ? itineraryData.getName() : "Itinerary";

            ItineraryDocument.DocumentType docType = itineraryDocumentType != null
                ? itineraryDocumentType
                : ItineraryDocument.DocumentType.FINAL_ITINERARY;

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String languageSuffix = (language != null && !language.isBlank() && !"en".equalsIgnoreCase(language))
                ? "_" + language.toLowerCase()
                : "";
            String fileName = String.format("%s_%s%s_%s.pdf",
                docType.name().toLowerCase(), itineraryCode, languageSuffix, timestamp);

            String documentTitle = title != null && !title.isBlank()
                ? title
                : String.format("%s - %s", docType.getDisplayName(), itineraryName);

            // 6. Save the document
            ItineraryDocumentDTO savedDocument = itineraryDocumentCreateService.saveGeneratedDocument(
                itineraryId,
                pdfBytes,
                fileName,
                docType,
                documentTitle,
                null, // description
                version,
                notes
            );

            if (savedDocument == null) {
                log.warn("PDF generated but failed to save to documents for itinerary: {}", itineraryIdObfuscated);
                // Still return the PDF even if saving failed
                return pdfResponse;
            }

            log.info("Generated and saved PDF document: {} for itinerary {} (document ID: {})",
                fileName, itineraryIdObfuscated, savedDocument.getId());

            // 7. Return the PDF response with additional header indicating document was saved
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
            log.error("Failed to generate and save PDF for itinerary: {}", itineraryIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate and save PDF: " + e.getMessage(), "PDF_SAVE_FAILED")
            );
        }
    }

    // =====================================================================
    // QUOTE PDF GENERATION METHODS
    // =====================================================================

    /**
     * Convenience method to generate quote PDF
     *
     * @param quoteIdObfuscated The obfuscated quote ID
     * @param templateIdObfuscated Optional template ID
     * @param language Optional target language code for translation
     * @return ResponseEntity with PDF bytes or error
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> generateQuotePdf(String quoteIdObfuscated, String templateIdObfuscated, String language) {
        return generatePdf("FULL_QUOTE", quoteIdObfuscated, templateIdObfuscated, language);
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
            // 1. Fetch quote data first to get code and title for filename generation
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

            // 3. Generate the PDF using existing method
            ResponseEntity<?> pdfResponse = generatePdf("FULL_QUOTE", quoteIdObfuscated, templateIdObfuscated, language);

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
                // Still return the PDF even if saving failed
                return pdfResponse;
            }

            log.info("Generated and saved PDF document: {} for quote {} (document ID: {})",
                fileName, quoteIdObfuscated, savedDocument.getId());

            // 7. Return the PDF response with additional header indicating document was saved
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

    // =====================================================================
    // SAFARI PDF GENERATION METHODS
    // =====================================================================

    /**
     * Convenience method to generate safari PDF
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param templateIdObfuscated Optional template ID
     * @param language Optional target language code for translation
     * @return ResponseEntity with PDF bytes or error
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> generateSafariPdf(String safariIdObfuscated, String templateIdObfuscated, String language) {
        return generatePdf("FULL_SAFARI", safariIdObfuscated, templateIdObfuscated, language);
    }

    /**
     * Generate Safari PDF and save it as a SafariDocument
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param templateIdObfuscated Optional template ID
     * @param language Optional target language code for translation
     * @param safariDocumentType The document type (e.g., QUOTATION, FINAL_ITINERARY)
     * @param title Optional custom title for the document
     * @param version Optional version string
     * @param notes Optional notes
     * @return ResponseEntity with PDF bytes and saved document metadata
     */
    public ResponseEntity<?> generateAndSaveSafariPdf(
            String safariIdObfuscated,
            String templateIdObfuscated,
            String language,
            SafariDocument.DocumentType safariDocumentType,
            String title,
            String version,
            String notes
    ) {
        try {
            // 1. Fetch safari data first to get code and name for title generation
            FullSafariDTO safariData = fetchSafariData(safariIdObfuscated);
            if (safariData == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari not found: " + safariIdObfuscated, "SAFARI_NOT_FOUND")
                );
            }

            // 2. Decode safari ID for saving
            Long safariId;
            try {
                safariId = idObfuscator.decodeId(safariIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid safari ID", "INVALID_SAFARI_ID")
                );
            }

            // 3. Generate the PDF using existing method
            ResponseEntity<?> pdfResponse = generatePdf("FULL_SAFARI", safariIdObfuscated, templateIdObfuscated, language);

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
            String safariCode = safariData.getCode() != null ? safariData.getCode() : "SAF";
            String safariName = safariData.getName() != null ? safariData.getName() : "Safari";

            SafariDocument.DocumentType docType = safariDocumentType != null
                ? safariDocumentType
                : SafariDocument.DocumentType.FINAL_ITINERARY;

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String languageSuffix = (language != null && !language.isBlank() && !"en".equalsIgnoreCase(language))
                ? "_" + language.toLowerCase()
                : "";
            String fileName = String.format("%s_%s%s_%s.pdf",
                docType.name().toLowerCase(), safariCode, languageSuffix, timestamp);

            String documentTitle = title != null && !title.isBlank()
                ? title
                : String.format("%s - %s", docType.getDisplayName(), safariName);

            // 6. Save the document
            SafariDocumentDTO savedDocument = safariDocumentCreateService.saveGeneratedDocument(
                safariId,
                pdfBytes,
                fileName,
                docType,
                documentTitle,
                null, // description
                version,
                notes
            );

            if (savedDocument == null) {
                log.warn("PDF generated but failed to save to documents for safari: {}", safariIdObfuscated);
                // Still return the PDF even if saving failed
                return pdfResponse;
            }

            log.info("Generated and saved PDF document: {} for safari {} (document ID: {})",
                fileName, safariIdObfuscated, savedDocument.getId());

            // 7. Return the PDF response with additional header indicating document was saved
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
            log.error("Failed to generate and save PDF for safari: {}", safariIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate and save PDF: " + e.getMessage(), "PDF_SAVE_FAILED")
            );
        }
    }

    /**
     * Preview PDF (return rendered HTML instead of PDF)
     * Useful for template development
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> previewPdf(String documentName, String dataId, String templateIdObfuscated) {
        return previewPdf(documentName, dataId, templateIdObfuscated, null);
    }

    /**
     * Preview PDF with optional translation (return rendered HTML instead of PDF)
     * Useful for template development and translation preview
     *
     * @param documentName The PDF document type name
     * @param dataId The ID of the data source
     * @param templateIdObfuscated Optional specific template ID
     * @param language Optional target language code for translation
     * @return ResponseEntity with rendered HTML or error
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> previewPdf(String documentName, String dataId, String templateIdObfuscated, String language) {
        try {
            // Similar to generatePdf but returns HTML
            PdfDocument document = pdfDocumentRepository.findByName(documentName).orElse(null);
            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "PDF document type not found", "DOCUMENT_TYPE_NOT_FOUND")
                );
            }

            PdfTemplate template;
            if (templateIdObfuscated != null && !templateIdObfuscated.isBlank()) {
                Long templateId = idObfuscator.decodeId(templateIdObfuscated);
                template = pdfTemplateRepository.findById(templateId).orElse(null);
            } else {
                template = pdfTemplateRepository.findByPdfDocumentIdAndIsDefaultAndEnabled(
                    document.getId(), true, true
                ).orElse(null);
            }

            if (template == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Template not found", "TEMPLATE_NOT_FOUND")
                );
            }

            Object data = fetchData(documentName, dataId);
            if (data == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Data not found", "DATA_NOT_FOUND")
                );
            }

            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put(document.getRootVariableName(), data);

            String renderedHtml = renderer.renderTemplate(template.getFileName(), dataModel);

            // Translate HTML if language is specified and not English
            String finalHtml = renderedHtml;
            boolean translated = false;
            String translationError = null;
            if (language != null && !language.isBlank() && !"en".equalsIgnoreCase(language)) {
                try {
                    log.info("Translating preview content to language: {}", language);
                    String translatedHtml = translationService.translateHtml(renderedHtml, language);
                    if (translatedHtml != null && !translatedHtml.equals(renderedHtml)) {
                        finalHtml = translatedHtml;
                        translated = true;
                        log.info("Preview content translated successfully to: {}", language);
                    }
                } catch (Exception e) {
                    log.warn("Translation failed for preview, using original English content: {}", e.getMessage());
                    translationError = e.getMessage();
                }
            }

            // Validate the rendered HTML and include validation status in response
            var validationResult = validationService.validate(finalHtml, template.getName());

            Map<String, Object> response = new HashMap<>();
            response.put("html", finalHtml);
            response.put("templateName", template.getName());
            response.put("paperSize", template.getPaperSize().getDisplayName());
            response.put("orientation", template.getOrientation().getDisplayName());
            response.put("validForPdf", validationResult.valid());
            response.put("language", translated ? language : "en");
            response.put("translated", translated);
            if (translationError != null) {
                response.put("translationError", translationError);
            }

            if (!validationResult.valid()) {
                response.put("validationErrors", validationResult.errors().stream()
                        .map(error -> {
                            var detail = new HashMap<String, Object>();
                            detail.put("errorType", error.getErrorType());
                            detail.put("message", error.getMessage());
                            if (error.getLineNumber() != null) {
                                detail.put("lineNumber", error.getLineNumber());
                            }
                            if (error.getSuggestion() != null) {
                                detail.put("suggestion", error.getSuggestion());
                            }
                            return detail;
                        })
                        .toList());
            }

            String message = translated
                ? "Preview generated and translated successfully"
                : "Preview generated successfully";

            return ResponseEntity.ok(ApiResponse.success(200, message, response));

        } catch (Exception e) {
            log.error("PDF preview failed for {} / {}", documentName, dataId, e);

            // Log error to audit log
            logPdfError("PREVIEW_PDF", documentName, dataId, templateIdObfuscated, e);

            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate preview: " + e.getMessage(), "PREVIEW_FAILED")
            );
        }
    }

    /**
     * Fetch data based on document type
     */
    private Object fetchData(String documentName, String dataId) {
        return switch (documentName) {
            case "FULL_ITINERARY" -> fetchItineraryData(dataId);
            case "FULL_QUOTE" -> fetchQuoteData(dataId);
            case "FULL_SAFARI" -> fetchSafariData(dataId);
            // Add more document types here as needed
            default -> {
                log.warn("Unknown document type: {}", documentName);
                yield null;
            }
        };
    }

    /**
     * Fetch full itinerary data
     */
    private FullItineraryDTO fetchItineraryData(String itineraryIdObfuscated) {
        try {
            ResponseEntity<ApiResponse<?>> response = itineraryFullGetService.getFullItinerary(itineraryIdObfuscated);

            if (response.getStatusCode().is2xxSuccessful()) {
                ApiResponse<?> apiResponse = response.getBody();
                if (apiResponse != null && apiResponse.getData() instanceof FullItineraryDTO) {
                    return (FullItineraryDTO) apiResponse.getData();
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch itinerary data: {}", itineraryIdObfuscated, e);
            return null;
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
     * Fetch full safari data
     */
    private FullSafariDTO fetchSafariData(String safariIdObfuscated) {
        try {
            ResponseEntity<ApiResponse<?>> response = safariFullGetService.getFullSafari(safariIdObfuscated);

            if (response.getStatusCode().is2xxSuccessful()) {
                ApiResponse<?> apiResponse = response.getBody();
                if (apiResponse != null && apiResponse.getData() instanceof FullSafariDTO) {
                    return (FullSafariDTO) apiResponse.getData();
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch safari data: {}", safariIdObfuscated, e);
            return null;
        }
    }

    /**
     * Generate a filename for the PDF using document code with optional language suffix
     *
     * @param documentType The document type name
     * @param data The data object (e.g., FullItineraryDTO)
     * @param language Optional language code to include in filename
     * @return Filename in format: {code}_{lang}_{timestamp}.pdf (e.g., ITI-00010126_fr_20260120_153045.pdf)
     */
    private String generateFileName(String documentType, Object data, String language) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        // Extract code from data based on document type
        String code = extractDocumentCode(documentType, data);

        if (language != null && !language.isBlank()) {
            return String.format("%s_%s_%s.pdf", code, language.toLowerCase(), timestamp);
        }
        return String.format("%s_%s.pdf", code, timestamp);
    }

    /**
     * Extract the document code from the data object based on document type
     *
     * @param documentType The document type name
     * @param data The data object
     * @return The document code or a fallback identifier
     */
    private String extractDocumentCode(String documentType, Object data) {
        return switch (documentType) {
            case "FULL_ITINERARY" -> {
                if (data instanceof FullItineraryDTO itinerary) {
                    String code = itinerary.getCode();
                    if (code != null && !code.isBlank()) {
                        yield code;
                    }
                }
                yield "itinerary";
            }
            case "FULL_QUOTE" -> {
                if (data instanceof FullQuoteDTO quote) {
                    String code = quote.getQuoteCode();
                    if (code != null && !code.isBlank()) {
                        yield code;
                    }
                }
                yield "quote";
            }
            case "FULL_SAFARI" -> {
                if (data instanceof FullSafariDTO safari) {
                    String code = safari.getCode();
                    if (code != null && !code.isBlank()) {
                        yield code;
                    }
                }
                yield "safari";
            }
            // Add more document types here as needed
            default -> documentType.toLowerCase().replace("_", "-");
        };
    }

    /**
     * Log PDF processing errors to audit log
     *
     * @param action The action being performed (GENERATE_PDF, PREVIEW_PDF)
     * @param documentName The PDF document type name
     * @param dataId The data source ID
     * @param templateId The template ID (may be null)
     * @param exception The exception that occurred
     */
    private void logPdfError(String action, String documentName, String dataId, String templateId, Exception exception) {
        try {
            // Get current user from security context
            Long userId = null;
            String username = "SYSTEM";
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getPrincipal().equals("anonymousUser")) {
                username = authentication.getName();
                Object principal = authentication.getPrincipal();
                if (principal instanceof User) {
                    userId = ((User) principal).getId();
                }
            }

            // Build description with context
            StringBuilder description = new StringBuilder();
            description.append("PDF ").append(action.equals("GENERATE_PDF") ? "generation" : "preview");
            description.append(" failed for document type: ").append(documentName);
            description.append(", data ID: ").append(dataId);
            if (templateId != null && !templateId.isBlank()) {
                description.append(", template ID: ").append(templateId);
            }

            // Build new values JSON with error context
            String newValues = String.format(
                "{\"documentType\":\"%s\",\"dataId\":\"%s\",\"templateId\":\"%s\",\"exceptionType\":\"%s\"}",
                documentName,
                dataId,
                templateId != null ? templateId : "",
                exception.getClass().getSimpleName()
            );

            // Truncate error message if too long
            String errorMessage = exception.getMessage();
            if (errorMessage != null && errorMessage.length() > 1000) {
                errorMessage = errorMessage.substring(0, 1000) + "... [TRUNCATED]";
            }

            AuditLog auditLog = AuditLog.builder()
                .userId(userId != null ? userId : 0L)
                .username(username != null ? username : "SYSTEM")
                .action(action)
                .entityType("PDF_DOCUMENT")
                .description(description.toString())
                .newValues(newValues)
                .status("FAILED")
                .errorMessage(errorMessage)
                .build();

            auditLogService.logAction(auditLog);

        } catch (Exception e) {
            // Don't let audit logging failure affect the main error response
            log.warn("Failed to log PDF error to audit log", e);
        }
    }
}
