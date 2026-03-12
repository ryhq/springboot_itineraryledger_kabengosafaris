package com.itineraryledger.kabengosafaris.Public.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Public.Annotations.Translatable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public-safe Hero DTO - excludes audit fields, isActive, displayOrder, page enum.
 * Used for hero sections on the public website.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicHeroDTO {

    private String id;
    @Translatable private String title;
    @Translatable private String subtitle;
    @Translatable private String description;
    @Translatable private String ctaText;
    private String ctaLink;

    // Styling
    private String overlayColor;
    private Double overlayOpacity;
    private String textAlignment;
    private String cssClasses;

    // Image
    private String primaryImageUrl;
    private Long imageCount;
}
