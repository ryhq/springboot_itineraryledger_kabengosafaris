package com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceDocumentDTOs;

import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceDocument.DocumentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for InvoiceDocument responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDocumentDTO {

    private String id;
    private String invoiceId;
    private String invoiceCode;

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
