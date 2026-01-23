package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SafariDayParkActivityDTO - Data Transfer Object for SafariDayParkActivity entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SafariDayParkActivityDTO {
    private String id;
    private String safariDayParkId;
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

    // Safari-specific fields
    private Boolean isCompleted;
    private LocalDateTime completedAt;
    private BigDecimal actualDurationHours;
    private String sightingsNotes;
    private String guestExperience;
    private Boolean isSkipped;
    private String skipReason;

    private LocalDateTime createdAt;
}
