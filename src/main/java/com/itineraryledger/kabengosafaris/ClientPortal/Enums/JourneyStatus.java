package com.itineraryledger.kabengosafaris.ClientPortal.Enums;

/**
 * Whether a journey link still works at all.
 *
 * <p>Separate from the proposal state, which says where the conversation stands, and separate from
 * the quote's own status, which says where the paperwork stands. Three different questions, and
 * conflating any two of them is how a client ends up locked out of a trip they are still going on,
 * or reading a document that was withdrawn.
 */
public enum JourneyStatus {

    /** The link works. The usual state for the whole life of a booking. */
    OPEN("Open", "The client can open their link"),

    /**
     * The trip is over or the booking fell through. The link still opens, because a client looks
     * up their own invoice months later, but nothing can be proposed or accepted and the
     * behavioural data is on its way out.
     */
    CLOSED("Closed", "Read only: the trip has finished or the booking did not go ahead"),

    /**
     * Withdrawn by the office. The link opens nothing at all.
     *
     * <p>This is the control that matters when something has gone wrong: a forwarded link, an
     * inbox somebody else is reading, a request that turned out not to be from the customer.
     */
    REVOKED("Revoked", "The link has been withdrawn and opens nothing");

    private final String displayName;
    private final String description;

    JourneyStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    /** Whether the client may still be shown anything. */
    public boolean opensAnything() {
        return this != REVOKED;
    }

    /** Whether the client may still change or decide anything. */
    public boolean allowsAction() {
        return this == OPEN;
    }
}
