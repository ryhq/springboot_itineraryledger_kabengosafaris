package com.itineraryledger.kabengosafaris.Hero.DTOs;

import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.Hero.Enums.HeroPage;

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

    private String title;

    private String subtitle;

    private String description;

    private HeroPage page;

    private String pageDisplayName;

    private String ctaText;

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
