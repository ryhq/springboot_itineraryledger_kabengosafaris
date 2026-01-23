package com.itineraryledger.kabengosafaris.PdfDocument.DTOs;

import com.itineraryledger.kabengosafaris.PdfDocument.Entity.Orientation;
import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PaperSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new PDF template
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePdfTemplateDTO {

    /**
     * Name of the template (must be unique per document type)
     */
    @NotBlank(message = "Template name is required")
    @Size(min = 2, max = 200, message = "Template name must be between 2 and 200 characters")
    private String name;

    /**
     * Description of the template
     */
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    /**
     * HTML content of the template (Thymeleaf syntax)
     */
    @NotBlank(message = "Template content is required")
    private String content;

    /**
     * Paper size (default: A4)
     */
    private PaperSize paperSize;

    /**
     * Page orientation (default: PORTRAIT)
     */
    private Orientation orientation;

    /**
     * Top margin in mm (default: 20)
     */
    private Integer marginTop;

    /**
     * Bottom margin in mm (default: 20)
     */
    private Integer marginBottom;

    /**
     * Left margin in mm (default: 15)
     */
    private Integer marginLeft;

    /**
     * Right margin in mm (default: 15)
     */
    private Integer marginRight;

    /**
     * Whether this should be the default template
     */
    private Boolean isDefault;

    /**
     * Whether this template is enabled
     */
    private Boolean enabled;

    /**
     * Template version
     */
    @Size(max = 50, message = "Version cannot exceed 50 characters")
    private String version;
}
