package com.itineraryledger.kabengosafaris.Public.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariPhase;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Public-safe Safari DTO - excludes customer data, audit info, and sensitive operational fields
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicSafariDTO {

    private String id;
    private String name;
    private String slug;

    // Itinerary reference (public)
    private String itineraryId;
    private String itineraryName;

    // State
    private SafariState state;
    private String stateDisplayName;

    // Phase
    private SafariPhase phase;
    private String phaseDisplayName;

    // Dates
    private LocalDate startDate;
    private LocalDate endDate;

    // Duration
    private Integer totalDays;
    private Integer totalNights;

    // Descriptions
    private String description;
    private String highlights;
    private String startLocation;
    private String endLocation;

    // Status
    private Boolean isActive;

    // Counts
    private Integer totalPaxCount;
    private Integer totalDaysCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
