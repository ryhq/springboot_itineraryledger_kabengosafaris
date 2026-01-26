package com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityDocumentDTOs;

import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityDocument.DocumentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating ParkActivityDocument metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateParkActivityDocumentDTO {

    private String title;

    private DocumentType documentType;

    private String description;

    private String version;

    private String notes;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private Boolean isActive;
}
