package com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.DTOs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper DTO for uploading multiple itinerary images.
 * Required because Spring can't directly bind @ModelAttribute to a List interface.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadItineraryImagesDTO {

    @NotEmpty(message = "At least one image is required")
    @Valid
    private List<CreateItineraryImageDTO> images;
}
