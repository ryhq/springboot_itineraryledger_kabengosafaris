package com.itineraryledger.kabengosafaris.Hero.DTOs;

import com.itineraryledger.kabengosafaris.Hero.Enums.HeroPage;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating Hero
 * All fields are optional
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateHeroDTO {

    private String title;

    private String subtitle;

    private String description;

    private HeroPage page;

    private String ctaText;

    private String ctaLink;

    private Boolean isActive;

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Overlay color must be a valid 6-digit hex color (e.g., #000000)")
    private String overlayColor;

    private Double overlayOpacity;

    private String textAlignment;

    private String cssClasses;
}
