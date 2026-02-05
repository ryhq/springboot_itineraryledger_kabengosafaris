package com.itineraryledger.kabengosafaris.Quote.Enums;

import lombok.Getter;

/**
 * Status enum for Quote workflow states
 */
@Getter
public enum QuoteStatus {
    DRAFT("Draft", "Quote is being prepared"),
    PENDING_REVIEW("Pending Review", "Quote submitted for internal review"),
    APPROVED("Approved", "Quote approved internally, ready to send"),
    SENT("Sent", "Quote sent to customer"),
    CUSTOMER_REVIEWING("Customer Reviewing", "Customer is reviewing the quote"),
    CUSTOMER_REQUESTED_CHANGES("Customer Requested Changes", "Customer requested modifications"),
    REVISED("Revised", "Quote has been revised based on feedback"),
    ACCEPTED("Accepted", "Customer accepted the quote"),
    REJECTED("Rejected", "Customer rejected the quote"),
    EXPIRED("Expired", "Quote validity period has passed"),
    CANCELLED("Cancelled", "Quote cancelled"),
    CONVERTED("Converted", "Quote converted to booking/invoice");

    private final String displayName;
    private final String description;

    QuoteStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
