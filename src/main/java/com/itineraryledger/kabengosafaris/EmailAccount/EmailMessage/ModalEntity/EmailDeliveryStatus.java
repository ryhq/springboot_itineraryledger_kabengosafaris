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
    COMPLAINED
}
