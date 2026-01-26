package com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryDocumentDTOs;

import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.Itinerary.Entity.ItineraryDocument.DocumentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating ItineraryDocument metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateItineraryDocumentDTO {

    private String title;

    private DocumentType documentType;

    private String description;

    private String version;

    private String notes;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private Boolean isActive;
}
