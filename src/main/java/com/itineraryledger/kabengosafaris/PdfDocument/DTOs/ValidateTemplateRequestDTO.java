package com.itineraryledger.kabengosafaris.PdfDocument.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for requesting PDF template validation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidateTemplateRequestDTO {

    /**
     * The HTML content to validate
     */
    @NotBlank(message = "HTML content is required")
    private String htmlContent;

    /**
     * Optional template name for error reporting
     */
    private String templateName;
}
