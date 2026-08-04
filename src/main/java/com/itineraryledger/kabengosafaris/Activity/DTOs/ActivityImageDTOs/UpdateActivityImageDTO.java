package com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityImageDTOs;

import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityImage.ImageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating ActivityImage metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateActivityImageDTO {

    private ImageType imageType;

    private String altText;

    private String caption;

    private String description;

    private Boolean isPrimary;

    private Boolean isActive;

    private Boolean isWebActive;
}
