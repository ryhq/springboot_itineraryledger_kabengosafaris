package com.itineraryledger.kabengosafaris.Park.DTOs.ParkImageDTOs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper DTO for uploading multiple park images.
 * Required because Spring can't directly bind @ModelAttribute to List interface.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadParkImagesDTO {

    @NotEmpty(message = "At least one image is required")
    @Valid
    private List<CreateParkImageDTO> images;
}
