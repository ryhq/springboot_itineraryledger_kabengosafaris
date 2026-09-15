package com.itineraryledger.kabengosafaris.Attribution;

/**
 * Where a lead came from, coarse enough to spend money against.
 *
 * Derived on the server from the tags the browser collected, never sent by the
 * browser itself: the raw tags are whatever was in the URL, and a channel that a
 * visitor could set by hand is a channel nobody can budget against. Deriving it
 * here also means the rule can be corrected and old rows re-derived, which is not
 * true of anything the page decided.
 */
public enum AcquisitionChannel {

    AI_ASSISTANT("AI Assistant", "Cited by ChatGPT, Perplexity, Copilot and the like"),
    PAID_SEARCH("Paid Search", "Google Ads, Microsoft Ads"),
    PAID_SOCIAL("Paid Social", "Meta, TikTok, LinkedIn and other paid placements"),
    PAID_DISPLAY("Paid Display", "Banner and programmatic placements"),
    ORGANIC_SEARCH("Organic Search", "Unpaid results on a search engine"),
    ORGANIC_SOCIAL("Organic Social", "Unpaid posts and shares"),
    EMAIL("Email", "Newsletters and campaign mail"),
    AFFILIATE("Affiliate", "Partner and commission links"),
    REFERRAL("Referral", "A link on somebody else's site"),
    DIRECT("Direct", "Typed the address or used a bookmark"),
    OTHER("Other", "Tagged, but not as anything we recognise");

    private final String displayName;
    private final String description;

    AcquisitionChannel(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /** True when the click was bought, which is the set worth costing. */
    public boolean isPaid() {
        return this == PAID_SEARCH || this == PAID_SOCIAL || this == PAID_DISPLAY;
    }
}
