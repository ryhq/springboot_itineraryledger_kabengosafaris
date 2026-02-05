package com.itineraryledger.kabengosafaris.PdfDocument.Controller;

import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceDocument;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.ItineraryDocument;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteDocument;
import com.itineraryledger.kabengosafaris.Safari.Entity.SafariDocument;
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
     * Supports optional saveToDocuments to save the PDF as an ItineraryDocument or QuoteDocument
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
    public ResponseEntity<?> generatePdf(@Valid @RequestBody GeneratePdfRequestDTO request) {
        log.info("POST /api/pdf/generate - Generating PDF for {} / {}{}{}",
            request.getDocumentType(), request.getDataId(),
            request.getLanguage() != null ? ", language: " + request.getLanguage() : "",
            Boolean.TRUE.equals(request.getSaveToDocuments()) ? ", saving to documents" : "");

        // If saveToDocuments is true and this is an itinerary-related PDF, use the save method
        if (Boolean.TRUE.equals(request.getSaveToDocuments()) && "FULL_ITINERARY".equals(request.getDocumentType())) {
            return generationService.generateAndSaveItineraryPdf(
                request.getDataId(),
                request.getTemplateId(),
                request.getLanguage(),
                request.getItineraryDocumentType(),
                request.getDocumentTitle(),
                request.getDocumentVersion(),
                request.getDocumentNotes()
            );
        }

        // If saveToDocuments is true and this is a quote-related PDF, use the save method
        if (Boolean.TRUE.equals(request.getSaveToDocuments()) && "FULL_QUOTE".equals(request.getDocumentType())) {
            return generationService.generateAndSaveQuotePdf(
                request.getDataId(),
                request.getTemplateId(),
                request.getLanguage(),
                request.getQuoteDocumentType(),
                request.getDocumentTitle(),
                request.getDocumentVersion(),
                request.getDocumentNotes()
            );
        }

        // If saveToDocuments is true and this is a safari-related PDF, use the save method
        if (Boolean.TRUE.equals(request.getSaveToDocuments()) && "FULL_SAFARI".equals(request.getDocumentType())) {
            return generationService.generateAndSaveSafariPdf(
                request.getDataId(),
                request.getTemplateId(),
                request.getLanguage(),
                request.getSafariDocumentType(),
                request.getDocumentTitle(),
                request.getDocumentVersion(),
                request.getDocumentNotes()
            );
        }

        // If saveToDocuments is true and this is an invoice-related PDF, use the save method
        if (Boolean.TRUE.equals(request.getSaveToDocuments()) && "FULL_INVOICE".equals(request.getDocumentType())) {
            return generationService.generateAndSaveInvoicePdf(
                request.getDataId(),
                request.getTemplateId(),
                request.getLanguage(),
                request.getInvoiceDocumentType(),
                request.getDocumentTitle(),
                request.getDocumentVersion(),
                request.getDocumentNotes()
            );
        }

        return generationService.generatePdf(
            request.getDocumentType(),
            request.getDataId(),
            request.getTemplateId(),
            request.getLanguage()
        );
    }

    /**
     * Preview PDF (returns rendered HTML)
     * Supports optional language parameter for translation preview
     */
    @PostMapping("/preview")
    @PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
    public ResponseEntity<ApiResponse<?>> previewPdf(@Valid @RequestBody GeneratePdfRequestDTO request) {
        log.info("POST /api/pdf/preview - Preview for {} / {}{}",
            request.getDocumentType(), request.getDataId(),
            request.getLanguage() != null ? ", language: " + request.getLanguage() : "");
        return generationService.previewPdf(
            request.getDocumentType(),
            request.getDataId(),
            request.getTemplateId(),
            request.getLanguage()
        );
    }

    /**
     * Generate itinerary PDF (convenience endpoint)
     * Supports optional language parameter for translation
     * Supports optional saveToDocuments to save the PDF as an ItineraryDocument
     *
     * @param itineraryId The obfuscated itinerary ID
     * @param templateId Optional template ID to use
     * @param language Optional target language code (e.g., "fr", "de", "es")
     *                 If not provided or "en", PDF is generated in English
     * @param saveToDocuments If true, save the PDF as an ItineraryDocument
     * @param documentType The ItineraryDocument type (e.g., QUOTATION, FINAL_ITINERARY)
     * @param documentTitle Optional title for the saved document
     * @param documentVersion Optional version string
     * @param documentNotes Optional notes
     */
    @GetMapping("/itinerary/{itineraryId}")
    @PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
    public ResponseEntity<?> generateItineraryPdf(
        @PathVariable String itineraryId,
        @RequestParam(required = false) String templateId,
        @RequestParam(required = false) String language,
        @RequestParam(required = false, defaultValue = "false") Boolean saveToDocuments,
        @RequestParam(required = false) ItineraryDocument.DocumentType documentType,
        @RequestParam(required = false) String documentTitle,
        @RequestParam(required = false) String documentVersion,
        @RequestParam(required = false) String documentNotes
    ) {
        log.info("GET /api/pdf/itinerary/{} - Generating itinerary PDF{}{}",
            itineraryId,
            language != null ? ", language: " + language : "",
            Boolean.TRUE.equals(saveToDocuments) ? ", saving to documents" : "");

        if (Boolean.TRUE.equals(saveToDocuments)) {
            return generationService.generateAndSaveItineraryPdf(
                itineraryId,
                templateId,
                language,
                documentType,
                documentTitle,
                documentVersion,
                documentNotes
            );
        }

        return generationService.generateItineraryPdf(itineraryId, templateId, language);
    }

    /**
     * Preview itinerary PDF (convenience endpoint)
     * Supports optional language parameter for translation preview
     *
     * @param itineraryId The obfuscated itinerary ID
     * @param templateId Optional template ID to use
     * @param language Optional target language code (e.g., "fr", "de", "es")
     *                 If not provided or "en", preview is in English
     */
    @GetMapping("/itinerary/{itineraryId}/preview")
    @PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
    public ResponseEntity<ApiResponse<?>> previewItineraryPdf(
        @PathVariable String itineraryId,
        @RequestParam(required = false) String templateId,
        @RequestParam(required = false) String language
    ) {
        log.info("GET /api/pdf/itinerary/{}/preview - Preview itinerary{}",
            itineraryId, language != null ? ", language: " + language : "");
        return generationService.previewPdf("FULL_ITINERARY", itineraryId, templateId, language);
    }

    /**
     * Generate quote PDF (convenience endpoint)
     * Supports optional language parameter for translation
     * Supports optional saveToDocuments to save the PDF as a QuoteDocument
     *
     * @param quoteId The obfuscated quote ID
     * @param templateId Optional template ID to use
     * @param language Optional target language code (e.g., "fr", "de", "es")
     *                 If not provided or "en", PDF is generated in English
     * @param saveToDocuments If true, save the PDF as a QuoteDocument
     * @param documentType The QuoteDocument type (e.g., QUOTATION, PROPOSAL, FINAL_QUOTE)
     * @param documentTitle Optional title for the saved document
     * @param documentVersion Optional version string
     * @param documentNotes Optional notes
     */
    @GetMapping("/quote/{quoteId}")
    @PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
    public ResponseEntity<?> generateQuotePdf(
        @PathVariable String quoteId,
        @RequestParam(required = false) String templateId,
        @RequestParam(required = false) String language,
        @RequestParam(required = false, defaultValue = "false") Boolean saveToDocuments,
        @RequestParam(required = false) QuoteDocument.DocumentType documentType,
        @RequestParam(required = false) String documentTitle,
        @RequestParam(required = false) String documentVersion,
        @RequestParam(required = false) String documentNotes
    ) {
        log.info("GET /api/pdf/quote/{} - Generating quote PDF{}{}",
            quoteId,
            language != null ? ", language: " + language : "",
            Boolean.TRUE.equals(saveToDocuments) ? ", saving to documents" : "");

        if (Boolean.TRUE.equals(saveToDocuments)) {
            return generationService.generateAndSaveQuotePdf(
                quoteId,
                templateId,
                language,
                documentType,
                documentTitle,
                documentVersion,
                documentNotes
            );
        }

        return generationService.generateQuotePdf(quoteId, templateId, language);
    }

    /**
     * Preview quote PDF (convenience endpoint)
     * Supports optional language parameter for translation preview
     *
     * @param quoteId The obfuscated quote ID
     * @param templateId Optional template ID to use
     * @param language Optional target language code (e.g., "fr", "de", "es")
     *                 If not provided or "en", preview is in English
     */
    @GetMapping("/quote/{quoteId}/preview")
    @PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
    public ResponseEntity<ApiResponse<?>> previewQuotePdf(
        @PathVariable String quoteId,
        @RequestParam(required = false) String templateId,
        @RequestParam(required = false) String language
    ) {
        log.info("GET /api/pdf/quote/{}/preview - Preview quote{}",
            quoteId, language != null ? ", language: " + language : "");
        return generationService.previewPdf("FULL_QUOTE", quoteId, templateId, language);
    }

    /**
     * Generate safari PDF (convenience endpoint)
     * Supports optional language parameter for translation
     * Supports optional saveToDocuments to save the PDF as a SafariDocument
     *
     * @param safariId The obfuscated safari ID
     * @param templateId Optional template ID to use
     * @param language Optional target language code (e.g., "fr", "de", "es")
     *                 If not provided or "en", PDF is generated in English
     * @param saveToDocuments If true, save the PDF as a SafariDocument
     * @param documentType The SafariDocument type (e.g., QUOTATION, FINAL_ITINERARY, TRAVEL_PLAN)
     * @param documentTitle Optional title for the saved document
     * @param documentVersion Optional version string
     * @param documentNotes Optional notes
     */
    @GetMapping("/safari/{safariId}")
    @PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
    public ResponseEntity<?> generateSafariPdf(
        @PathVariable String safariId,
        @RequestParam(required = false) String templateId,
        @RequestParam(required = false) String language,
        @RequestParam(required = false, defaultValue = "false") Boolean saveToDocuments,
        @RequestParam(required = false) SafariDocument.DocumentType documentType,
        @RequestParam(required = false) String documentTitle,
        @RequestParam(required = false) String documentVersion,
        @RequestParam(required = false) String documentNotes
    ) {
        log.info("GET /api/pdf/safari/{} - Generating safari PDF{}{}",
            safariId,
            language != null ? ", language: " + language : "",
            Boolean.TRUE.equals(saveToDocuments) ? ", saving to documents" : "");

        if (Boolean.TRUE.equals(saveToDocuments)) {
            return generationService.generateAndSaveSafariPdf(
                safariId,
                templateId,
                language,
                documentType,
                documentTitle,
                documentVersion,
                documentNotes
            );
        }

        return generationService.generateSafariPdf(safariId, templateId, language);
    }

    /**
     * Preview safari PDF (convenience endpoint)
     * Supports optional language parameter for translation preview
     *
     * @param safariId The obfuscated safari ID
     * @param templateId Optional template ID to use
     * @param language Optional target language code (e.g., "fr", "de", "es")
     *                 If not provided or "en", preview is in English
     */
    @GetMapping("/safari/{safariId}/preview")
    @PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
    public ResponseEntity<ApiResponse<?>> previewSafariPdf(
        @PathVariable String safariId,
        @RequestParam(required = false) String templateId,
        @RequestParam(required = false) String language
    ) {
        log.info("GET /api/pdf/safari/{}/preview - Preview safari{}",
            safariId, language != null ? ", language: " + language : "");
        return generationService.previewPdf("FULL_SAFARI", safariId, templateId, language);
    }

    /**
     * Generate invoice PDF (convenience endpoint)
     * Supports optional language parameter for translation
     * Supports optional saveToDocuments to save the PDF as an InvoiceDocument
     *
     * @param invoiceId The obfuscated invoice ID
     * @param templateId Optional template ID to use
     * @param language Optional target language code (e.g., "fr", "de", "es")
     *                 If not provided or "en", PDF is generated in English
     * @param saveToDocuments If true, save the PDF as an InvoiceDocument
     * @param documentType The InvoiceDocument type (e.g., INVOICE_PDF, TAX_INVOICE, PROFORMA_INVOICE)
     * @param documentTitle Optional title for the saved document
     * @param documentVersion Optional version string
     * @param documentNotes Optional notes
     */
    @GetMapping("/invoice/{invoiceId}")
    @PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
    public ResponseEntity<?> generateInvoicePdf(
        @PathVariable String invoiceId,
        @RequestParam(required = false) String templateId,
        @RequestParam(required = false) String language,
        @RequestParam(required = false, defaultValue = "false") Boolean saveToDocuments,
        @RequestParam(required = false) InvoiceDocument.DocumentType documentType,
        @RequestParam(required = false) String documentTitle,
        @RequestParam(required = false) String documentVersion,
        @RequestParam(required = false) String documentNotes
    ) {
        log.info("GET /api/pdf/invoice/{} - Generating invoice PDF{}{}",
            invoiceId,
            language != null ? ", language: " + language : "",
            Boolean.TRUE.equals(saveToDocuments) ? ", saving to documents" : "");

        if (Boolean.TRUE.equals(saveToDocuments)) {
            return generationService.generateAndSaveInvoicePdf(
                invoiceId,
                templateId,
                language,
                documentType,
                documentTitle,
                documentVersion,
                documentNotes
            );
        }

        return generationService.generateInvoicePdf(invoiceId, templateId, language);
    }

    /**
     * Preview invoice PDF (convenience endpoint)
     * Supports optional language parameter for translation preview
     *
     * @param invoiceId The obfuscated invoice ID
     * @param templateId Optional template ID to use
     * @param language Optional target language code (e.g., "fr", "de", "es")
     *                 If not provided or "en", preview is in English
     */
    @GetMapping("/invoice/{invoiceId}/preview")
    @PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
    public ResponseEntity<ApiResponse<?>> previewInvoicePdf(
        @PathVariable String invoiceId,
        @RequestParam(required = false) String templateId,
        @RequestParam(required = false) String language
    ) {
        log.info("GET /api/pdf/invoice/{}/preview - Preview invoice{}",
            invoiceId, language != null ? ", language: " + language : "");
        return generationService.previewPdf("FULL_INVOICE", invoiceId, templateId, language);
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
