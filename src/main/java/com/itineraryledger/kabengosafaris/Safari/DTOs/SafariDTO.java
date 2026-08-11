package com.itineraryledger.kabengosafaris.Safari.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariPhase;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SafariDTO - Data Transfer Object for Safari entity
 * Contains obfuscated ID for secure data transfer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SafariDTO {
    private String id;
    private String name;
    private String code;
    private String slug;

    // Itinerary reference
    private String itineraryId;
    private String itineraryName;
    private String itineraryCode;

    // Customer reference
    private String customerId;
    private String customerName;
    private String customerCode;
    /** so "send details" can say where it is going, and refuse when it cannot */
    private String customerEmail;
    private String customerPhone;

    // State information (booking/operational - manually managed)
    private SafariState state;
    private String stateDisplayName;
    private String stateDescription;
    private String stateReason;
    private LocalDateTime stateChangedAt;

    // Phase information (time-based - automatically calculated)
    private SafariPhase phase;
    private String phaseDisplayName;
    private String phaseDescription;
    private Integer phaseUrgencyLevel;
    private String phaseColorCode;

    // Dates
    private LocalDate startDate;
    private LocalDate endDate;

    // Duration
    private Integer totalDays;
    private Integer totalNights;
    private Integer carCount;

    // Descriptions
    private String description;
    private String highlights;
    private String startLocation;
    private String endLocation;

    // Safari-specific
    private String specialRequests;
    private String dietaryRequirements;
    private String emergencyContact;

    /**
     * For the office. The entity has always had it and the update endpoint has
     * always taken it — it just never came back, so the field read "—" however
     * often it was filled in and the note looked lost.
     */
    private String internalNotes;

    // Status flags
    private Boolean isActive;
    private Boolean isEditable;
    private Boolean isCancellable;
    private Boolean hasStarted;
    private Boolean hasEnded;
    private Boolean isInProgress;
    private Boolean isUrgentPhase;

    // Time calculations
    private Long daysUntilStart;
    private Long daysSinceEnd;
    private Integer currentDayNumber;

    // Counts
    private Integer totalPaxCount;

    /**
     * Money, read from this safari's invoices rather than held here.
     *
     * The safari used to carry payment as two states of its own, set by hand and
     * joined to nothing. This is the invoice's answer, so it cannot disagree
     * with it.
     */
    private SafariBillingDTO billing;

    /**
     * What the trip cost us, read from its expenses, and the margin against what
     * we invoiced. Derived like billing — nothing here is stored on the safari.
     */
    private SafariCostingDTO costing;
    private Integer totalDaysCount;

    // Audit information
    private String createdById;
    private String createdByUsername;
    private String createdByFullName;
    private String updatedById;
    private String updatedByUsername;
    private String updatedByFullName;

    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
