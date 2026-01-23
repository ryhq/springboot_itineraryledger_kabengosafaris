package com.itineraryledger.kabengosafaris.PdfDocument.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for PdfDocument entity responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PdfDocumentDTO {

    private String id;
    private String name;
    private String displayName;
    private String description;
    private String dataSourceClass;
    private String rootVariableName;
    private Boolean enabled;
    private String variablesJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Template count for this document type
    private Integer templateCount;
}
