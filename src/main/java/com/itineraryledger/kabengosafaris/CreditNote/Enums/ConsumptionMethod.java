package com.itineraryledger.kabengosafaris.CreditNote.Enums;

import lombok.Getter;

/**
 * How the credit note amount was consumed/used.
 */
@Getter
public enum ConsumptionMethod {
    REFUNDED("Refunded", "Money returned to customer"),
    APPLIED_TO_INVOICE("Applied to Invoice", "Credit applied to another invoice"),
    APPLIED_TO_SAFARI("Applied to Safari", "Credit applied to another safari booking");

    private final String displayName;
    private final String description;

    ConsumptionMethod(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
