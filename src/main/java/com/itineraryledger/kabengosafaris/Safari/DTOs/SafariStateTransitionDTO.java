package com.itineraryledger.kabengosafaris.Safari.DTOs;

import com.itineraryledger.kabengosafaris.Safari.Enums.SafariCancellationReason;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariHoldReason;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for Safari state transition requests
 *
 * Used for simplified 14-state workflow with context-specific fields
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

    // ========================
    // SIMPLIFIED WORKFLOW FIELDS
    // ========================

    /**
     * Hold reason enum for ON_HOLD state
     * Required when putting safari on hold
     */
    private SafariHoldReason holdReason;

    /**
     * Cancellation reason enum for CANCELLED state
     * Required when cancelling safari
     */
    private SafariCancellationReason cancellationReason;

    /**
     * Indicates if payment being recorded is full payment (true) or partial (false)
     * Used in recordPayment to determine PENDING_PAYMENT vs FULLY_PAID
     */
    private Boolean isFullPayment;

    /**
     * Indicates if refund being recorded is final refund (true) or partial (false)
     * Used in recordRefund to determine REFUND_PENDING vs REFUND_COMPLETE
     */
    private Boolean isFinalRefund;
}
