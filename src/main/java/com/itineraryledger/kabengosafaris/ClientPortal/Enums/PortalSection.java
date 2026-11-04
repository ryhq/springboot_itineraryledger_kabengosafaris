package com.itineraryledger.kabengosafaris.ClientPortal.Enums;

/**
 * Everything that can be written to the access log, and what kind of thing it is.
 *
 * <p>This was a bare string until insight was built on top of it, and the string is why the
 * enum now exists. The first honest question anybody asks of the log is "have they looked at
 * it?", and answering that by counting rows gives the wrong number: the same table holds a
 * consultant freezing the quotation, the office taking the proposal over, and a link being
 * withdrawn. A count that folds our own clicks into the customer's interest is worse than no
 * count, because it looks like an answer.
 *
 * <p>So the kind is attached to the constant rather than inferred later. A new section cannot be
 * added without deciding what it counts as, which is the property that keeps the figures true as
 * the portal grows. {@link #OTHER} exists only for a key read back from a row written by an
 * older version of this application; nothing counts it as anything.
 *
 * <p>The stored column is a varchar holding {@link #name()}, per the house rule about native
 * enums: a database enum is frozen at the values it was created with, and this list will grow.
 */
public enum PortalSection {

    // ----------------------------------------------------------------- they looked at something

    TRIP(Kind.READING, "Opened the trip"),
    QUOTE(Kind.READING, "Read the quotation"),
    INVOICE(Kind.READING, "Read the invoice"),
    DOCUMENTS(Kind.READING, "Opened the documents list"),
    DOCUMENT(Kind.READING, "Opened a document"),
    DEVICES(Kind.READING, "Looked at their devices"),
    PEOPLE(Kind.READING, "Looked at who can see it"),
    PRIVACY(Kind.READING, "Read the privacy notice"),
    CHANGES(Kind.READING, "Read the change history"),
    CATALOGUE(Kind.READING, "Looked for another lodge"),

    // ----------------------------------------------------------------- they changed or decided something

    EDIT_STAY(Kind.DECIDING, "Chose a different lodge we had offered"),
    EDIT_STAY_CATALOGUE(Kind.DECIDING, "Chose a lodge from the catalogue"),
    EDIT_ROOMS(Kind.DECIDING, "Changed how many rooms"),
    EDIT_PARTY(Kind.DECIDING, "Changed who is travelling"),
    EDIT_ACTIVITY_ADDED(Kind.DECIDING, "Added an optional activity"),
    EDIT_ACTIVITY_DROPPED(Kind.DECIDING, "Dropped an optional activity"),
    PROPOSAL_SUBMITTED(Kind.DECIDING, "Asked us to look at it"),
    PROPOSAL_AGREED(Kind.DECIDING, "Agreed to the quotation"),
    PROPOSAL_DECLINED(Kind.DECIDING, "Declined the quotation"),
    COMMENT_ADDED(Kind.DECIDING, "Left a comment"),
    INVITE_SENT(Kind.DECIDING, "Invited somebody to read it"),
    INVITE_WITHDRAWN(Kind.DECIDING, "Took somebody off the booking"),

    // ----------------------------------------------------------------- getting in, and security

    VERIFY_PAGE(Kind.ACCESS, "Opened the verification page"),
    VERIFY_CODE_SENT(Kind.ACCESS, "Asked for a code"),
    VERIFY_OK(Kind.ACCESS, "Verified successfully"),
    VERIFY_FAILED(Kind.ACCESS, "Entered a wrong code"),
    VERIFY_UNKNOWN_ADDRESS(Kind.ACCESS, "Tried an address that is not on this booking"),
    /** Either side can end a device, so this is counted as neither side's activity. */
    DEVICE_REVOKED(Kind.ACCESS, "A device was ended"),

    // ----------------------------------------------------------------- we did it, not them

    /** We emailed them their link. Ours, not theirs: it says nothing about their interest. */
    LINK_EMAILED(Kind.OURS, "Link emailed to the customer"),
    PROPOSAL_TAKEN_OVER(Kind.OURS, "Taken over by us"),
    PROPOSAL_RETURNED(Kind.OURS, "Sent back to the customer"),
    JOURNEY_REVOKED(Kind.OURS, "Link withdrawn"),
    JOURNEY_REOPENED(Kind.OURS, "Link reopened"),
    JOURNEY_LOCKED(Kind.OURS, "Frozen"),
    JOURNEY_UNLOCKED(Kind.OURS, "Unfrozen"),
    JOURNEY_LEVEL(Kind.OURS, "Editing level changed"),

    /** A key from a row this version does not know. Counted as nothing, shown as itself. */
    OTHER(Kind.UNKNOWN, "Something else");

    public enum Kind {
        /** The client looking at what we sent. The only kind that answers "are they interested". */
        READING,
        /** The client changing or deciding something. */
        DECIDING,
        /** Proving who they are, and anything about a trusted browser. */
        ACCESS,
        /** Us, recorded on their journey because it happened to their journey. */
        OURS,
        /** Written before this constant existed. */
        UNKNOWN
    }

    private final Kind kind;
    private final String label;

    PortalSection(Kind kind, String label) {
        this.kind = kind;
        this.label = label;
    }

    public Kind getKind() { return kind; }

    public String getLabel() { return label; }

    public boolean isTheClientLooking() { return kind == Kind.READING; }

    public boolean isTheClientActing() { return kind == Kind.DECIDING; }

    /** Whether this row says anything at all about the customer, as opposed to about us. */
    public boolean isTheirs() { return kind == Kind.READING || kind == Kind.DECIDING; }

    /**
     * The constant for a stored key, or {@link #OTHER}.
     *
     * <p>Never throws. These rows are read back months later, and a log that cannot be displayed
     * because one row holds an unfamiliar word is a log nobody can use.
     */
    public static PortalSection of(String key) {
        if (key == null) return OTHER;
        for (PortalSection section : values()) {
            if (section.name().equals(key)) return section;
        }
        return OTHER;
    }
}
