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

    /*
     * Enums arrive as Strings so a blank can CLEAR the field; null still means
     * "leave unchanged". Bound as the enum itself, an empty value makes Jackson
     * reject the whole request body, which is how a set value became impossible
     * to unset (see the charging-basis fix).
     */
    private String page;

    private String ctaText;

    private String ctaLink;

    private Boolean isActive;

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Overlay color must be a valid 6-digit hex color (e.g., #000000)")
    private String overlayColor;

    private Double overlayOpacity;

    private String textAlignment;

    private String cssClasses;
}
