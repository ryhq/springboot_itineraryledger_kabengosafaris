package com.itineraryledger.kabengosafaris.Newsletter.Entity;

public enum SubscriptionStatus {
    ACTIVE("Active", "Subscriber is actively receiving newsletters"),
    UNSUBSCRIBED("Unsubscribed", "Subscriber has opted out of newsletters"),
    BOUNCED("Bounced", "Email address is invalid or unreachable");

    private final String displayName;
    private final String description;

    SubscriptionStatus(String displayName, String description) {
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
