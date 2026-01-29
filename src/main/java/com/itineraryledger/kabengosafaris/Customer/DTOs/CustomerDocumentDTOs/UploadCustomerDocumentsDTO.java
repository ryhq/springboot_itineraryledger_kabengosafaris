package com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerDocumentDTOs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper DTO for uploading multiple customer documents.
 * Required because Spring can't directly bind @ModelAttribute to List interface.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadCustomerDocumentsDTO {

    @NotEmpty(message = "At least one document is required")
    @Valid
    private List<CreateCustomerDocumentDTO> documents;
}
