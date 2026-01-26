package com.itineraryledger.kabengosafaris.Park.DTOs.ParkDocumentDTOs;

import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.Park.Entities.ParkDocument.DocumentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating ParkDocument metadata.
 * Note: To replace the actual document file, a new upload is required.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateParkDocumentDTO {

    private String title;
    private DocumentType documentType;
    private String description;
    private String version;
    private String notes;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Boolean isActive;
}
