package com.itineraryledger.kabengosafaris.PdfDocument.Controller;

import com.itineraryledger.kabengosafaris.PdfDocument.DTOs.GeneratePdfRequestDTO;
import com.itineraryledger.kabengosafaris.PdfDocument.DTOs.ValidateTemplateRequestDTO;
import com.itineraryledger.kabengosafaris.PdfDocument.Services.PdfGenerationService;
import com.itineraryledger.kabengosafaris.PdfDocument.Services.PdfTemplateValidationService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for PDF Generation
 *
 * Provides endpoints for generating PDFs from various document types.
 */
@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
@Slf4j
public class PdfGenerationController {

    private final PdfGenerationService generationService;
    private final PdfTemplateValidationService validationService;

    /**
     * Generate PDF using default template for document type
     * Supports optional language parameter for translation
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
    public ResponseEntity<?> generatePdf(@Valid @RequestBody GeneratePdfRequestDTO request) {
        log.info("POST /api/pdf/generate - Generating PDF for {} / {}{}",
            request.getDocumentType(), request.getDataId(),
            request.getLanguage() != null ? ", language: " + request.getLanguage() : "");
        return generationService.generatePdf(
            request.getDocumentType(),
            request.getDataId(),
            request.getTemplateId(),
            request.getLanguage()
        );
    }

    /**
     * Preview PDF (returns rendered HTML)
     */
    @PostMapping("/preview")
    @PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
    public ResponseEntity<ApiResponse<?>> previewPdf(@Valid @RequestBody GeneratePdfRequestDTO request) {
        log.info("POST /api/pdf/preview - Preview for {} / {}",
            request.getDocumentType(), request.getDataId());
        return generationService.previewPdf(
            request.getDocumentType(),
            request.getDataId(),
            request.getTemplateId()
        );
    }

    /**
     * Generate itinerary PDF (convenience endpoint)
     * Supports optional language parameter for translation
     *
     * @param itineraryId The obfuscated itinerary ID
     * @param templateId Optional template ID to use
     * @param language Optional target language code (e.g., "fr", "de", "es")
     *                 If not provided or "en", PDF is generated in English
     */
    @GetMapping("/itinerary/{itineraryId}")
    @PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
    public ResponseEntity<?> generateItineraryPdf(
        @PathVariable String itineraryId,
        @RequestParam(required = false) String templateId,
        @RequestParam(required = false) String language
    ) {
        log.info("GET /api/pdf/itinerary/{} - Generating itinerary PDF{}",
            itineraryId, language != null ? ", language: " + language : "");
        return generationService.generateItineraryPdf(itineraryId, templateId, language);
    }

    /**
     * Preview itinerary PDF (convenience endpoint)
     */
    @GetMapping("/itinerary/{itineraryId}/preview")
    @PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
    public ResponseEntity<ApiResponse<?>> previewItineraryPdf(
        @PathVariable String itineraryId,
        @RequestParam(required = false) String templateId
    ) {
        log.info("GET /api/pdf/itinerary/{}/preview - Preview itinerary", itineraryId);
        return generationService.previewPdf("FULL_ITINERARY", itineraryId, templateId);
    }

    /**
     * Validate HTML content for PDF generation compatibility
     *
     * This endpoint allows testing HTML content to ensure it's valid XHTML
     * that Flying Saucer can process for PDF generation.
     */
    @PostMapping("/validate")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PDF_TEMPLATE')")
    public ResponseEntity<ApiResponse<?>> validateTemplate(
        @Valid @RequestBody ValidateTemplateRequestDTO request
    ) {
        log.info("POST /api/pdf/validate - Validating template: {}", request.getTemplateName());

        var result = validationService.validate(request.getHtmlContent(), request.getTemplateName());

        Map<String, Object> response = new HashMap<>();
        response.put("valid", result.valid());
        response.put("message", result.message());
        response.put("templateName", request.getTemplateName());

        if (!result.valid()) {
            response.put("errorCount", result.errors().size());
            response.put("errors", result.errors().stream()
                    .map(error -> {
                        var detail = new HashMap<String, Object>();
                        detail.put("errorType", error.getErrorType());
                        detail.put("message", error.getMessage());
                        if (error.getLineNumber() != null) {
                            detail.put("lineNumber", error.getLineNumber());
                        }
                        if (error.getColumnNumber() != null) {
                            detail.put("columnNumber", error.getColumnNumber());
                        }
                        if (error.getSuggestion() != null) {
                            detail.put("suggestion", error.getSuggestion());
                        }
                        if (error.getContext() != null) {
                            detail.put("context", error.getContext());
                        }
                        return detail;
                    })
                    .toList());
        }

        String message = result.valid()
                ? "Template is valid for PDF generation"
                : "Template has validation errors";

        return ResponseEntity.ok(ApiResponse.success(200, message, response));
    }
}
