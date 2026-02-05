package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * UpdateSafariDayActivityDTO - Data Transfer Object for updating a SafariDayActivity
 *
 * Note: sortOrder cannot be updated directly - use the reorder endpoint.
 * activityId cannot be changed after creation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSafariDayActivityDTO {

    private BigDecimal durationHours;
    private String startTime;
    private String endTime;
    private String notes;
    private Boolean isIncludedInPrice;
    private Boolean isOptional;

    // Safari-specific fields
    private Boolean isCompleted;
    private String actualStartTime;
    private String actualEndTime;
    private String feedback;
    private Boolean isSkipped;
    private String skipReason;
}
