package com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityImageDTOs;

import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityImage.ImageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating ParkActivityImage metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateParkActivityImageDTO {

    private ImageType imageType;

    private String altText;

    private String caption;

    private String description;

    private Boolean isPrimary;

    private Boolean isActive;

    private Boolean isWebActive;
}
