package com.itineraryledger.kabengosafaris.CreditNote.Enums;

import lombok.Getter;

/**
 * CreditNote workflow states:
 *   DRAFT → CONFIRMED → SENT → CONSUMED
 *
 * On CONSUMED: the credited amount was either returned to the customer
 * or applied to another safari/invoice.
 */
@Getter
public enum CreditNoteStatus {
    DRAFT("Draft", "Credit note is being prepared"),
    CONFIRMED("Confirmed", "Credit note confirmed and approved"),
    SENT("Sent", "Credit note sent to customer"),
    CONSUMED("Consumed", "Credit applied — money returned or used for another safari");

    private final String displayName;
    private final String description;

    CreditNoteStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public boolean isEditable() {
        return this == DRAFT;
    }

    public boolean isDeletable() {
        return this == DRAFT;
    }

    public boolean isFinalState() {
        return this == CONSUMED;
    }

    public boolean canTransitionTo(CreditNoteStatus targetState) {
        switch (this) {
            case DRAFT:
                return targetState == CONFIRMED;
            case CONFIRMED:
                return targetState == SENT || targetState == DRAFT;
            case SENT:
                return targetState == CONSUMED;
            case CONSUMED:
                return false;
            default:
                return false;
        }
    }
}
