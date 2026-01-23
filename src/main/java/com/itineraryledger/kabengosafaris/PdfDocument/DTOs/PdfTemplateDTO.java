package com.itineraryledger.kabengosafaris.PdfDocument.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.PdfDocument.Entity.Orientation;
import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PaperSize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for PdfTemplate entity responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PdfTemplateDTO {

    private String id;
    private String pdfDocumentId;
    private String pdfDocumentName;
    private String pdfDocumentDisplayName;
    private String name;
    private String description;
    private String fileName;
    private PaperSize paperSize;
    private String paperSizeDisplayName;
    private Orientation orientation;
    private String orientationDisplayName;
    private Integer marginTop;
    private Integer marginBottom;
    private Integer marginLeft;
    private Integer marginRight;
    private Boolean isDefault;
    private Boolean isSystemDefault;
    private Boolean enabled;
    private Long fileSize;
    private String fileSizeFormatted;
    private String version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Optional: template content (only included when specifically requested)
    private String content;
}
