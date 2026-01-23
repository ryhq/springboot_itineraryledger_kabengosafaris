package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * CreateSafariDayParkActivityDTO - Data Transfer Object for creating a SafariDayParkActivity
 *
 * Note: sortOrder is auto-assigned and handled by reorder methods
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSafariDayParkActivityDTO {

    @NotBlank(message = "Park ID is required")
    private String parkId; // Obfuscated ID (must match parent park)

    @NotBlank(message = "Activity ID is required")
    private String activityId; // Obfuscated ID

    private BigDecimal durationHours;
    private String startTime; // e.g., "06:00"
    private String endTime;
    private String notes;
    private Boolean isIncludedInPrice;
}
