package com.itineraryledger.kabengosafaris.Public.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Park.ParkType;
import com.itineraryledger.kabengosafaris.Public.Annotations.Translatable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight Park DTO for listing pages.
 * Only includes fields needed for cards/grids.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicParkListDTO {

    private String slug;
    @Translatable private String name;
    private ParkType parkType;
    private String region;
    @Translatable private String shortDescription;
    private String primaryImageUrl;
    private String tags; // JSON array string, e.g. ["Big Five","Great Migration"] — for the highlights facet
    private Long safariCount; // distinct active itineraries that visit this park (public "most visited")
}
