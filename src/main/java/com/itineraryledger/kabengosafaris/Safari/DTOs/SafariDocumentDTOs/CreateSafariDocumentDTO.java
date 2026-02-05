package com.itineraryledger.kabengosafaris.Safari.DTOs.SafariDocumentDTOs;

import java.time.LocalDateTime;

import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.Safari.Entity.SafariDocument.DocumentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new SafariDocument.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSafariDocumentDTO {

    private String safariId;

    private MultipartFile document;

    private String title;

    private DocumentType documentType;

    private String description;

    private String version;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private String notes;
}
