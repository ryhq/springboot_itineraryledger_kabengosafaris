package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationImageDTOs;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage.ImageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating AccommodationImage metadata.
 * Note: To replace the actual image file, a new upload is required.
 * Note: displayOrder is managed via the reorder endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAccommodationImageDTO {

    private ImageType imageType;
    private String altText;
    private String caption;
    private String description;
    private Boolean isPrimary;
    private Boolean isActive;
    private Boolean isWebActive;
}
