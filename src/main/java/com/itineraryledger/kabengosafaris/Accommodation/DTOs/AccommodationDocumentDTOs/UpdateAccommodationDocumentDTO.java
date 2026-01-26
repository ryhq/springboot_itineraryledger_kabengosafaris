package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationDocumentDTOs;

import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationDocument.DocumentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating AccommodationDocument metadata.
 * Note: To replace the actual document file, a new upload is required.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAccommodationDocumentDTO {

    private String title;
    private DocumentType documentType;
    private String description;
    private String version;
    private String notes;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Boolean isActive;
}
