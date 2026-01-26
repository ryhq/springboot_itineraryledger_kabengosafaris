package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationDocumentDTOs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper DTO for uploading multiple accommodation documents.
 * Required because Spring can't directly bind @ModelAttribute to List interface.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadAccommodationDocumentsDTO {

    @NotEmpty(message = "At least one document is required")
    @Valid
    private List<CreateAccommodationDocumentDTO> documents;
}
