package com.itineraryledger.kabengosafaris.EmailAccount;

import static com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailDeliveryStatus.BOUNCED;
import static com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailDeliveryStatus.COMPLAINED;
import static com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailDeliveryStatus.DELIVERED;
import static com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailDeliveryStatus.DELIVERY_DELAYED;
import static com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailDeliveryStatus.SENT;
import static com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailDeliveryStatus.resolve;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailDeliveryStatus;

/**
 * A message that bounced must not read "Delivered", however the webhooks arrived.
 *
 * <p>Written from a live fault. A send to several recipients produces ONE EmailMessage row and
 * SEVERAL outcomes: the provider reports a delivery for the recipients who got it and a bounce
 * for the one who did not. The old rule was {@code if (current != DELIVERED) setBounced()}, so
 * whichever event landed second lost, and in practice the delivery landed first. The row said
 * "Delivered" and the bounce survived only in the webhook event table, which nothing reads.
 *
 * <p>It hid a real failure for eight months. One address soft-bounced every message sent to it
 * from 2026-03 onwards — 363 of them, including supplier availability replies and a flight
 * inquiry — while every one of those rows reported Delivered. Nobody chases a message marked
 * delivered, which is exactly why a bounce behind a delivery is the expensive kind to lose.
 *
 * <p>The same code had always let a COMPLAINT override a delivery, with the comment "Complaints
 * can follow a delivery". Bounces can follow a delivery for precisely the same reason; only the
 * bounce branch was written as though they could not.
 */
@DisplayName("A bounce is not buried by a delivery")
class ABounceIsNotBuriedByADeliveryTest {

    @Test
    @DisplayName("the fault: delivered first, then bounced, still reads BOUNCED")
    void bounceAfterDeliveryWins() {
        assertEquals(BOUNCED, resolve(DELIVERED, BOUNCED));
    }

    @Test
    @DisplayName("and the reverse order agrees, because arrival order is not evidence")
    void deliveryAfterBounceDoesNotBuryIt() {
        assertEquals(BOUNCED, resolve(BOUNCED, DELIVERED));
    }

    @Test
    @DisplayName("a complaint still overrides anything, as it always did")
    void complaintWins() {
        assertEquals(COMPLAINED, resolve(DELIVERED, COMPLAINED));
        assertEquals(COMPLAINED, resolve(BOUNCED, COMPLAINED));
        assertEquals(COMPLAINED, resolve(SENT, COMPLAINED));
    }

    @Test
    @DisplayName("nothing walks a complaint backwards")
    void complaintIsNotOverwritten() {
        assertEquals(COMPLAINED, resolve(COMPLAINED, DELIVERED));
        assertEquals(COMPLAINED, resolve(COMPLAINED, BOUNCED));
        assertEquals(COMPLAINED, resolve(COMPLAINED, DELIVERY_DELAYED));
    }

    @Test
    @DisplayName("a late delivery_delayed does not walk a settled row backwards")
    void lateDelayDoesNotRegress() {
        assertEquals(DELIVERED, resolve(DELIVERED, DELIVERY_DELAYED));
        assertEquals(BOUNCED, resolve(BOUNCED, DELIVERY_DELAYED));
    }

    @Test
    @DisplayName("a late sent event does not undo a known outcome")
    void lateSentDoesNotRegress() {
        assertEquals(DELIVERED, resolve(DELIVERED, SENT));
        assertEquals(BOUNCED, resolve(BOUNCED, SENT));
        assertEquals(DELIVERY_DELAYED, resolve(DELIVERY_DELAYED, SENT));
    }

    @Test
    @DisplayName("a row with no status yet takes whatever arrives")
    void firstEventSetsTheRow() {
        assertEquals(SENT, resolve(null, SENT));
        assertEquals(DELIVERED, resolve(null, DELIVERED));
        assertEquals(BOUNCED, resolve(null, BOUNCED));
        assertNull(resolve(null, null));
    }

    @Test
    @DisplayName("an unrecognised event leaves the row alone")
    void unknownEventKeepsCurrent() {
        assertEquals(DELIVERED, resolve(DELIVERED, null));
        assertEquals(BOUNCED, resolve(BOUNCED, null));
    }

    @Test
    @DisplayName("precedence is declared, not inherited from declaration order")
    void precedenceIsExplicit() {
        // Someone reordering the constants for readability must not change which
        // outcome a customer-facing screen reports, so precedence() is a switch
        // rather than ordinal(). This pins the intended ranking.
        assertEquals(0, SENT.precedence());
        assertEquals(1, DELIVERY_DELAYED.precedence());
        assertEquals(2, DELIVERED.precedence());
        assertEquals(3, BOUNCED.precedence());
        assertEquals(4, COMPLAINED.precedence());

        for (EmailDeliveryStatus s : EmailDeliveryStatus.values()) {
            assertEquals(s, resolve(s, s), "a repeated event must not change the row");
        }
    }
}
