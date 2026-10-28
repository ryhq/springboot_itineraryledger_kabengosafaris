package com.itineraryledger.kabengosafaris.ClientPortal.DTOs;

import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;

/**
 * What happened, and the sentence to show for it.
 *
 * <p>Not a bare boolean, for the reason {@code QuoteEditPolicy.Decision} is not: the refusal is
 * the useful half. "We have already started reviewing this" tells somebody what to expect;
 * "not allowed" tells them to ring up, which is the telephone call the portal exists to avoid.
 *
 * <p>Shared between the proposal loop and the client's own edits because they are the same
 * answer to the same shape of question, and because a client who submits and a client who swaps
 * a lodge are refused for exactly the same reasons.
 */
public record PortalOutcome(boolean done, String message, String code, PortalJourney journey) {

    public static PortalOutcome ok(String message, PortalJourney journey) {
        return new PortalOutcome(true, message, null, journey);
    }

    public static PortalOutcome refuse(String message, String code, PortalJourney journey) {
        return new PortalOutcome(false, message, code, journey);
    }
}
