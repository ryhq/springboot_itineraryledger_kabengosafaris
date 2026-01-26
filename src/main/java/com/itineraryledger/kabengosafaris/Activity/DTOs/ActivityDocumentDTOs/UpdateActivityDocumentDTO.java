package com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityDocumentDTOs;

import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityDocument.DocumentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating ActivityDocument metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateActivityDocumentDTO {

    private String title;

    private DocumentType documentType;

    private String description;

    private String version;

    private String notes;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private Boolean isActive;
}
