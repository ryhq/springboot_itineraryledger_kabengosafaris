package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationImageDTOs;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage.ImageType;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO for creating AccommodationImage.
 * Contains the image file and metadata for upload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccommodationImageDTO {

    @NotNull(message = "Accommodation ID is required")
    private String accommodationId;

    @NotNull(message = "Image file is required")
    private MultipartFile image;

    private ImageType imageType;

    private String altText;

    private String caption;

    private String description;
}
