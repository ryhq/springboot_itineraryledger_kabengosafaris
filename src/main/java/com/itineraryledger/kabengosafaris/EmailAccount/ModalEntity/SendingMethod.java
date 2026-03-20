package com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity;

/**
 * SendingMethod Enum - Defines how an email account sends emails
 *
 * Supported methods:
 * - API: Send via provider's HTTP API (e.g., Resend REST API on port 443)
 * - SMTP: Send via SMTP protocol (traditional or provider's SMTP gateway)
 */
public enum SendingMethod {
    API("HTTP API"),
    SMTP("SMTP");

    private final String displayName;

    SendingMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
