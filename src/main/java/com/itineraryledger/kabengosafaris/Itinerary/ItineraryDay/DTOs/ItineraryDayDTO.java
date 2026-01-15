package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ItineraryDayDTO - Data Transfer Object for ItineraryDay entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryDayDTO {
    private String id;
    private String itineraryId;
    private Integer dayNumber;
    private String dayTag;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
