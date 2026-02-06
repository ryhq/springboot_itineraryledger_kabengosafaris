package com.itineraryledger.kabengosafaris.Safari.Enums;

/**
 * SafariHoldReason - Specifies why a Safari is in ON_HOLD state
 *
 * This enum provides context for the ON_HOLD state, replacing the need
 * for separate PENDING_DOCUMENTS, PENDING_AVAILABILITY, POSTPONED, and
 * RESCHEDULING states.
 */
public enum SafariHoldReason {
    PENDING_DOCUMENTS("Pending Documents", "Awaiting required documents (visas, permits, etc.)"),
    PENDING_AVAILABILITY("Pending Availability", "Awaiting accommodation/activity availability confirmation"),
    RESCHEDULING("Rescheduling", "Safari dates being changed or rescheduled"),
    CLIENT_REQUEST("Client Request", "On hold at client's request"),
    PAYMENT_ISSUE("Payment Issue", "Hold due to payment-related concerns"),
    OPERATIONAL_ISSUE("Operational Issue", "Hold due to operational/logistical issues"),
    OTHER("Other", "Other reason (see notes for details)");

    private final String displayName;
    private final String description;

    SafariHoldReason(String displayName, String description) {
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
