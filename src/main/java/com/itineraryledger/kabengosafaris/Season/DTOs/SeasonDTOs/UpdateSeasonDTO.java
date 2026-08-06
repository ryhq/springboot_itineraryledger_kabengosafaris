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
    /*
     * Enums arrive as Strings so a blank can CLEAR the field; null still means
     * "leave unchanged". Bound as the enum itself, an empty value makes Jackson
     * reject the whole request body, which is how a set value became impossible
     * to unset (see the charging-basis fix).
     */
    private String seasonType;
    private String description;
    private Boolean isActive;

    /**
     * Note: accommodationId and isGlobal cannot be changed after creation
     * This maintains data integrity for related pricing and periods
     */
}
