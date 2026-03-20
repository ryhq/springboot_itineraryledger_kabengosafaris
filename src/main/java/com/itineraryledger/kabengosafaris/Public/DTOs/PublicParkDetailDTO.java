package com.itineraryledger.kabengosafaris.Public.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Park.ParkType;
import com.itineraryledger.kabengosafaris.Public.Annotations.Translatable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Full Park DTO for detail pages.
 * Excludes: createdAt, updatedAt, internalNotes, isActive.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicParkDetailDTO {

    private String slug;
    @Translatable private String name;
    private ParkType parkType;

    // Location
    private String region;
    private String district;
    private String location;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String elevation;
    private String size;

    // Descriptions
    @Translatable private String shortDescription;
    @Translatable private String fullDescription;
    @Translatable private String history;
    @Translatable private String ecosystem;
    @Translatable private String wildlife;
    @Translatable private String vegetation;

    // Image
    private String primaryImageUrl;

    // Visitor info
    @Translatable private String bestTimeToVisit;
    private String openingHours;
    @Translatable private String accessInformation;

    @Translatable private String tags;
}
