package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationImageDTOs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper DTO for uploading multiple accommodation images.
 * Required because Spring can't directly bind @ModelAttribute to List interface.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadAccommodationImagesDTO {

    @NotEmpty(message = "At least one image is required")
    @Valid
    private List<CreateAccommodationImageDTO> images;
}
