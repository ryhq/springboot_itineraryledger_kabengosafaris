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

    /*
     * Enums arrive as Strings so a blank can CLEAR the field; null still means
     * "leave unchanged". Bound as the enum itself, an empty value makes Jackson
     * reject the whole request body, which is how a set value became impossible
     * to unset (see the charging-basis fix).
     */
    private String imageType;

    private String altText;

    private String caption;

    private String description;

    private Boolean isPrimary;

    private Boolean isActive;

    private Boolean isWebActive;
}
