package com.itineraryledger.kabengosafaris.Public.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Public.Annotations.Translatable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Full Activity DTO for detail pages.
 * Excludes: createdAt, updatedAt, isActive, isWebActive, hasTariff, chargingBasis.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicActivityDetailDTO {

    private String slug;
    @Translatable private String name;

    // Descriptions
    @Translatable private String description;
    @Translatable private String detailedDescription;

    // Requirements
    private Integer minimumAge;
    private Integer maximumParticipants;
    @Translatable private String equipmentRequired;
    private String seasonAvailability;
    @Translatable private String safetyInformation;

    // Image
    private String primaryImageUrl;

    @Translatable private String tags;
}
