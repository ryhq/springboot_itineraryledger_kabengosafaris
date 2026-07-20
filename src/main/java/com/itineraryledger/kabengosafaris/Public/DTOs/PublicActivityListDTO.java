package com.itineraryledger.kabengosafaris.Public.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Public.Annotations.Translatable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight Activity DTO for listing pages.
 * Only includes fields needed for cards/grids.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicActivityListDTO {

    private String slug;
    @Translatable private String name;
    @Translatable private String description;
    private String primaryImageUrl;
    private String seasonAvailability;
    private Long safariCount; // distinct active itineraries that include this activity (public "most popular")
}
