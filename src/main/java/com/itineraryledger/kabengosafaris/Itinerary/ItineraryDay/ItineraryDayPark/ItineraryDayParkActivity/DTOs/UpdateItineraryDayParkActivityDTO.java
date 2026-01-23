package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * UpdateItineraryDayParkActivityDTO - Data Transfer Object for updating ItineraryDayParkActivity
 *
 * Note: parkActivity cannot be changed after creation. To change the activity,
 * delete this record and create a new one.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateItineraryDayParkActivityDTO {

    private BigDecimal durationHours;

    private String startTime; // e.g., "06:00"

    private String endTime;

    private String notes;

    private Boolean isIncludedInPrice;
}
