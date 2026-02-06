package com.itineraryledger.kabengosafaris.Safari.Enums;

/**
 * SafariState - Represents the BOOKING/OPERATIONAL state of a Safari
 *
 * This is the administrative state that tracks where the safari is
 * in its booking lifecycle. These states are manually set and represent
 * business decisions, not time-based calculations.
 *
 * For time-based phase tracking, see SafariPhase enum.
 *
 * Simplified workflow (14 states):
 * Core Journey: DRAFT → PENDING_APPROVAL → APPROVED → CONFIRMED →
 *               PENDING_PAYMENT → FULLY_PAID → IN_PROGRESS → COMPLETED → CLOSED
 *
 * Exception/Special: ON_HOLD, CANCELLED, REFUND_PENDING, REFUND_COMPLETE, DISPUTED
 */
public enum SafariState {
    // ========================
    // CORE JOURNEY (9 states)
    // ========================
    DRAFT("Draft", "Safari booking is being prepared, not yet submitted for approval"),
    PENDING_APPROVAL("Pending Approval", "Safari booking submitted, awaiting management approval"),
    APPROVED("Approved", "Safari booking approved by management"),
    CONFIRMED("Confirmed", "Safari confirmed with client and suppliers"),
    PENDING_PAYMENT("Pending Payment", "Awaiting deposit or full payment from client"),
    FULLY_PAID("Fully Paid", "All payments received, safari ready to commence"),
    IN_PROGRESS("In Progress", "Safari is currently running"),
    COMPLETED("Completed", "Safari has successfully ended"),
    CLOSED("Closed", "Safari fully completed with all post-trip tasks done"),

    // ========================
    // EXCEPTION/SPECIAL (5 states)
    // ========================
    ON_HOLD("On Hold", "Safari temporarily paused (see holdReason for details)"),
    CANCELLED("Cancelled", "Safari has been cancelled (see cancellationReason for details)"),
    REFUND_PENDING("Refund Pending", "Refund process initiated, awaiting completion"),
    REFUND_COMPLETE("Refund Complete", "Refund has been fully issued"),
    DISPUTED("Disputed", "Client has raised a dispute or complaint, under investigation");

    private final String displayName;
    private final String description;

    SafariState(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if the safari can be edited in this state
     */
    public boolean isEditable() {
        return this == DRAFT || this == PENDING_APPROVAL || this == APPROVED ||
               this == CONFIRMED || this == PENDING_PAYMENT || this == ON_HOLD;
    }

    /**
     * Check if the safari can be cancelled in this state
     */
    public boolean isCancellable() {
        return this == DRAFT || this == PENDING_APPROVAL || this == APPROVED ||
               this == CONFIRMED || this == PENDING_PAYMENT || this == FULLY_PAID ||
               this == ON_HOLD;
    }

    /**
     * Check if this is a terminal state (no further transitions expected)
     */
    public boolean isTerminal() {
        return this == CLOSED || this == CANCELLED || this == REFUND_COMPLETE;
    }

    /**
     * Check if this state indicates the safari is active/ongoing
     */
    public boolean isActive() {
        return this == CONFIRMED || this == PENDING_PAYMENT || this == FULLY_PAID ||
               this == IN_PROGRESS;
    }

    /**
     * Check if this state indicates a cancellation
     */
    public boolean isCancelled() {
        return this == CANCELLED;
    }

    /**
     * Check if this state indicates a hold/pause
     */
    public boolean isOnHold() {
        return this == ON_HOLD;
    }

    /**
     * Check if this state requires payment action
     */
    public boolean requiresPayment() {
        return this == PENDING_PAYMENT;
    }

    /**
     * Check if this state allows the safari to start
     */
    public boolean canStart() {
        return this == FULLY_PAID;
    }

    /**
     * Check if this state allows date modifications
     */
    public boolean allowsDateChange() {
        return this == DRAFT || this == PENDING_APPROVAL || this == APPROVED ||
               this == CONFIRMED || this == ON_HOLD;
    }

    /**
     * Check if this is part of the core journey (happy path)
     */
    public boolean isCoreJourney() {
        return this == DRAFT || this == PENDING_APPROVAL || this == APPROVED ||
               this == CONFIRMED || this == PENDING_PAYMENT || this == FULLY_PAID ||
               this == IN_PROGRESS || this == COMPLETED || this == CLOSED;
    }

    /**
     * Check if this is an exception/special state
     */
    public boolean isException() {
        return this == ON_HOLD || this == CANCELLED || this == REFUND_PENDING ||
               this == REFUND_COMPLETE || this == DISPUTED;
    }
}
