package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.Itinerary.Entity.ItineraryDocument;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceDocument;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteDocument;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.Entity.SafariDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PdfGenerationService - Facade for PDF generation services
 *
 * Delegates PDF generation requests to specialized services:
 * - ItineraryPdfGenerationService
 * - QuotePdfGenerationService
 * - SafariPdfGenerationService
 * - InvoicePdfGenerationService
 *
 * This service maintains backward compatibility with existing controllers
 * while using the new modular architecture.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfGenerationService {

    private final ItineraryPdfGenerationService itineraryPdfService;
    private final QuotePdfGenerationService quotePdfService;
    private final SafariPdfGenerationService safariPdfService;
    private final InvoicePdfGenerationService invoicePdfService;

    // =====================================================================
    // GENERIC PDF GENERATION (delegates to specialized services)
    // =====================================================================

    /**
     * Generate PDF for a specific document type and data ID
     *
     * @param documentName The PDF document type name (e.g., "FULL_ITINERARY")
     * @param dataId The ID of the data source
     * @param templateIdObfuscated Optional specific template ID
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
     * @param dataId The ID of the data source
     * @param templateIdObfuscated Optional specific template ID
     * @param language Optional target language code for translation
     * @return ResponseEntity with PDF bytes or error
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> generatePdf(String documentName, String dataId, String templateIdObfuscated, String language) {
        return switch (documentName) {
            case "FULL_ITINERARY" -> itineraryPdfService.generateItineraryPdf(dataId, templateIdObfuscated, language);
            case "FULL_QUOTE" -> quotePdfService.generateQuotePdf(dataId, templateIdObfuscated, language);
            case "FULL_SAFARI" -> safariPdfService.generateSafariPdf(dataId, templateIdObfuscated, language);
            case "FULL_INVOICE" -> invoicePdfService.generateInvoicePdf(dataId, templateIdObfuscated, language);
            default -> {
                log.warn("Unknown document type: {}", documentName);
                yield ResponseEntity.status(404).body(
                    ApiResponse.error(404, "PDF document type not found: " + documentName, "DOCUMENT_TYPE_NOT_FOUND")
                );
            }
        };
    }

    /**
     * Preview PDF (return rendered HTML instead of PDF)
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> previewPdf(String documentName, String dataId, String templateIdObfuscated) {
        return previewPdf(documentName, dataId, templateIdObfuscated, null);
    }

    /**
     * Preview PDF with optional translation (return rendered HTML instead of PDF)
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> previewPdf(String documentName, String dataId, String templateIdObfuscated, String language) {
        return switch (documentName) {
            case "FULL_ITINERARY" -> itineraryPdfService.previewItineraryPdf(dataId, templateIdObfuscated, language);
            case "FULL_QUOTE" -> quotePdfService.previewQuotePdf(dataId, templateIdObfuscated, language);
            case "FULL_SAFARI" -> safariPdfService.previewSafariPdf(dataId, templateIdObfuscated, language);
            case "FULL_INVOICE" -> invoicePdfService.previewInvoicePdf(dataId, templateIdObfuscated, language);
            default -> {
                log.warn("Unknown document type: {}", documentName);
                yield ResponseEntity.status(404).body(
                    ApiResponse.error(404, "PDF document type not found: " + documentName, "DOCUMENT_TYPE_NOT_FOUND")
                );
            }
        };
    }

    // =====================================================================
    // ITINERARY PDF GENERATION
    // =====================================================================

    @Transactional(readOnly = true)
    public ResponseEntity<?> generateItineraryPdf(String itineraryIdObfuscated, String templateIdObfuscated, String language) {
        return itineraryPdfService.generateItineraryPdf(itineraryIdObfuscated, templateIdObfuscated, language);
    }

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
        return itineraryPdfService.generateAndSaveItineraryPdf(
            itineraryIdObfuscated, templateIdObfuscated, language,
            itineraryDocumentType, title, version, notes
        );
    }

    // =====================================================================
    // QUOTE PDF GENERATION
    // =====================================================================

    @Transactional(readOnly = true)
    public ResponseEntity<?> generateQuotePdf(String quoteIdObfuscated, String templateIdObfuscated, String language) {
        return quotePdfService.generateQuotePdf(quoteIdObfuscated, templateIdObfuscated, language);
    }

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
        return quotePdfService.generateAndSaveQuotePdf(
            quoteIdObfuscated, templateIdObfuscated, language,
            quoteDocumentType, title, version, notes
        );
    }

    // =====================================================================
    // SAFARI PDF GENERATION
    // =====================================================================

    @Transactional(readOnly = true)
    public ResponseEntity<?> generateSafariPdf(String safariIdObfuscated, String templateIdObfuscated, String language) {
        return safariPdfService.generateSafariPdf(safariIdObfuscated, templateIdObfuscated, language);
    }

    @Transactional
    public ResponseEntity<?> generateAndSaveSafariPdf(
            String safariIdObfuscated,
            String templateIdObfuscated,
            String language,
            SafariDocument.DocumentType safariDocumentType,
            String title,
            String version,
            String notes
    ) {
        return safariPdfService.generateAndSaveSafariPdf(
            safariIdObfuscated, templateIdObfuscated, language,
            safariDocumentType, title, version, notes
        );
    }

    // =====================================================================
    // INVOICE PDF GENERATION
    // =====================================================================

    @Transactional(readOnly = true)
    public ResponseEntity<?> generateInvoicePdf(String invoiceIdObfuscated, String templateIdObfuscated, String language) {
        return invoicePdfService.generateInvoicePdf(invoiceIdObfuscated, templateIdObfuscated, language);
    }

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
        return invoicePdfService.generateAndSaveInvoicePdf(
            invoiceIdObfuscated, templateIdObfuscated, language,
            invoiceDocumentType, title, version, notes
        );
    }
}
