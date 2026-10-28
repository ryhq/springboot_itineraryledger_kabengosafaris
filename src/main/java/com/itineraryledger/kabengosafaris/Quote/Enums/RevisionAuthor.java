package com.itineraryledger.kabengosafaris.Quote.Enums;

/**
 * Which side of the conversation a revision was taken from.
 *
 * <p>Three values rather than a user id and a null, because the useful question on a change list
 * is "did we do this or did they", and a null user id answers it only by convention. A system
 * revision is one nobody chose to take: the baseline written when a quotation is first shared.
 */
public enum RevisionAuthor {

    /** The customer, through their own link. */
    CLIENT("The customer"),

    /** Somebody signed in to the panel. */
    STAFF("Us"),

    /** Nobody: a baseline the application wrote so there is something to compare against. */
    SYSTEM("The system");

    private final String displayName;

    RevisionAuthor(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
