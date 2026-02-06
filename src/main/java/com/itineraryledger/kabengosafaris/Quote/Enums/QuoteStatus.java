package com.itineraryledger.kabengosafaris.Quote.Enums;

import lombok.Getter;

/**
 * Status enum for Quote workflow states
 *
 * Simplified workflow (8 states):
 * DRAFT → READY → SENT → [ACCEPTED/REJECTED/EXPIRED] → [CONVERTED/CANCELLED]
 */
@Getter
public enum QuoteStatus {
    DRAFT("Draft", "Quote is being prepared"),
    READY("Ready", "Quote is complete and ready to send to customer"),
    SENT("Sent", "Quote sent to customer for review"),
    ACCEPTED("Accepted", "Customer accepted the quote"),
    REJECTED("Rejected", "Customer rejected the quote"),
    EXPIRED("Expired", "Quote validity period has passed"),
    CANCELLED("Cancelled", "Quote cancelled by company"),
    CONVERTED("Converted", "Quote converted to booking/safari");

    private final String displayName;
    private final String description;

    QuoteStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
