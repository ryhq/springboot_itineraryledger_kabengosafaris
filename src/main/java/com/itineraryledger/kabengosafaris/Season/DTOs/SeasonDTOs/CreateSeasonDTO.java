package com.itineraryledger.kabengosafaris.Season.DTOs.SeasonDTOs;

import com.itineraryledger.kabengosafaris.Season.DTOs.SeasonPeriodDTOs.CreateSeasonPeriodDTO;
import com.itineraryledger.kabengosafaris.Season.Season;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * CreateSeasonDTO - Request DTO for creating a new Season
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSeasonDTO {

    private String name; // Required
    private Season.SeasonType seasonType;
    private String description;

    /**
     * Optional: Obfuscated accommodation ID
     * If NULL or empty, this will be a GLOBAL season
     * If provided, this will be an accommodation-specific season
     */
    private String accommodationId;

    /**
     * Optional: Initial season periods to create with this season
     */
    private List<CreateSeasonPeriodDTO> seasonPeriods;

    private Boolean isActive; // Defaults to true if not provided
}
