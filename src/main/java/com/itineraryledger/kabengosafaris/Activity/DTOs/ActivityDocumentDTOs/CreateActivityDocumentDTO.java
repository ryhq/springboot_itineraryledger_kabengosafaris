package com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityDocumentDTOs;

import java.time.LocalDateTime;

import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityDocument.DocumentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new ActivityDocument.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateActivityDocumentDTO {

    private String activityId;

    private MultipartFile document;

    private String title;

    private DocumentType documentType;

    private String description;

    private String version;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private String notes;
}
