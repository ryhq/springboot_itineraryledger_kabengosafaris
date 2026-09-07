package com.itineraryledger.kabengosafaris.Expense;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The advice a supplier receives has to be true, and it has to be sent on purpose.
 *
 * <p>A payment advice is read by somebody matching money to a reservation. Two ways it can do harm:
 * by claiming a bill is settled when it is not, so a supplier stops chasing a balance they are
 * owed; and by arriving twice for one transfer, so somebody has to ring to find out which is real.
 */
class PaymentAdviceTellsTheTruthTest {

    private static final Path SERVICE = Path.of("src/main/java/com/itineraryledger/kabengosafaris/"
        + "Expense/Services/ExpensePaymentServices/ExpensePaymentAdviceService.java");
    private static final Path TEMPLATE = Path.of("src/main/resources/templates/email-templates/"
        + "send_payment_advice_default.html");
    private static final Path SCHEMA = Path.of("src/main/resources/schemas/email-events/"
        + "payment-advice-schema.json");

    @Test
    @DisplayName("what is still owing comes from the bill's own balances, not a second sum")
    void theBalanceHasOneSource() throws IOException {
        String source = Files.readString(SERVICE);
        assertTrue(source.contains("aggregation.computeBalances(bill)"),
            "the balance must come from the same service the bill's own screen reads, or the two "
                + "figures will one day disagree and the supplier will hold the wrong one");
        assertTrue(source.contains("settled ? \"Paid in full\" : \"Part payment\""),
            "the heading has to follow the balance rather than the fact that a payment happened");
    }

    @Test
    @DisplayName("it is sent by hand, never fired by a payment being recorded")
    void nothingSendsItAutomatically() throws IOException {
        /*
         * Recording a payment must not email the supplier: correcting a mistyped reference would
         * tell them a second time about money that only moved once. The advice hangs off its own
         * endpoint, and the create service must know nothing about it.
         */
        String create = Files.readString(Path.of("src/main/java/com/itineraryledger/kabengosafaris/"
            + "Expense/Services/ExpensePaymentServices/ExpensePaymentCreateService.java"));
        assertFalse(create.contains("AdviceService") || create.contains("SEND_PAYMENT_ADVICE"),
            "recording a payment must not send the advice; a correction would send it again");

        String controller = Files.readString(Path.of("src/main/java/com/itineraryledger/"
            + "kabengosafaris/Expense/Controller/ExpensePaymentController.java"));
        assertTrue(controller.contains("@PostMapping(\"/{paymentId}/advice\")"),
            "it needs a door of its own");
    }

    @Test
    @DisplayName("no recipient is refused out loud, not answered with a cheerful 200")
    void sendingToNobodyIsAnError() throws IOException {
        String source = Files.readString(SERVICE);
        assertTrue(source.contains("NO_VENDOR_EMAIL"),
            "a vendor with no address must produce an error naming the vendor, not a success that "
                + "sent nothing");
        assertTrue(source.contains("PAYMENT_ADVICE_SEND_FAILED"),
            "every address failing is a failure, however many were tried");
    }

    @Test
    @DisplayName("the letter never carries our account number")
    void weDoNotPublishOurOwnBankDetails() throws IOException {
        String source = Files.readString(SERVICE);
        assertTrue(source.contains("getAccountName()"),
            "a supplier reconciling needs the account's NAME");
        assertFalse(source.contains("getAccountNumber()") || source.contains("getIban("),
            "and not its number: this letter leaves the company");
    }

    @Test
    @DisplayName("every variable the letter uses is declared in the schema")
    void theTemplateAndItsSchemaAgree() throws IOException {
        String template = Files.readString(TEMPLATE);
        String schema = Files.readString(SCHEMA);

        /* the event's own variables; company placeholders are resolved by a later pass */
        for (String name : new String[] {
            "vendorName", "settlement", "amountPaid", "paidOn", "method", "reference", "paidFrom",
            "billCode", "billTitle", "billDescription", "vendorReference", "billTotal",
            "balanceRemaining", "safariName", "safariCode", "notes"}) {
            assertTrue(template.contains("{{" + name + "}}"),
                name + " is declared but the default letter never shows it");
            assertTrue(schema.contains("\"" + name + "\""),
                name + " is used by the letter but is not in the schema, so the editor will not "
                    + "offer it and nobody writing a template will know it exists");
        }

        assertFalse(template.contains("<style"),
            "a mail client discards a style block, and one containing {{ is discarded outright");
    }
}
