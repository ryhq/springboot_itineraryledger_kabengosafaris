package com.itineraryledger.kabengosafaris.Newsletter.Entity;

public enum SubscriptionStatus {
    /**
     * They typed the address; nobody has proved they own it yet.
     *
     * <p>Nothing is ever sent to an address in this state except the one email asking them to
     * confirm. That is the law for European subscribers, and it is also what keeps the sending
     * domain clean: a list built from unconfirmed addresses collects bounces and complaints, and
     * the reputation that costs is paid for by every quote the company emails afterwards.
     */
    PENDING_CONFIRMATION("Awaiting confirmation", "Subscriber has not yet confirmed the address"),
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
