package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.DTOs;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateItineraryDayDTO - Data Transfer Object for updating an ItineraryDay
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateItineraryDayDTO {

    @Min(value = 1, message = "Day number must be at least 1")
    private Integer dayNumber;

    private String dayTag;

    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    private String description;
    private String morningActivities;
    private String afternoonActivities;
    private String eveningActivities;
    private String wildlifeHighlights;
    private String scenicHighlights;
    private String specialNotes;
    private String startLocation;
    private String endLocation;
    private Integer distanceKm;
    private Boolean isOvernight;
    private String mealsIncluded;
}
