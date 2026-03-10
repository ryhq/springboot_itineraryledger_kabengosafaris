package com.itineraryledger.kabengosafaris.Hero.DTOs;

import com.itineraryledger.kabengosafaris.Hero.Enums.HeroPage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    private Boolean isActive = true;

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Overlay color must be a valid 6-digit hex color (e.g., #000000)")
    private String overlayColor;

    private Double overlayOpacity;

    private String textAlignment;

    private String cssClasses;
}
