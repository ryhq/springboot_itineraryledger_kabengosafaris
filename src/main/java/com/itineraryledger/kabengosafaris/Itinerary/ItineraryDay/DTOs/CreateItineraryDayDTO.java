package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateItineraryDayDTO - Data Transfer Object for creating a new ItineraryDay
 *
 * Note: dayNumber is auto-determined based on existing days in the itinerary.
 * The first day will be dayNumber = 1, subsequent days increment from there.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateItineraryDayDTO {

    @NotBlank(message = "Day title is required")
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
