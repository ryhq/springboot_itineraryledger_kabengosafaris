package com.itineraryledger.kabengosafaris.Inclusion.Entity;

/**
 * Whether a document's promise is still its parent's, or has been changed here.
 *
 * <p>Without this, a quote, the safari it became and the invoice that bills it can each hold a
 * different statement of what the price covers, with nothing on screen revealing that they
 * diverged. It is the same discipline as putting a date on a calculated total.
 *
 * <p>It is also what makes Reset honest: a reset is only worth offering if somebody can see that
 * there is something to reset from.
 */
public enum InclusionsSource {

    /** Unchanged since it was copied — resetting would do nothing. */
    INHERITED("Inherited", "Still exactly what the parent document said"),

    /** Changed on this document. Resetting discards those changes and re-copies the parent's. */
    EDITED("Edited here", "Changed on this document, so it no longer matches the parent");

    private final String displayName;
    private final String description;

    InclusionsSource(String displayName, String description) {
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
