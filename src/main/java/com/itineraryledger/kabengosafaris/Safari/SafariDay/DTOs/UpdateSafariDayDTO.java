package com.itineraryledger.kabengosafaris.Safari.SafariDay.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateSafariDayDTO - DTO for updating SafariDay
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSafariDayDTO {

    private String title;

    // Description fields
    private String description;
    private String morningActivities;
    private String afternoonActivities;
    private String eveningActivities;
    private String wildlifeHighlights;
    private String scenicHighlights;
    private String specialNotes;

    // Location & Travel
    private String startLocation;
    private String endLocation;
    private Integer distanceKm;

    // Settings
    private Boolean isOvernight;
    private String mealsIncluded;
    private String internalNotes;

    // Safari-specific
    private String weatherNotes;
    private String actualStartTime;
    private String actualEndTime;
    private String driverNotes;
}
