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
 * PdfGenerationBaseService - Base class for PDF generation services
 *
 * Contains shared logic for:
 * - Template resolution and validation
 * - HTML rendering and translation
 * - PDF generation from HTML
 * - Error logging and audit trails
 *
 * Extended by document-specific services:
 * - ItineraryPdfGenerationService
 * - QuotePdfGenerationService
 * - SafariPdfGenerationService
 * - InvoicePdfGenerationService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfGenerationBaseService {

    protected final PdfDocumentRepository pdfDocumentRepository;
    protected final PdfTemplateRepository pdfTemplateRepository;
    protected final PdfTemplateRenderer renderer;
    protected final PdfGenerator generator;
    protected final PdfTemplateValidationService validationService;
    protected final IdObfuscator idObfuscator;
    protected final AuditLogService auditLogService;
    protected final TranslationService translationService;

    /**
     * Core PDF generation logic used by all document types
     *
     * @param documentName The PDF document type name (e.g., "FULL_ITINERARY")
     * @param data The data object to render (e.g., FullItineraryDTO)
     * @param templateIdObfuscated Optional specific template ID
     * @param language Optional target language code for translation
     * @param codeExtractor Function to extract document code from data
     * @return ResponseEntity with PDF bytes or error
     */
    @Transactional(readOnly = true)
    protected ResponseEntity<?> generatePdfInternal(
            String documentName,
            Object data,
            String templateIdObfuscated,
            String language,
            CodeExtractor codeExtractor
    ) {
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

            // 4. Render template
            String renderedHtml = renderer.renderTemplate(template.getFileName(), dataModel);

            // 5. Validate rendered HTML for XHTML compliance BEFORE translation
            validationService.validateForPdf(renderedHtml, template.getName());

            // 6. Translate HTML if language is specified and not English
            String finalHtml = translateIfNeeded(renderedHtml, template.getName(), language);
            boolean translated = !finalHtml.equals(renderedHtml);

            // 7. Generate PDF
            byte[] pdfBytes = generator.generatePdf(finalHtml, template);

            // 8. Generate filename
            String fileName = generateFileName(codeExtractor.extractCode(data), translated ? language : null);

            // 9. Return PDF response
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(pdfBytes.length);

            log.info("Generated PDF: {} for {} (size: {} bytes{})",
                fileName, documentName, pdfBytes.length,
                translated ? ", language: " + language : "");

            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);

        } catch (Exception e) {
            log.error("PDF generation failed for {}", documentName, e);
            logPdfError("GENERATE_PDF", documentName, null, templateIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate PDF: " + e.getMessage(), "PDF_GENERATION_FAILED")
            );
        }
    }

    /**
     * Core preview generation logic used by all document types
     */
    @Transactional(readOnly = true)
    protected ResponseEntity<ApiResponse<?>> previewPdfInternal(
            String documentName,
            Object data,
            String templateIdObfuscated,
            String language
    ) {
        try {
            PdfDocument document = pdfDocumentRepository.findByName(documentName).orElse(null);
            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "PDF document type not found", "DOCUMENT_TYPE_NOT_FOUND")
                );
            }

            PdfTemplate template = resolveTemplate(document, templateIdObfuscated);
            if (template == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Template not found", "TEMPLATE_NOT_FOUND")
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

            // Validate the rendered HTML
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
            log.error("PDF preview failed for {}", documentName, e);
            logPdfError("PREVIEW_PDF", documentName, null, templateIdObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to generate preview: " + e.getMessage(), "PREVIEW_FAILED")
            );
        }
    }

    /**
     * Resolve template by ID or use default template
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
     * Translate HTML if language is specified and not English
     */
    protected String translateIfNeeded(String html, String templateName, String language) {
        if (language == null || language.isBlank() || "en".equalsIgnoreCase(language)) {
            return html;
        }

        try {
            log.info("Translating PDF content to language: {}", language);
            String translatedHtml = translationService.translateHtml(html, language);

            // Verify translated HTML is still valid XHTML
            try {
                validationService.validateForPdf(translatedHtml, templateName + " (translated)");
                log.info("PDF content translated successfully to: {}", language);
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
     * Generate filename with optional language suffix
     */
    protected String generateFileName(String code, String language) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        if (language != null && !language.isBlank()) {
            return String.format("%s_%s_%s.pdf", code, language.toLowerCase(), timestamp);
        }
        return String.format("%s_%s.pdf", code, timestamp);
    }

    /**
     * Log PDF processing errors to audit log
     */
    protected void logPdfError(String action, String documentName, String dataId, String templateId, Exception exception) {
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
            description.append("PDF ").append(action.equals("GENERATE_PDF") ? "generation" : "preview");
            description.append(" failed for document type: ").append(documentName);
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
                .entityType("PDF_DOCUMENT")
                .description(description.toString())
                .newValues(newValues)
                .status("FAILED")
                .errorMessage(errorMessage)
                .build();

            auditLogService.logAction(auditLog);

        } catch (Exception e) {
            log.warn("Failed to log PDF error to audit log", e);
        }
    }

    /**
     * Functional interface for extracting document code from data
     */
    @FunctionalInterface
    protected interface CodeExtractor {
        String extractCode(Object data);
    }
}
