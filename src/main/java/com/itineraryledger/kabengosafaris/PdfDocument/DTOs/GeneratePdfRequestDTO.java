package com.itineraryledger.kabengosafaris.PdfDocument.DTOs;

import com.itineraryledger.kabengosafaris.Itinerary.Entity.ItineraryDocument;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteDocument;
import com.itineraryledger.kabengosafaris.Safari.Entity.SafariDocument;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for requesting PDF generation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratePdfRequestDTO {

    /**
     * The PDF document type name (e.g., "FULL_ITINERARY")
     */
    @NotBlank(message = "Document type is required")
    private String documentType;

    /**
     * The ID of the data source (e.g., itinerary ID)
     */
    @NotBlank(message = "Data ID is required")
    private String dataId;

    /**
     * Optional: specific template ID to use
     * If not provided, the default template for the document type will be used
     */
    private String templateId;

    /**
     * Optional: custom file name for the generated PDF
     */
    private String fileName;

    /**
     * Optional: target language code for translation (e.g., "fr", "de", "es")
     * If not provided or set to "en", the PDF will be generated in English.
     * Supported languages depend on the translation settings configuration.
     */
    private String language;

    /**
     * Optional: if true, the generated PDF will be saved as an ItineraryDocument.
     * Only applicable for itinerary-related PDFs (FULL_ITINERARY, etc.)
     */
    @Builder.Default
    private Boolean saveToDocuments = false;

    /**
     * Optional: the ItineraryDocument type to use when saving.
     * Required if saveToDocuments is true and documentType is FULL_ITINERARY.
     * Examples: QUOTATION, FINAL_ITINERARY, TRAVEL_PLAN, INVOICE, etc.
     */
    private ItineraryDocument.DocumentType itineraryDocumentType;

    /**
     * Optional: the QuoteDocument type to use when saving.
     * Required if saveToDocuments is true and documentType is FULL_QUOTE.
     * Examples: QUOTATION, PROPOSAL, FINAL_QUOTE, INVOICE, etc.
     */
    private QuoteDocument.DocumentType quoteDocumentType;

    /**
     * Optional: the SafariDocument type to use when saving.
     * Required if saveToDocuments is true and documentType is FULL_SAFARI.
     * Examples: QUOTATION, TRAVEL_PLAN, FINAL_ITINERARY, BOOKING_CONFIRMATION, etc.
     */
    private SafariDocument.DocumentType safariDocumentType;

    /**
     * Optional: title for the saved document.
     * If not provided, a default title will be generated.
     */
    private String documentTitle;

    /**
     * Optional: version string for the saved document.
     */
    private String documentVersion;

    /**
     * Optional: notes for the saved document.
     */
    private String documentNotes;
}
