package com.itineraryledger.kabengosafaris.Safari.Enums;

/**
 * SafariCancellationReason - Specifies why a Safari was cancelled
 *
 * This enum provides context for the CANCELLED state, replacing the need
 * for separate CANCELLED_BY_CLIENT, CANCELLED_BY_OPERATOR, and
 * CANCELLED_FORCE_MAJEURE states.
 */
public enum SafariCancellationReason {
    BY_CLIENT("Cancelled by Client", "Safari cancelled at client's request"),
    BY_OPERATOR("Cancelled by Operator", "Safari cancelled by tour operator"),
    FORCE_MAJEURE("Force Majeure", "Safari cancelled due to unforeseen circumstances (natural disaster, political unrest, etc.)"),
    PAYMENT_FAILURE("Payment Failure", "Cancelled due to non-payment or payment issues"),
    NO_AVAILABILITY("No Availability", "Cancelled due to lack of accommodation/activity availability"),
    OTHER("Other", "Other cancellation reason (see notes for details)");

    private final String displayName;
    private final String description;

    SafariCancellationReason(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
