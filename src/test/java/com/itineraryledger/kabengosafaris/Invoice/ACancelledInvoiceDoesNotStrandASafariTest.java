package com.itineraryledger.kabengosafaris.Invoice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cancelling an invoice must not leave a safari that can never be billed.
 *
 * <p>Three rules met and made a trap. {@code safari_id} carries a unique index - one invoice row
 * per safari. The generator's guard deliberately let a <b>cancelled</b> invoice through, promising
 * that cancel-and-reissue works. And delete refused anything that was not a DRAFT.
 *
 * <p>So cancelling an invoice to correct it put the safari beyond reach: the replacement could not
 * be inserted, because the cancelled row still held the slot, and the cancelled row could not be
 * removed to make room. SAF-8D7N-01002 reached exactly that state on 15 November 2026 while
 * reissuing an invoice that had billed TZS as USD. The only symptom was <i>"An unexpected error
 * occurred"</i> - a constraint violation escaping on commit, after the service's own catch.
 *
 * <p>The delete rule's reason was always about <b>sending</b>, not about status: once a customer
 * holds a copy, deleting our half leaves a demand with no record behind it. A cancelled invoice
 * that was never sent has no copy anywhere. The rule now tests that instead.
 */
class ACancelledInvoiceDoesNotStrandASafariTest {

    private static final Path DELETE = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/Invoice/Services/"
            + "InvoiceServices/InvoiceDeleteService.java");
    private static final Path GENERATE = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/Invoice/Services/"
            + "InvoiceServices/InvoiceFromSafariGenerationService.java");

    @Test
    @DisplayName("a cancelled invoice that was never sent can be deleted")
    void aCancelledUnsentInvoiceIsRemovable() throws IOException {
        String body = Files.readString(DELETE);

        assertTrue(body.contains("InvoiceStatus.CANCELLED && !everSent"),
            "a cancelled invoice that never reached the customer must be deletable, or the safari "
                + "it belongs to can never be invoiced again");
        assertTrue(body.contains("invoice.getSentDate() != null"),
            "the test must be whether it was SENT. That was always the reason for the rule; "
                + "status was only ever a proxy for it.");
    }

    @Test
    @DisplayName("an invoice the customer holds is still protected")
    void aSentInvoiceIsStillProtected() throws IOException {
        String body = Files.readString(DELETE);
        assertTrue(body.contains("It was sent to the customer on"),
            "a sent invoice must still refuse deletion, and say when it was sent rather than "
                + "quoting a status the reader has to interpret");
    }

    @Test
    @DisplayName("generating over a cancelled invoice explains itself instead of throwing")
    void theOccupiedSlotIsNamed() throws IOException {
        String body = Files.readString(GENERATE);

        assertTrue(body.contains("existsBySafariId(safari.getId())"),
            "the generator must notice that a cancelled invoice still occupies the safari, "
                + "rather than letting the unique index reject the insert on commit");
        assertTrue(body.contains("CANCELLED_INVOICE_OCCUPIES_SAFARI"),
            "the refusal needs a code and a remedy. 'An unexpected error occurred' tells the "
                + "person nothing about the cancelled invoice in the way.");
    }
}
