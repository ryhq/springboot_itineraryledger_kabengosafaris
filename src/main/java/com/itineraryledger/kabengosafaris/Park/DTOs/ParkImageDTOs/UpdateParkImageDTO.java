package com.itineraryledger.kabengosafaris.Park.DTOs.ParkImageDTOs;

import com.itineraryledger.kabengosafaris.Park.Entities.ParkImage.ImageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating ParkImage metadata.
 * Note: To replace the actual image file, a new upload is required.
 * Note: displayOrder is managed via the reorder endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateParkImageDTO {

    private ImageType imageType;
    private String altText;
    private String caption;
    private String description;
    private Boolean isPrimary;
    private Boolean isActive;
    private Boolean isWebActive;
}
