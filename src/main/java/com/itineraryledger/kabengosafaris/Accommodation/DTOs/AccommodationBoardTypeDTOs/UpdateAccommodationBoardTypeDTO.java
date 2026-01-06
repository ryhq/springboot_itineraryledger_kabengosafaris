package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationBoardTypeDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateAccommodationBoardTypeDTO - DTO for updating accommodation board types
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateAccommodationBoardTypeDTO {

    private String name;

    private String description;

    private String mealsIncluded;

    private Boolean breakfastIncluded;

    private Boolean lunchIncluded;

    private Boolean dinnerIncluded;

    private Boolean snacksIncluded;

    private Boolean drinksIncluded;

    private Boolean alcoholicDrinksIncluded;

    private String inclusions;

    private String exclusions;

    private String mealTimes;

    private Boolean isActive;
}
