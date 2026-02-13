package com.itineraryledger.kabengosafaris.Hero.DTOs;

import com.itineraryledger.kabengosafaris.Hero.Enums.HeroPage;

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

    private Integer displayOrder;

    private Boolean isActive;

    private String overlayColor;

    private Double overlayOpacity;

    private String textAlignment;

    private String cssClasses;
}
