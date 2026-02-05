package com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteDocumentDTOs;

import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteDocument.DocumentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO for creating QuoteDocument.
 * Contains the document file and metadata for upload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQuoteDocumentDTO {

    @NotNull(message = "Quote ID is required")
    private String quoteId;

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
