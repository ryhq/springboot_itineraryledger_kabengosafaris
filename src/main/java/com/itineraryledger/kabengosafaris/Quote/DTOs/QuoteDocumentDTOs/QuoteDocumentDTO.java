package com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteDocumentDTOs;

import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteDocument.DocumentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for QuoteDocument responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteDocumentDTO {

    private String id;
    private String quoteId;
    private String quoteCode;

    private String title;
    private DocumentType documentType;
    private String documentTypeDisplayName;
    private String documentTypeDescription;

    private String documentUrl;
    private String fileDocumentUrl;
    private String fileName;
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
