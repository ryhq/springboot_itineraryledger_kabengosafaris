package com.itineraryledger.kabengosafaris.PdfDocument.DTOs;

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
}
