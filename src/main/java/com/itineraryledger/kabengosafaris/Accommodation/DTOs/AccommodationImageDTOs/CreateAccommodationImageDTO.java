package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationImageDTOs;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage.ImageType;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating AccommodationImage.
 * Note: The actual image file is uploaded via multipart form data.
 * This DTO contains the metadata for the image.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccommodationImageDTO {

    @NotNull(message = "Image type is required")
    private ImageType imageType;

    private String altText;
    private String caption;
    private String description;

    private Boolean isPrimary = false;
    private Integer displayOrder;
}
