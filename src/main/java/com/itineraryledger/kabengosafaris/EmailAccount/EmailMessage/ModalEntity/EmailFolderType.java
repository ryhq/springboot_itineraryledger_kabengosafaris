package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity;

public enum EmailFolderType {
    INBOX("Inbox"),
    SENT("Sent"),
    DRAFTS("Drafts"),
    TRASH("Trash"),
    ARCHIVE("Archive"),
    SPAM("Spam"),
    CUSTOM("Custom");

    private final String displayName;

    EmailFolderType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
