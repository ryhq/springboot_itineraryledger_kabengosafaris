package com.itineraryledger.kabengosafaris.ContactMessage.Entity;

public enum ContactMessageStatus {
    NEW("New", "Message received, not yet read"),
    READ("Read", "Message has been read by admin"),
    RESPONDED("Responded", "Admin has responded to the message"),
    ARCHIVED("Archived", "Message has been archived");

    private final String displayName;
    private final String description;

    ContactMessageStatus(String displayName, String description) {
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
