package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * UpdateItineraryDayActivityDTO - Data Transfer Object for updating an ItineraryDayActivity
 *
 * Note: sortOrder cannot be updated directly - use the reorder endpoint.
 * activityId cannot be changed after creation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateItineraryDayActivityDTO {

    private BigDecimal durationHours;
    private String startTime;
    private String endTime;
    private String notes;
    private Boolean isIncludedInPrice;
    private Boolean isOptional;
}
