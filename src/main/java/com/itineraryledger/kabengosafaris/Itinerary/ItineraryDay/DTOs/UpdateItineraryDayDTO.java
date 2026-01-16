package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.DTOs;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateItineraryDayDTO - Data Transfer Object for updating an ItineraryDay
 *
 * Note: dayNumber and dayTag cannot be updated as they are auto-managed.
 * dayNumber is determined by creation order, dayTag is auto-generated from dayNumber.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateItineraryDayDTO {

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
