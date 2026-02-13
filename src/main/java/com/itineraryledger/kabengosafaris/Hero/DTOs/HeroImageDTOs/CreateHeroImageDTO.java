package com.itineraryledger.kabengosafaris.Hero.DTOs.HeroImageDTOs;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating HeroImage
 * Contains the image file and metadata for upload
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateHeroImageDTO {

    @NotNull(message = "Hero ID is required")
    private String heroId;

    @NotNull(message = "Image file is required")
    private MultipartFile image;

    private String altText;

    private String caption;

    private String description;
}
