package com.itineraryledger.kabengosafaris.Safari.DTOs;

import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for Safari state transition requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SafariStateTransitionDTO {

    /**
     * Reason for the state transition (required for some transitions)
     */
    private String reason;

    /**
     * Target state for transitions that allow specifying the destination
     * (e.g., when releasing from hold, resolving disputes)
     */
    private SafariState targetState;

    /**
     * New start date for reschedule operations
     */
    private LocalDate newStartDate;

    /**
     * New end date for reschedule operations (optional, calculated from totalDays if not provided)
     */
    private LocalDate newEndDate;

    /**
     * Additional notes for the transition
     */
    private String notes;
}
