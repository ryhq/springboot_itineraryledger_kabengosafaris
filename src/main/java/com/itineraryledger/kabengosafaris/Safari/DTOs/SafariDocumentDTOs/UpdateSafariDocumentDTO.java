package com.itineraryledger.kabengosafaris.Safari.DTOs.SafariDocumentDTOs;

import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.Safari.Entity.SafariDocument.DocumentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating SafariDocument metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSafariDocumentDTO {

    private String title;

    private DocumentType documentType;

    private String description;

    private String version;

    private String notes;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private Boolean isActive;
}
