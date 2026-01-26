package com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryDocumentDTOs;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.ItineraryDocument.DocumentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for ItineraryDocument responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItineraryDocumentDTO {

    private String id;
    private String itineraryId;
    private String itineraryName;
    private String itineraryCode;

    private String title;

    private DocumentType documentType;
    private String documentTypeDisplayName;
    private String documentTypeDescription;

    /**
     * Full URL to the document using obfuscated ID
     */
    private String documentUrl;

    /**
     * Full URL to the document using filename
     */
    private String fileDocumentUrl;

    /**
     * Stored filename only (for reference)
     */
    private String fileName;

    /**
     * Original filename uploaded by user
     */
    private String originalFileName;

    private Long fileSize;
    private String fileSizeFormatted;
    private String fileType;

    private String description;
    private String version;
    private String notes;

    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Boolean isCurrentlyValid;

    private Boolean isActive;
    private Boolean isGenerated;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
