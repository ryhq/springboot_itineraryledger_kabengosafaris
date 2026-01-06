package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationBoardTypeDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateAccommodationBoardTypeDTO - DTO for creating accommodation board types
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateAccommodationBoardTypeDTO {

    @NotBlank(message = "Accommodation ID is required")
    private String accommodationId; // Obfuscated ID

    @NotBlank(message = "Board type name is required")
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
