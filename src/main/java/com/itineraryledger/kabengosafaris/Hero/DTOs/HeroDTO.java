package com.itineraryledger.kabengosafaris.Hero.DTOs;

import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.Hero.Enums.HeroPage;
import com.itineraryledger.kabengosafaris.Public.Annotations.Translatable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Hero response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeroDTO {

    private String id; // Obfuscated ID

    @Translatable private String title;

    @Translatable private String subtitle;

    @Translatable private String description;

    private HeroPage page;

    @Translatable private String pageDisplayName;

    @Translatable private String ctaText;

    private String ctaLink;

    private Integer displayOrder;

    private Boolean isActive;

    private String overlayColor;

    private Double overlayOpacity;

    private String textAlignment;

    private String cssClasses;

    // Primary image URL
    private String primaryImageUrl;

    // Number of images
    private Long imageCount;

    // Audit fields
    private String createdById;
    private String createdByName;
    private String updatedById;
    private String updatedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
