package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity;

/**
 * Lifecycle status of an outgoing email, driven by Resend webhook events.
 *
 * <p>{@link #SENT} is the initial state set when we hand the message to the
 * provider — at that point we know it left our process, nothing more. The
 * provider then asynchronously reports delivery via webhook, which advances
 * the row through one of the terminal states ({@link #DELIVERED},
 * {@link #BOUNCED}, {@link #COMPLAINED}). {@link #DELIVERY_DELAYED} is a
 * transient hint that the provider is retrying; the row will land on a
 * terminal state once retries succeed or exhaust.
 *
 * <p>Used only for messages sent through the Resend API. Inbound /
 * IMAP-fetched messages leave the field null.
 */
public enum EmailDeliveryStatus {
    SENT,
    DELIVERY_DELAYED,
    DELIVERED,
    BOUNCED,
    COMPLAINED;

    /**
     * How loudly this status asks somebody to do something about it.
     *
     * <p>One row carries one status, but a single send can have several
     * recipients and therefore several outcomes, and a provider may report a
     * delivery and then a bounce for the same message. When outcomes disagree,
     * the one worth showing is the one that needs acting on: nobody chases a
     * message marked delivered, so a bounce hidden behind a delivery is a
     * bounce nobody answers.
     *
     * <p>Written as a switch rather than {@link #ordinal()} on purpose. The
     * constants happen to be declared in this order today, and somebody
     * reordering them for readability must not silently change which outcome
     * a customer-facing screen reports.
     */
    public int precedence() {
        switch (this) {
            case SENT:             return 0;
            case DELIVERY_DELAYED: return 1;
            case DELIVERED:        return 2;
            case BOUNCED:          return 3;
            case COMPLAINED:       return 4;
            default:               return 0;
        }
    }

    /**
     * The status a row should hold once {@code incoming} is reported on a row
     * currently holding {@code current}.
     *
     * <p>Webhooks arrive out of order, so this deliberately does not trust
     * arrival sequence: the more actionable outcome wins whichever order the
     * two events turn up in. A late {@code delivery_delayed} cannot walk a
     * delivered row backwards, and a late {@code delivered} cannot bury a
     * bounce.
     */
    public static EmailDeliveryStatus resolve(EmailDeliveryStatus current, EmailDeliveryStatus incoming) {
        if (incoming == null) return current;
        if (current == null) return incoming;
        return incoming.precedence() >= current.precedence() ? incoming : current;
    }
}
