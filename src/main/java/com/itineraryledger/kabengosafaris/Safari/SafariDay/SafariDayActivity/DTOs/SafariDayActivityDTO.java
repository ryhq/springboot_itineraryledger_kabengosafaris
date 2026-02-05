package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * SafariDayActivityDTO - Data Transfer Object for SafariDayActivity entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SafariDayActivityDTO {
    private String id;
    private String safariDayId;
    private String activityId;
    private String activityName;
    private String activitySlug;
    private Integer sortOrder;
    private BigDecimal durationHours;
    private String startTime;
    private String endTime;
    private String notes;
    private Boolean isIncludedInPrice;
    private Boolean isOptional;

    // Safari-specific fields
    private Boolean isCompleted;
    private LocalDateTime completedAt;
    private String actualStartTime;
    private String actualEndTime;
    private String feedback;
    private Boolean isSkipped;
    private String skipReason;

    private LocalDateTime createdAt;
}
