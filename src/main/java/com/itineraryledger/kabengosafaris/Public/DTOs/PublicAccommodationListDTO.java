package com.itineraryledger.kabengosafaris.Public.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationCategory;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationType;
import com.itineraryledger.kabengosafaris.Public.Annotations.Translatable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight Accommodation DTO for listing pages.
 * Only includes fields needed for cards/grids.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicAccommodationListDTO {

    private String id;
    private String slug;
    @Translatable private String name;
    private AccommodationType accommodationType;
    @Translatable private String accommodationTypeDisplayName;
    private AccommodationCategory category;
    @Translatable private String categoryDisplayName;
    private Integer starRating;
    private String region;
    @Translatable private String shortDescription;
    private String primaryImageUrl;
}
