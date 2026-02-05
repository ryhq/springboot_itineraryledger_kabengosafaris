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
 * Supports dual update modes:
 * - Planning updates (durationHours, startTime, endTime, notes, isIncludedInPrice) require editable safari state
 * - Operational updates (isCompleted, actualDurationHours, sightingsNotes, guestExperience, isSkipped, skipReason) allowed anytime
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSafariDayParkActivityDTO {

    // Planning fields (require editable safari)
    private BigDecimal durationHours;

    private String startTime; // e.g., "06:00"

    private String endTime;

    private String notes;

    private Boolean isIncludedInPrice;

    // Safari-specific operational fields (allowed anytime)
    private Boolean isCompleted;

    private BigDecimal actualDurationHours;

    private String sightingsNotes;

    private String guestExperience;

    private Boolean isSkipped;

    private String skipReason;
}
