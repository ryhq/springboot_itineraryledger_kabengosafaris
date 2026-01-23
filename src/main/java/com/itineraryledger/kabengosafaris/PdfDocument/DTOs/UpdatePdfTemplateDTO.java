package com.itineraryledger.kabengosafaris.PdfDocument.DTOs;

import com.itineraryledger.kabengosafaris.PdfDocument.Entity.Orientation;
import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PaperSize;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating an existing PDF template
 * All fields are optional - only provided fields will be updated
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePdfTemplateDTO {

    /**
     * Updated template name
     */
    @Size(min = 2, max = 200, message = "Template name must be between 2 and 200 characters")
    private String name;

    /**
     * Updated description
     */
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    /**
     * Updated HTML content
     */
    private String content;

    /**
     * Updated paper size
     */
    private PaperSize paperSize;

    /**
     * Updated page orientation
     */
    private Orientation orientation;

    /**
     * Updated top margin in mm
     */
    private Integer marginTop;

    /**
     * Updated bottom margin in mm
     */
    private Integer marginBottom;

    /**
     * Updated left margin in mm
     */
    private Integer marginLeft;

    /**
     * Updated right margin in mm
     */
    private Integer marginRight;

    /**
     * Set as default template
     */
    private Boolean isDefault;

    /**
     * Enable/disable template
     */
    private Boolean enabled;

    /**
     * Updated version
     */
    @Size(max = 50, message = "Version cannot exceed 50 characters")
    private String version;
}
