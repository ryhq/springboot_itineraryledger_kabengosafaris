package com.itineraryledger.kabengosafaris.Hero.DTOs;

import com.itineraryledger.kabengosafaris.Hero.Enums.HeroPage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating Hero
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateHeroDTO {

    @NotBlank(message = "Title is required")
    private String title;

    private String subtitle;

    private String description;

    @NotNull(message = "Page is required")
    private HeroPage page;

    private String ctaText;

    private String ctaLink;

    @Builder.Default
    private Integer displayOrder = 0;

    @Builder.Default
    private Boolean isActive = true;

    private String overlayColor;

    private Double overlayOpacity;

    private String textAlignment;

    private String cssClasses;
}
