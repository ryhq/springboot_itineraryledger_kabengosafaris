package com.itineraryledger.kabengosafaris.Park.DTOs.ParkDocumentDTOs;

import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.Park.Entities.ParkDocument.DocumentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO for creating ParkDocument.
 * Contains the document file and metadata for upload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateParkDocumentDTO {

    @NotNull(message = "Park ID is required")
    private String parkId;

    @NotNull(message = "Document file is required")
    private MultipartFile document;

    @NotBlank(message = "Document title is required")
    private String title;

    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    private String description;

    private String version;

    private String notes;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;
}
