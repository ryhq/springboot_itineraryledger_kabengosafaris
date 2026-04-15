package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLog;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogService;
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
 * WordGenerationBaseService - Base class for Word (.docx) generation services.
 *
 * Mirrors {@link PdfGenerationBaseService} exactly: same template lookup,
 * same data-model build, same translation hook, same audit-log on failure.
 * Only the final converter (and content type / extension) differ — the same
 * Thymeleaf-rendered HTML feeds both PDF and DOCX output.
 *
 * Extended by document-specific services:
 *  - InvoiceWordGenerationService
 *  - QuoteWordGenerationService
 *  - SafariWordGenerationService
 *  - ItineraryWordGenerationService
 *  - PaymentReceiptWordGenerationService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WordGenerationBaseService {

    /**
     * MIME type for .docx (Office Open XML WordprocessingML).
     */
    public static final String DOCX_MEDIA_TYPE =
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    protected final PdfDocumentRepository pdfDocumentRepository;
    protected final PdfTemplateRepository pdfTemplateRepository;
    protected final PdfTemplateRenderer renderer;
    protected final WordGenerator wordGenerator;
    protected final PdfTemplateValidationService validationService;
    protected final IdObfuscator idObfuscator;
    protected final AuditLogService auditLogService;
    protected final TranslationService translationService;

    /**
     * Backwards-compatible overload — uses the global {@code docx.engine} setting.
     */
    @Transactional(readOnly = true)
    protected ResponseEntity<?> generateDocxInternal(
            String documentName,
            Object data,
            String templateIdObfuscated,
            String language,
            CodeExtractor codeExtractor
    ) {
        return generateDocxInternal(documentName, data, templateIdObfuscated, language, null, codeExtractor);
    }

    /**
     * Core DOCX generation logic used by all document types.
     *
     * @param documentName The PDF document type name (e.g. "FULL_INVOICE")
     * @param data The data object to render (e.g. FullInvoiceDTO)
     * @param templateIdObfuscated Optional specific template ID
     * @param language Optional target language code for translation
     * @param engineOverride Optional engine id ("docx4j" or "libreoffice"). Null = use global config.
     * @param codeExtractor Function to extract document code from data
     * @return ResponseEntity with DOCX bytes or error
     */
    @Transactional(readOnly = true)
    protected ResponseEntity<?> generateDocxInternal(
            String documentName,
            Object data,
            String templateIdObfuscated,
            String language,
            String engineOverride,
            CodeExtractor codeExtractor
    ) {
        try {
            // 1. Find document type (reuses the same PDF document catalog)
            PdfDocument document = pdfDocumentRepository.findByName(documentName).orElse(null);
            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Document type not found: " + documentName, "DOCUMENT_TYPE_NOT_FOUND")
                );
            }

            if (!document.getEnabled()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Document type is disabled", "DOCUMENT_TYPE_DISABLED")
                );
            }

            // 2. Find template (same templates as PDF)
            PdfTemplate template = resolveTemplate(document, templateIdObfuscated);
            if (template == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Template not found", "TEMPLATE_NOT_FOUND")
                );
            }

            if (!template.getEnabled()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Template is disabled", "TEMPLATE_DISABLED")
                );
            }

            // 3. Build data model
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put(document.getRootVariableName(), data);

            // 4. Render template (shared with PDF path)
            String renderedHtml = renderer.renderTemplate(template.getFileName(), dataModel);

            // 5. Validate rendered HTML for XHTML compliance — XHTMLImporter requires it
            validationService.validateForPdf(renderedHtml, template.getName());

            // 6. Translate HTML if language is specified and not English
            String finalHtml = translateIfNeeded(renderedHtml, template.getName(), language);
            boolean translated = !finalHtml.equals(renderedHtml);

            // 7. Generate DOCX (engineOverride=null → router uses global docx.engine config)
            byte[] docxBytes = wordGenerator.generateDocx(finalHtml, template, engineOverride);
            // Report the engine that actually produced the bytes (accounts for fallback-to-docx4j)
            String engineUsed = wordGenerator.activeEngineIdFor(engineOverride);

            // 8. Generate filename
            String fileName = generateFileName(codeExtractor.extractCode(data), translated ? language : null);

            // 9. Return DOCX response
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(DOCX_MEDIA_TYPE));
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(docxBytes.length);
            // Expose the engine used so A/B comparisons can tell outputs apart from the wire
            headers.set("X-Docx-Engine", engineUsed);

            log.info("Generated DOCX via {}: {} for {} (size: {} bytes{})",
                engineUsed, fileName, documentName, docxBytes.length,
                translated ? ", language: " + language : "");

            return ResponseEntity.ok()
                .headers(headers)
                .body(docxBytes);

        } catch (Exception e) {
            log.error("DOCX generation failed for {}", documentName, e);
            logDocxError("GENERATE_DOCX", documentName, null, templateIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate DOCX: " + e.getMessage(), "DOCX_GENERATION_FAILED")
            );
        }
    }

    /**
     * Resolve template by ID or use default template.
     */
    protected PdfTemplate resolveTemplate(PdfDocument document, String templateIdObfuscated) {
        if (templateIdObfuscated != null && !templateIdObfuscated.isBlank()) {
            try {
                Long templateId = idObfuscator.decodeId(templateIdObfuscated);
                if (templateId != null) {
                    PdfTemplate template = pdfTemplateRepository.findById(templateId).orElse(null);
                    if (template != null && template.getPdfDocument().getId().equals(document.getId())) {
                        return template;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to decode template ID: {}", templateIdObfuscated, e);
            }
        }

        // Use default template
        return pdfTemplateRepository.findByPdfDocumentIdAndIsDefaultAndEnabled(
            document.getId(), true, true
        ).orElse(null);
    }

    /**
     * Translate HTML if language is specified and not English.
     */
    protected String translateIfNeeded(String html, String templateName, String language) {
        if (language == null || language.isBlank() || "en".equalsIgnoreCase(language)) {
            return html;
        }

        try {
            log.info("Translating DOCX content to language: {}", language);
            String translatedHtml = translationService.translateHtml(html, language);

            // Verify translated HTML is still valid XHTML for the importer
            try {
                validationService.validateForPdf(translatedHtml, templateName + " (translated)");
                log.info("DOCX content translated successfully to: {}", language);
                return translatedHtml;
            } catch (Exception validationEx) {
                log.warn("Translated HTML validation failed, using original English content: {}", validationEx.getMessage());
                return html;
            }
        } catch (Exception e) {
            log.warn("Translation failed, using original English content: {}", e.getMessage());
            return html;
        }
    }

    /**
     * Generate filename with optional language suffix.
     */
    protected String generateFileName(String code, String language) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        if (language != null && !language.isBlank()) {
            return String.format("%s_%s_%s.docx", code, language.toLowerCase(), timestamp);
        }
        return String.format("%s_%s.docx", code, timestamp);
    }

    /**
     * Log DOCX processing errors to audit log.
     */
    protected void logDocxError(String action, String documentName, String dataId, String templateId, Exception exception) {
        try {
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

            StringBuilder description = new StringBuilder();
            description.append("DOCX generation failed for document type: ").append(documentName);
            if (dataId != null) {
                description.append(", data ID: ").append(dataId);
            }
            if (templateId != null && !templateId.isBlank()) {
                description.append(", template ID: ").append(templateId);
            }

            String newValues = String.format(
                "{\"documentType\":\"%s\",\"dataId\":\"%s\",\"templateId\":\"%s\",\"exceptionType\":\"%s\"}",
                documentName,
                dataId != null ? dataId : "",
                templateId != null ? templateId : "",
                exception.getClass().getSimpleName()
            );

            String errorMessage = exception.getMessage();
            if (errorMessage != null && errorMessage.length() > 1000) {
                errorMessage = errorMessage.substring(0, 1000) + "... [TRUNCATED]";
            }

            AuditLog auditLog = AuditLog.builder()
                .userId(userId != null ? userId : 0L)
                .username(username != null ? username : "SYSTEM")
                .action(action)
                .entityType("DOCX_DOCUMENT")
                .description(description.toString())
                .newValues(newValues)
                .status("FAILED")
                .errorMessage(errorMessage)
                .build();

            auditLogService.logAction(auditLog);

        } catch (Exception e) {
            log.warn("Failed to log DOCX error to audit log", e);
        }
    }

    /**
     * Functional interface for extracting document code from data.
     */
    @FunctionalInterface
    protected interface CodeExtractor {
        String extractCode(Object data);
    }
}
