package com.itineraryledger.kabengosafaris.Safari.Enums;

/**
 * SafariState - Represents the BOOKING/OPERATIONAL state of a Safari
 *
 * This is the administrative state that tracks where the safari is
 * in its booking lifecycle. These states are manually set and represent
 * business decisions, not time-based calculations.
 *
 * For time-based phase tracking, see SafariPhase enum.
 */
public enum SafariState {
    // ========================
    // BOOKING STATES
    // ========================
    DRAFT("Draft", "Safari booking is being prepared, not yet submitted for approval"),
    PENDING_APPROVAL("Pending Approval", "Safari booking submitted, awaiting management approval"),
    APPROVED("Approved", "Safari booking approved by management"),
    CONFIRMED("Confirmed", "Safari confirmed with client and suppliers"),

    // ========================
    // PAYMENT STATES
    // ========================
    PENDING_DEPOSIT("Pending Deposit", "Awaiting deposit payment from client"),
    DEPOSIT_PAID("Deposit Paid", "Deposit received, awaiting full payment"),
    FULLY_PAID("Fully Paid", "All payments received"),

    // ========================
    // OPERATIONAL STATES
    // ========================
    READY("Ready", "All preparations complete, safari ready to commence"),
    IN_PROGRESS("In Progress", "Safari is currently running"),

    // ========================
    // COMPLETION STATES
    // ========================
    COMPLETED("Completed", "Safari has successfully ended"),
    PENDING_REVIEW("Pending Review", "Safari ended, awaiting post-trip review/feedback"),
    CLOSED("Closed", "Safari fully completed with all post-trip tasks done"),

    // ========================
    // HOLD/PAUSE STATES
    // ========================
    ON_HOLD("On Hold", "Safari temporarily paused due to issues"),
    PENDING_DOCUMENTS("Pending Documents", "Awaiting required documents (visas, permits, etc.)"),
    PENDING_AVAILABILITY("Pending Availability", "Awaiting accommodation/activity availability confirmation"),

    // ========================
    // RESCHEDULE STATES
    // ========================
    POSTPONED("Postponed", "Safari rescheduled to a later date"),
    RESCHEDULING("Rescheduling", "Safari dates being changed"),

    // ========================
    // CANCELLATION STATES
    // ========================
    CANCELLATION_REQUESTED("Cancellation Requested", "Client requested cancellation"),
    CANCELLED("Cancelled", "Safari has been cancelled"),
    CANCELLED_BY_CLIENT("Cancelled by Client", "Safari cancelled at client's request"),
    CANCELLED_BY_OPERATOR("Cancelled by Operator", "Safari cancelled by tour operator"),
    CANCELLED_FORCE_MAJEURE("Cancelled - Force Majeure", "Safari cancelled due to unforeseen circumstances"),

    // ========================
    // REFUND STATES
    // ========================
    REFUND_PENDING("Refund Pending", "Cancellation processed, refund pending"),
    REFUND_PARTIAL("Partial Refund Issued", "Partial refund has been issued"),
    REFUND_COMPLETE("Refund Complete", "Full refund has been issued"),

    // ========================
    // DISPUTE STATES
    // ========================
    DISPUTED("Disputed", "Client has raised a dispute or complaint"),
    UNDER_INVESTIGATION("Under Investigation", "Dispute is being investigated");

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
               this == CONFIRMED || this == PENDING_DEPOSIT || this == DEPOSIT_PAID ||
               this == ON_HOLD || this == POSTPONED || this == RESCHEDULING ||
               this == PENDING_DOCUMENTS || this == PENDING_AVAILABILITY;
    }

    /**
     * Check if the safari can be cancelled in this state
     */
    public boolean isCancellable() {
        return this == DRAFT || this == PENDING_APPROVAL || this == APPROVED ||
               this == CONFIRMED || this == PENDING_DEPOSIT || this == DEPOSIT_PAID ||
               this == FULLY_PAID || this == READY || this == ON_HOLD ||
               this == POSTPONED || this == PENDING_DOCUMENTS || this == PENDING_AVAILABILITY;
    }

    /**
     * Check if this is a terminal state (no further transitions expected)
     */
    public boolean isTerminal() {
        return this == CLOSED || this == CANCELLED || this == CANCELLED_BY_CLIENT ||
               this == CANCELLED_BY_OPERATOR || this == CANCELLED_FORCE_MAJEURE ||
               this == REFUND_COMPLETE;
    }

    /**
     * Check if this state indicates the safari is active/ongoing
     */
    public boolean isActive() {
        return this == CONFIRMED || this == PENDING_DEPOSIT || this == DEPOSIT_PAID ||
               this == FULLY_PAID || this == READY || this == IN_PROGRESS;
    }

    /**
     * Check if this state indicates a cancellation
     */
    public boolean isCancelled() {
        return this == CANCELLED || this == CANCELLED_BY_CLIENT ||
               this == CANCELLED_BY_OPERATOR || this == CANCELLED_FORCE_MAJEURE;
    }

    /**
     * Check if this state indicates a hold/pause
     */
    public boolean isOnHold() {
        return this == ON_HOLD || this == PENDING_DOCUMENTS ||
               this == PENDING_AVAILABILITY || this == RESCHEDULING;
    }

    /**
     * Check if this state requires payment action
     */
    public boolean requiresPayment() {
        return this == PENDING_DEPOSIT || this == DEPOSIT_PAID;
    }

    /**
     * Check if this state allows the safari to start
     */
    public boolean canStart() {
        return this == READY || this == FULLY_PAID;
    }

    /**
     * Check if this state allows date modifications
     */
    public boolean allowsDateChange() {
        return this == DRAFT || this == PENDING_APPROVAL || this == APPROVED ||
               this == CONFIRMED || this == ON_HOLD || this == POSTPONED ||
               this == RESCHEDULING;
    }
}
