package com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerDocumentDTOs;

import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerDocument.DocumentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating CustomerDocument metadata.
 * Note: To replace the actual document file, a new upload is required.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCustomerDocumentDTO {

    private String title;
    private DocumentType documentType;
    private String description;
    private String documentNumber;
    private String version;
    private String notes;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Boolean isActive;
}
