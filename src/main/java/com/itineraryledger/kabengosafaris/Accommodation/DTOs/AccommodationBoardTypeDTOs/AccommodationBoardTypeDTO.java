package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationBoardTypeDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AccommodationBoardTypeDTO - Response DTO for accommodation board types
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccommodationBoardTypeDTO {

    private String id; // Obfuscated ID

    private String accommodationId; // Obfuscated ID

    private String accommodationName;

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

    private Integer mealCount; // Calculated field

    private Boolean isFullMealPlan; // Calculated field

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
