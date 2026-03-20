package com.itineraryledger.kabengosafaris.Public.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Public.Annotations.Translatable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified gallery image DTO that includes entity context.
 * Used by the public gallery endpoint to return images from all entity types.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicGalleryImageDTO {

    private String imageUrl;
    @Translatable private String altText;
    @Translatable private String caption;
    private String imageType;

    // Entity context
    private String entityType;   // PARK, ACTIVITY, ACCOMMODATION
    private String entityName;
    private String entitySlug;
}
