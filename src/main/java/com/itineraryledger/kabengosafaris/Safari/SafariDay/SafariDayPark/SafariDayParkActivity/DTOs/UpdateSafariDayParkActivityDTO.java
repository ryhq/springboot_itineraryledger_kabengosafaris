package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * UpdateSafariDayParkActivityDTO - Data Transfer Object for updating SafariDayParkActivity
 *
 * Note: parkActivity cannot be changed after creation. To change the activity,
 * delete this record and create a new one.
 *
 * sortOrder is handled by reorder methods.
 *
 * Safari-specific fields (isCompleted, actualDurationHours, sightingsNotes, guestExperience,
 * isSkipped, skipReason) are updated via separate tracking endpoints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSafariDayParkActivityDTO {

    private BigDecimal durationHours;

    private String startTime; // e.g., "06:00"

    private String endTime;

    private String notes;

    private Boolean isIncludedInPrice;
}
