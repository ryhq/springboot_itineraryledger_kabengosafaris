package com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteDocumentDTOs;

import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteDocument.DocumentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating QuoteDocument metadata.
 * Note: To replace the actual document file, a new upload is required.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateQuoteDocumentDTO {

    private String title;
    private DocumentType documentType;
    private String description;
    private String version;
    private String notes;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Boolean isActive;
}
