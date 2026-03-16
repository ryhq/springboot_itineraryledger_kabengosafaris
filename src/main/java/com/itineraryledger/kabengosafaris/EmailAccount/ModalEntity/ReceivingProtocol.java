package com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity;

public enum ReceivingProtocol {
    IMAP("IMAP"),
    POP3("POP3"),
    NONE("None");

    private final String displayName;

    ReceivingProtocol(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
