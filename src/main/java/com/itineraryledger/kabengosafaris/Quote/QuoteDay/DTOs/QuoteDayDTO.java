package com.itineraryledger.kabengosafaris.Quote.QuoteDay.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuoteDayDTO {
    private String id;
    private String quoteId;

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
    private String internalNotes;

    private Long activityCount;
    private Long accommodationCount;
    private Long parkCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
