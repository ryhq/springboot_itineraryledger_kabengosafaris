package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * CreateItineraryDayActivityDTO - Data Transfer Object for creating an ItineraryDayActivity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateItineraryDayActivityDTO {

    @NotBlank(message = "Activity ID is required")
    private String activityId; // Obfuscated ID

    private Integer sortOrder;
    private BigDecimal durationHours;
    private String startTime;
    private String endTime;
    private String notes;
    private Boolean isIncludedInPrice;
}
