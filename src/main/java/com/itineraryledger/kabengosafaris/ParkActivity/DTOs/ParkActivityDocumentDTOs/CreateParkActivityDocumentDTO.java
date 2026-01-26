package com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityDocumentDTOs;

import java.time.LocalDateTime;

import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityDocument.DocumentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new ParkActivityDocument.
 * Both parkId and activityId are required to identify the park-activity relationship.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateParkActivityDocumentDTO {

    /**
     * Obfuscated park ID (required)
     */
    private String parkId;

    /**
     * Obfuscated activity ID (required)
     */
    private String activityId;

    /**
     * Document file (required)
     */
    private MultipartFile document;

    /**
     * Document title (required)
     */
    private String title;

    /**
     * Type of document (required)
     */
    private DocumentType documentType;

    /**
     * Detailed description (optional)
     */
    private String description;

    /**
     * Document version (optional)
     */
    private String version;

    /**
     * Validity start date (optional)
     */
    private LocalDateTime validFrom;

    /**
     * Validity end date (optional)
     */
    private LocalDateTime validTo;

    /**
     * Additional notes (optional)
     */
    private String notes;
}
