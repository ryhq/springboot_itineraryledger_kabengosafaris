package com.itineraryledger.kabengosafaris.Quote.Enums;

/**
 * Why a revision was taken.
 *
 * <p>A snapshot is written when the pen changes hands, never on every edit. Snapshotting each
 * keystroke would produce a history nobody can read and a diff between two states the customer
 * never saw; snapshotting at the handover produces exactly the rounds of the conversation, which
 * is what both sides want to read back.
 *
 * <p>The vocabulary is deliberately the one {@code AvailabilityRequest} already uses for the
 * request, reply and close loop in this codebase, rather than a second set of words for the same
 * shape.
 */
public enum RevisionReason {

    /** The first one, written when the quotation is shared, so there is always a baseline. */
    PUBLISHED("Sent to the customer"),

    /** The client asked us to look at what they have put together. */
    SUBMITTED("Submitted by the customer"),

    /** A member of staff took it over; the client is read only from here. */
    TAKEN_OVER("Taken over by us"),

    /** We handed it back, with our version or our comments. */
    RETURNED("Returned to the customer"),

    /** The client accepted. */
    AGREED("Agreed"),

    /** The client declined. */
    DECLINED("Declined"),

    /** An earlier snapshot was put back, which is itself a change worth recording. */
    RESTORED("Restored from an earlier version");

    private final String displayName;

    RevisionReason(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
