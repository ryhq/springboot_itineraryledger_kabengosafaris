package com.itineraryledger.kabengosafaris.Quote.QuoteDay.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Patch payload for a QuoteDay. Fields are nullable so the service can apply
 * only those that are provided.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQuoteDayDTO {

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
    private String internalNotes;
}
