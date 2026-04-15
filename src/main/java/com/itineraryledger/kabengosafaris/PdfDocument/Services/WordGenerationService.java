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
 * WordGenerationService - Facade for Word (.docx) generation services.
 *
 * Mirrors {@link PdfGenerationService}: routes by document type to the
 * specialized per-document Word generation service. All methods have both
 * a plain overload (uses global {@code docx.engine}) and an engine-aware
 * overload that forwards a per-request override.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WordGenerationService {

    private final ItineraryWordGenerationService itineraryWordService;
    private final QuoteWordGenerationService quoteWordService;
    private final SafariWordGenerationService safariWordService;
    private final InvoiceWordGenerationService invoiceWordService;

    // =====================================================================
    // GENERIC DOCX GENERATION (delegates to specialized services)
    // =====================================================================

    @Transactional(readOnly = true)
    public ResponseEntity<?> generateDocx(String documentName, String dataId, String templateIdObfuscated) {
        return generateDocx(documentName, dataId, templateIdObfuscated, null, null);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> generateDocx(String documentName, String dataId, String templateIdObfuscated, String language) {
        return generateDocx(documentName, dataId, templateIdObfuscated, language, null);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> generateDocx(String documentName, String dataId, String templateIdObfuscated, String language, String engine) {
        return switch (documentName) {
            case "FULL_ITINERARY" -> itineraryWordService.generateItineraryDocx(dataId, templateIdObfuscated, language, engine);
            case "FULL_QUOTE" -> quoteWordService.generateQuoteDocx(dataId, templateIdObfuscated, language, engine);
            case "FULL_SAFARI" -> safariWordService.generateSafariDocx(dataId, templateIdObfuscated, language, engine);
            case "FULL_INVOICE" -> invoiceWordService.generateInvoiceDocx(dataId, templateIdObfuscated, language, engine);
            default -> {
                log.warn("Unknown document type for DOCX: {}", documentName);
                yield ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Document type not supported for DOCX: " + documentName, "DOCUMENT_TYPE_NOT_FOUND")
                );
            }
        };
    }

    // =====================================================================
    // ITINERARY DOCX GENERATION
    // =====================================================================

    @Transactional(readOnly = true)
    public ResponseEntity<?> generateItineraryDocx(String itineraryIdObfuscated, String templateIdObfuscated, String language) {
        return itineraryWordService.generateItineraryDocx(itineraryIdObfuscated, templateIdObfuscated, language);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> generateItineraryDocx(String itineraryIdObfuscated, String templateIdObfuscated, String language, String engine) {
        return itineraryWordService.generateItineraryDocx(itineraryIdObfuscated, templateIdObfuscated, language, engine);
    }

    @Transactional
    public ResponseEntity<?> generateAndSaveItineraryDocx(
            String itineraryIdObfuscated,
            String templateIdObfuscated,
            String language,
            ItineraryDocument.DocumentType itineraryDocumentType,
            String title,
            String version,
            String notes
    ) {
        return generateAndSaveItineraryDocx(itineraryIdObfuscated, templateIdObfuscated, language,
            itineraryDocumentType, title, version, notes, null);
    }

    @Transactional
    public ResponseEntity<?> generateAndSaveItineraryDocx(
            String itineraryIdObfuscated,
            String templateIdObfuscated,
            String language,
            ItineraryDocument.DocumentType itineraryDocumentType,
            String title,
            String version,
            String notes,
            String engine
    ) {
        return itineraryWordService.generateAndSaveItineraryDocx(
            itineraryIdObfuscated, templateIdObfuscated, language,
            itineraryDocumentType, title, version, notes, engine
        );
    }

    // =====================================================================
    // QUOTE DOCX GENERATION
    // =====================================================================

    @Transactional(readOnly = true)
    public ResponseEntity<?> generateQuoteDocx(String quoteIdObfuscated, String templateIdObfuscated, String language) {
        return quoteWordService.generateQuoteDocx(quoteIdObfuscated, templateIdObfuscated, language);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> generateQuoteDocx(String quoteIdObfuscated, String templateIdObfuscated, String language, String engine) {
        return quoteWordService.generateQuoteDocx(quoteIdObfuscated, templateIdObfuscated, language, engine);
    }

    @Transactional
    public ResponseEntity<?> generateAndSaveQuoteDocx(
            String quoteIdObfuscated,
            String templateIdObfuscated,
            String language,
            QuoteDocument.DocumentType quoteDocumentType,
            String title,
            String version,
            String notes
    ) {
        return generateAndSaveQuoteDocx(quoteIdObfuscated, templateIdObfuscated, language,
            quoteDocumentType, title, version, notes, null);
    }

    @Transactional
    public ResponseEntity<?> generateAndSaveQuoteDocx(
            String quoteIdObfuscated,
            String templateIdObfuscated,
            String language,
            QuoteDocument.DocumentType quoteDocumentType,
            String title,
            String version,
            String notes,
            String engine
    ) {
        return quoteWordService.generateAndSaveQuoteDocx(
            quoteIdObfuscated, templateIdObfuscated, language,
            quoteDocumentType, title, version, notes, engine
        );
    }

    // =====================================================================
    // SAFARI DOCX GENERATION
    // =====================================================================

    @Transactional(readOnly = true)
    public ResponseEntity<?> generateSafariDocx(String safariIdObfuscated, String templateIdObfuscated, String language) {
        return safariWordService.generateSafariDocx(safariIdObfuscated, templateIdObfuscated, language);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> generateSafariDocx(String safariIdObfuscated, String templateIdObfuscated, String language, String engine) {
        return safariWordService.generateSafariDocx(safariIdObfuscated, templateIdObfuscated, language, engine);
    }

    @Transactional
    public ResponseEntity<?> generateAndSaveSafariDocx(
            String safariIdObfuscated,
            String templateIdObfuscated,
            String language,
            SafariDocument.DocumentType safariDocumentType,
            String title,
            String version,
            String notes
    ) {
        return generateAndSaveSafariDocx(safariIdObfuscated, templateIdObfuscated, language,
            safariDocumentType, title, version, notes, null);
    }

    @Transactional
    public ResponseEntity<?> generateAndSaveSafariDocx(
            String safariIdObfuscated,
            String templateIdObfuscated,
            String language,
            SafariDocument.DocumentType safariDocumentType,
            String title,
            String version,
            String notes,
            String engine
    ) {
        return safariWordService.generateAndSaveSafariDocx(
            safariIdObfuscated, templateIdObfuscated, language,
            safariDocumentType, title, version, notes, engine
        );
    }

    // =====================================================================
    // INVOICE DOCX GENERATION
    // =====================================================================

    @Transactional(readOnly = true)
    public ResponseEntity<?> generateInvoiceDocx(String invoiceIdObfuscated, String templateIdObfuscated, String language) {
        return invoiceWordService.generateInvoiceDocx(invoiceIdObfuscated, templateIdObfuscated, language);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> generateInvoiceDocx(String invoiceIdObfuscated, String templateIdObfuscated, String language, String engine) {
        return invoiceWordService.generateInvoiceDocx(invoiceIdObfuscated, templateIdObfuscated, language, engine);
    }

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
        return invoiceWordService.generateAndSaveInvoiceDocx(
            invoiceIdObfuscated, templateIdObfuscated, language,
            invoiceDocumentType, title, version, notes, engine
        );
    }
}
