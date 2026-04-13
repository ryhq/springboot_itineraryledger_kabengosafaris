package com.itineraryledger.kabengosafaris.CreditNote.Enums;

import lombok.Getter;

@Getter
public enum CreditNoteItemType {
    ACCOMMODATION("Accommodation", "Credited accommodation"),
    PARK_FEE("Park Fee", "Credited park fees"),
    ACTIVITY("Activity", "Credited activity"),
    TRANSPORT("Transport", "Credited transport"),
    OVERCHARGE("Overcharge", "Overcharge correction"),
    SERVICE_ISSUE("Service Issue", "Credit for service quality issue"),
    OTHER("Other", "Other credit reason");

    private final String displayName;
    private final String description;

    CreditNoteItemType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
