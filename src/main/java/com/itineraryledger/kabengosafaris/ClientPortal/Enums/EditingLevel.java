package com.itineraryledger.kabengosafaris.ClientPortal.Enums;

/**
 * How far into their own quotation a client may reach.
 *
 * <p>Resolved company, then itinerary, then quotation, most specific winning. A honeymoon product
 * and a fixed group departure do not deserve the same freedom, and one suspicious request should
 * be closable on its own without touching either.
 *
 * <p>What a client edits is always their own quotation, never the itinerary. The itinerary is the
 * blueprint the next client will be sold; the quotation already carries a full copy of it, which
 * is exactly why it can be the sandbox.
 *
 * <p><strong>There is no level that rebuilds the trip.</strong> Adding, removing or reordering
 * days would put the quotation's day tree out of step with the itinerary it was snapshotted from,
 * and a client cannot be left holding an incoherent copy. They suggest it in words instead and we
 * build it. A suggestion is a sentence; a broken day tree is an afternoon.
 */
public enum EditingLevel {

    /** Look, and answer. Accept, decline, ask for a discount, leave a note. */
    RESPOND("Respond only", "View and answer. No changes."),

    /**
     * Swap between the options we already recorded, and change the party.
     *
     * <p>The default. Nothing here exposes anything the client was not already shown, because
     * every alternative is already on their quotation and already priced.
     */
    CHOOSE_OFFERED("Choose from what is offered",
        "Swap a night to an alternative already on the quotation, include or drop optional "
            + "activities, change rooms and party size."),

    /**
     * Choose from the catalogue, within the route.
     *
     * <p>Any accommodation we hold a rate for in that place on that date. The route and the days
     * stay as they are.
     *
     * <p>Worth deciding product by product rather than leaving on: anybody who can request a
     * quotation can then read which lodges we work with, in which season, at what price.
     */
    CHOOSE_CATALOGUE("Choose from the catalogue",
        "Any accommodation we hold a rate for in that place on that date. Route and days fixed.");

    private final String displayName;
    private final String description;

    EditingLevel(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    /** Whether this level lets the client change anything at all about the trip. */
    public boolean allowsChanges() {
        return this != RESPOND;
    }

    /**
     * Whether this level can be chosen yet.
     *
     * <p>CHOOSE_CATALOGUE is designed and named and is not built: the portal-safe search over
     * accommodation rates that it needs is phase three. Until then it is accepted by nothing
     * and behaves exactly like CHOOSE_OFFERED, which is the worst possible state for a setting
     * — an operator sets it, believes they have opened the catalogue to a client, and nothing
     * whatever has happened.
     *
     * <p>So it cannot be set. This codebase's rule about facets applies to settings too: one
     * that does nothing is worse than one that is absent, because the absent one is at least
     * honest about it.
     *
     * <p><strong>To turn it on, flip this and nothing else.</strong>
     * {@code TheCatalogueLevelIsNotOfferedUntilItWorksTest} fails the moment anything starts
     * consulting {@link #allowsCatalogue()}, so phase three cannot forget.
     */
    public boolean isAvailable() {
        return this != CHOOSE_CATALOGUE;
    }

    /** Whether the client may reach past the alternatives already on their quotation. */
    public boolean allowsCatalogue() {
        return this == CHOOSE_CATALOGUE;
    }
}
