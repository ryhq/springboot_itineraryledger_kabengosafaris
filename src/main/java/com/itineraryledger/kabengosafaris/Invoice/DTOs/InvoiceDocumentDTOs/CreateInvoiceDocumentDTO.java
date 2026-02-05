package com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceDocumentDTOs;

import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceDocument.DocumentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO for creating InvoiceDocument.
 * Contains the document file and metadata for upload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInvoiceDocumentDTO {

    @NotNull(message = "Invoice ID is required")
    private String invoiceId;

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
