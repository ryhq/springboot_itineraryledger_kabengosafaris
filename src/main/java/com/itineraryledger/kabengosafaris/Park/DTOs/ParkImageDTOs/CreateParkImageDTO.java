package com.itineraryledger.kabengosafaris.Park.DTOs.ParkImageDTOs;

import com.itineraryledger.kabengosafaris.Park.Entities.ParkImage.ImageType;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO for creating ParkImage.
 * Contains the image file and metadata for upload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateParkImageDTO {

    @NotNull(message = "Park ID is required")
    private String parkId;

    @NotNull(message = "Image file is required")
    private MultipartFile image;

    private ImageType imageType;

    private String altText;

    private String caption;

    private String description;
}
