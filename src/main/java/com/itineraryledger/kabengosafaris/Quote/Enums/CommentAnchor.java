package com.itineraryledger.kabengosafaris.Quote.Enums;

/**
 * What a comment is about.
 *
 * <p>The whole point of the feature. Without an anchor a client's only way to say "that lodge is
 * too far from the gate" is to decline the quotation or to write an email, and both throw away
 * which part they meant; somebody here then reads "not quite right" and has to ask. Anchoring is
 * what turns a verdict into a conversation.
 */
public enum CommentAnchor {

    /** About the quotation as a whole: the price, the dates, the shape of it. */
    WHOLE("The whole quotation"),

    /** About one day. Held by day number, because day rows are replaced and days are not. */
    DAY("A day"),

    /** About one line on one day: this lodge, this activity. */
    LINE("One thing on a day");

    private final String displayName;

    CommentAnchor(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
