package com.itineraryledger.kabengosafaris.ClientPortal.Enums;

/**
 * Who is holding the pen.
 *
 * <p>Exactly one side holds it at any moment, and the handover is always explicit. That is the
 * whole design: a loop where both sides can edit produces two versions of one quotation and no way
 * to say which is current, and a loop that locks on submit produces a client who is stuck waiting
 * because they noticed a mistake one second after sending it.
 *
 * <p>So <strong>submitting does not lock anything</strong>. It is a signal that the client would
 * like us to look, and they keep the pen until a member of staff deliberately takes it. Until that
 * moment they can keep editing and submit again. After it they are read only, and they can see who
 * has it and since when, rather than wondering whether anybody received it.
 *
 * <p>Modelled on {@code InvoiceStatus}, which carries its own transition rules rather than leaving
 * them scattered through a service, and on {@code AvailabilityRequest}, which is the existing
 * request, reply and close loop in this codebase.
 */
public enum ProposalState {

    /** The client may look and change things. Nothing is waiting on us. */
    OPEN_TO_CLIENT("Open to the client", "They are looking at it; we have nothing to do"),

    /**
     * The client has asked us to look, and still holds the pen.
     *
     * <p>They can keep editing and submit again. We are notified, and the panel shows how long it
     * has been waiting.
     */
    SUBMITTED("Submitted", "They have asked us to review it, and can still change it"),

    /**
     * A member of staff has taken it over. The client is read only and is told so.
     *
     * <p>The revision it was taken from is recorded, so an edit that arrives from a page the client
     * had already open is refused rather than quietly overwriting what we are reviewing.
     */
    WITH_OPERATOR("With us", "We are working on it; the client is waiting"),

    /**
     * We have handed it back, with our version or our comments. The client decides.
     *
     * <p>Three ways out: accept, decline, or change something and submit again, which puts it back
     * to OPEN_TO_CLIENT. A conversation, not an ultimatum.
     */
    RETURNED("Returned to the client", "Our answer is with them, to accept, decline or counter"),

    /** The client accepted. The quote itself is moved on by the existing status machine. */
    AGREED("Agreed", "Accepted by the client"),

    /** The client declined. */
    DECLINED("Declined", "Declined by the client");

    private final String displayName;
    private final String description;

    ProposalState(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    /**
     * Whether the client may change their quotation right now.
     *
     * <p>True in SUBMITTED on purpose. That is the requirement: submitting is a signal, not a
     * lock, and the pen stays with them until somebody takes it.
     */
    public boolean clientMayEdit() {
        return this == OPEN_TO_CLIENT || this == SUBMITTED || this == RETURNED;
    }

    /** Whether the client may accept or decline right now. */
    public boolean clientMayDecide() {
        return this == RETURNED || this == OPEN_TO_CLIENT;
    }

    /** Whether a member of staff may take it over. */
    public boolean operatorMayTakeOver() {
        return this == OPEN_TO_CLIENT || this == SUBMITTED || this == RETURNED;
    }

    /** Whether we are the ones holding it up. */
    public boolean waitingOnUs() {
        return this == SUBMITTED || this == WITH_OPERATOR;
    }

    /** Nothing further happens from here without a new quote or a new version. */
    public boolean isFinal() {
        return this == AGREED || this == DECLINED;
    }
}
