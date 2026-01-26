package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationDocumentDTOs;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationDocument.DocumentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for AccommodationDocument responses.
 * Constructs the full document URL from the stored filename and configured base URL.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccommodationDocumentDTO {

    private String id;
    private String accommodationId;
    private String accommodationName;

    private String title;

    private DocumentType documentType;
    private String documentTypeDisplayName;
    private String documentTypeDescription;

    /**
     * Full URL to the document using obfuscated ID
     * Example: "http://localhost:4450/api/accommodation-documents/{id}/file"
     */
    private String documentUrl;

    /**
     * Full URL to the document using filename
     * Example: "http://localhost:4450/api/accommodation-documents/file/{fileName}"
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

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
