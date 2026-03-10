package com.itineraryledger.kabengosafaris.Hero.DTOs.HeroImageDTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating HeroImage metadata.
 * Note: To replace the actual image file, a new upload is required.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateHeroImageDTO {

    private String altText;
    private String caption;
    private String description;
    private Boolean isPrimary;
    private Boolean isActive;
}
