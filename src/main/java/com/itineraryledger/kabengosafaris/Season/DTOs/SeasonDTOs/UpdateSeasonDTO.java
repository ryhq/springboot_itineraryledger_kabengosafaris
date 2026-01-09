package com.itineraryledger.kabengosafaris.Season.DTOs.SeasonDTOs;

import com.itineraryledger.kabengosafaris.Season.Season;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateSeasonDTO - Request DTO for updating a Season
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSeasonDTO {

    private String name;
    private Season.SeasonType seasonType;
    private String description;
    private Boolean isActive;

    /**
     * Note: accommodationId and isGlobal cannot be changed after creation
     * This maintains data integrity for related pricing and periods
     */
}
