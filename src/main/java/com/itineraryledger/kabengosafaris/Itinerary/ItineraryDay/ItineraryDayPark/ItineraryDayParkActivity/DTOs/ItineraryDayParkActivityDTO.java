package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ItineraryDayParkActivityDTO - Data Transfer Object for ItineraryDayParkActivity entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryDayParkActivityDTO {
    private String id;
    private String itineraryDayParkId;
    private String parkId;
    private String parkName;
    private String activityId;
    private String activityName;
    private Integer sortOrder;
    private BigDecimal durationHours;
    private String startTime;
    private String endTime;
    private String notes;
    private Boolean isIncludedInPrice;
    private LocalDateTime createdAt;
}
