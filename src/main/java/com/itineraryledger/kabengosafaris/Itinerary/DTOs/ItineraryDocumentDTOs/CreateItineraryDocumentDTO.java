package com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryDocumentDTOs;

import java.time.LocalDateTime;

import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.Itinerary.Entity.ItineraryDocument.DocumentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new ItineraryDocument.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateItineraryDocumentDTO {

    private String itineraryId;

    private MultipartFile document;

    private String title;

    private DocumentType documentType;

    private String description;

    private String version;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private String notes;
}
