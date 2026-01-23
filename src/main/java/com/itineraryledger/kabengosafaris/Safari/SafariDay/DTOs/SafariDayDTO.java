package com.itineraryledger.kabengosafaris.Safari.SafariDay.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SafariDayDTO - Data Transfer Object for SafariDay entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SafariDayDTO {
    private String id;
    private String safariId;
    private Integer dayNumber;
    private String dayTag;
    private String title;

    // Actual date for this safari day
    private LocalDate actualDate;
    private Boolean isPast;
    private Boolean isToday;
    private Boolean isFuture;

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
    private Boolean isModified;
    private String modificationNotes;
    private String weatherNotes;
    private String actualStartTime;
    private String actualEndTime;
    private String driverNotes;

    // Counts
    private Integer activitiesCount;
    private Integer parksCount;
    private Integer accommodationsCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
