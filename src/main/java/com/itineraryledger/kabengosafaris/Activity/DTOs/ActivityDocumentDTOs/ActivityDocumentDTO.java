package com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityDocumentDTOs;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityDocument.DocumentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for ActivityDocument responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActivityDocumentDTO {

    private String id;
    private String activityId;
    private String activityName;

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

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
