package com.itineraryledger.kabengosafaris.Park.DTOs.ParkDocumentDTOs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper DTO for uploading multiple park documents.
 * Required because Spring can't directly bind @ModelAttribute to List interface.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadParkDocumentsDTO {

    @NotEmpty(message = "At least one document is required")
    @Valid
    private List<CreateParkDocumentDTO> documents;
}
