package com.itineraryledger.kabengosafaris.ClientPortal.Enums;

/**
 * Whether this person speaks for the booking, or is only reading it.
 *
 * <p>A group trip has one organiser and several travellers who all want to see the itinerary and
 * what they owe. They should be able to, without each of them being able to accept a quotation on
 * everybody else's behalf: the contract is with one person, and a booking accepted by somebody who
 * was not authorised to accept it is a problem no software can unpick afterwards.
 */
public enum ParticipantRole {

    /** The person we are dealing with. May propose, submit, accept, decline and invite. */
    LEAD("Lead traveller", "Can propose changes, accept or decline, and invite others to read"),

    /**
     * Invited by the lead. Sees everything the lead sees, including the change history, and
     * decides nothing.
     */
    READER("Reader", "Can read the trip, the quotation and what is owed. Cannot change or decide");

    private final String displayName;
    private final String description;

    ParticipantRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    public boolean mayAct() {
        return this == LEAD;
    }
}
