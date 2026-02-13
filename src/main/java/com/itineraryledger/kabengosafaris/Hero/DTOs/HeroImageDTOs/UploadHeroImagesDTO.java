package com.itineraryledger.kabengosafaris.Hero.DTOs.HeroImageDTOs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper DTO for uploading multiple hero images.
 * Required because Spring can't directly bind @ModelAttribute to List interface.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadHeroImagesDTO {

    @NotEmpty(message = "At least one image is required")
    @Valid
    private List<CreateHeroImageDTO> images;
}
