package com.itineraryledger.kabengosafaris.Invoice;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;

/**
 * A wrong figure belongs on the invoice, not in a second document arguing with it.
 *
 * <p>An invoice froze the moment it left DRAFT. A client queried her Zanzibar nights, was right,
 * and the only instrument available was a credit note: a separate document telling her the invoice
 * she was holding was wrong, for a balance she had not yet paid. The invoice itself went on
 * stating the wrong total, which is the figure she reads and the figure she pays against. Worse,
 * a credit note in this system adjusts nothing — consuming one stamps a method and leaves the
 * invoice balance exactly where it was — so the paperwork and the ledger would have disagreed
 * until somebody reconciled them by hand.
 *
 * <p>ON_HOLD unlocks the invoice in place. The rules that matter are small and easy to get wrong
 * in the opposite direction, which is why they are pinned here: a held invoice is editable, a paid
 * one can still be corrected, a draft is not something you hold, and nothing about the hold makes
 * an invoice deletable.
 */
class AnInvoiceCanBeCorrectedWithoutACreditNoteTest {

    @Test
    @DisplayName("a held invoice is editable; a part-paid one still is not")
    void holdingIsWhatUnlocksIt() {
        assertFalse(InvoiceStatus.PARTIALLY_PAID.isEditable(),
            "a part-paid invoice must stay locked until somebody deliberately holds it");
        assertFalse(InvoiceStatus.PAID.isEditable());
        assertFalse(InvoiceStatus.OVERDUE.isEditable());
        assertFalse(InvoiceStatus.CANCELLED.isEditable());

        assertTrue(InvoiceStatus.ON_HOLD.isEditable(), "holding an invoice is the whole point");
        assertTrue(InvoiceStatus.DRAFT.isEditable());
    }

    @Test
    @DisplayName("every invoice that has left DRAFT can be held, and DRAFT cannot")
    void holdIsForInvoicesTheCustomerAlreadyHas() {
        Set<InvoiceStatus> holdable = EnumSet.of(
            InvoiceStatus.SENT, InvoiceStatus.PARTIALLY_PAID,
            InvoiceStatus.OVERDUE, InvoiceStatus.PAID);

        for (InvoiceStatus status : InvoiceStatus.values()) {
            if (holdable.contains(status)) {
                assertTrue(status.canBeHeld(), status + " is a document the customer holds, so it must be correctable");
                assertTrue(status.canTransitionTo(InvoiceStatus.ON_HOLD),
                    status + " claims it can be held but the transition table disagrees");
            } else {
                assertFalse(status.canBeHeld(), status + " must not be holdable");
            }
        }

        assertFalse(InvoiceStatus.DRAFT.canBeHeld(), "a draft is edited directly; holding it means nothing");
        assertFalse(InvoiceStatus.CANCELLED.canBeHeld(), "a cancelled invoice is not corrected, it is replaced");
    }

    @Test
    @DisplayName("a held invoice releases only into states the payments can justify")
    void releaseGoesBackIntoTheWorkflow() {
        for (InvoiceStatus target : EnumSet.of(
                InvoiceStatus.SENT, InvoiceStatus.PARTIALLY_PAID,
                InvoiceStatus.PAID, InvoiceStatus.OVERDUE)) {
            assertTrue(InvoiceStatus.ON_HOLD.canTransitionTo(target),
                "release must be able to land on " + target);
        }

        assertTrue(InvoiceStatus.ON_HOLD.canTransitionTo(InvoiceStatus.CANCELLED),
            "a correction can end in the invoice being abandoned");
        assertFalse(InvoiceStatus.ON_HOLD.canTransitionTo(InvoiceStatus.DRAFT),
            "an invoice the customer has seen never goes back to being a draft");
        assertFalse(InvoiceStatus.ON_HOLD.canTransitionTo(InvoiceStatus.ON_HOLD),
            "holding a held invoice is a no-op the caller should be told about");
    }

    @Test
    @DisplayName("holding an invoice does not make it deletable")
    void correctableIsNotDisposable() {
        assertFalse(InvoiceStatus.ON_HOLD.isDeletable(),
            "a held invoice has payments against it; only a draft may be destroyed");
        assertTrue(InvoiceStatus.DRAFT.isDeletable());

        for (InvoiceStatus status : InvoiceStatus.values()) {
            if (status != InvoiceStatus.DRAFT) {
                assertFalse(status.isDeletable(), status + " must not be deletable");
            }
        }
    }

    @Test
    @DisplayName("a held invoice is not a final state")
    void aHeldInvoiceIsWorkInProgress() {
        assertFalse(InvoiceStatus.ON_HOLD.isFinalState(),
            "somebody is working on it; nothing downstream may treat it as settled");
        assertFalse(InvoiceStatus.ON_HOLD.isPaymentState(),
            "the hold says nothing about the money, which is exactly why it is safe");
        assertTrue(InvoiceStatus.ON_HOLD.isHeld());
    }
}
