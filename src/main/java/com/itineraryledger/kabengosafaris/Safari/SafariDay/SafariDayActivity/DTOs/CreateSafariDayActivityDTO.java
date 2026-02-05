package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * CreateSafariDayActivityDTO - Data Transfer Object for creating a SafariDayActivity
 *
 * Note: sortOrder is auto-determined based on existing activities in the day.
 * First activity = 1, subsequent activities increment from there.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSafariDayActivityDTO {

    @NotBlank(message = "Activity ID is required")
    private String activityId; // Obfuscated ID

    private BigDecimal durationHours;
    private String startTime;
    private String endTime;
    private String notes;
    private Boolean isIncludedInPrice;
    private Boolean isOptional;
}
