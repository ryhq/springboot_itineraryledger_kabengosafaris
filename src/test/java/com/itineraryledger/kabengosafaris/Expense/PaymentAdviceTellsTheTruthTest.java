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
    @DisplayName("nothing here sends: the letter is rendered and the mailbox sends it")
    void theServiceRendersRatherThanSends() throws IOException {
        /*
         * The first version of this posted the advice straight out of a drawer. It left no trace
         * anybody could search, the person sending never saw what went, and a supplier's reply had
         * nothing to thread under. Now it renders a letter and the composer sends it, exactly as a
         * request for availability does.
         */
        String source = Files.readString(SERVICE);
        assertFalse(source.contains("EmailSendingService") || source.contains("sendHtmlEmail("),
            "this service must not be able to send. The composer sends, so the advice lands in "
                + "Sent and can be found again");
        assertTrue(source.contains("public ResponseEntity<ApiResponse<?>> letter("),
            "it renders a letter for the composer to carry");

        String controller = Files.readString(Path.of("src/main/java/com/itineraryledger/"
            + "kabengosafaris/Expense/Controller/ExpensePaymentController.java"));
        assertTrue(controller.contains("@GetMapping(\"/{paymentId}/advice\")"),
            "a GET, because asking for the letter changes nothing");
        assertFalse(controller.contains("@PostMapping(\"/{paymentId}/advice\")"),
            "two ways to send the same letter is how the two come to disagree");

        /* and recording a payment still must not tell the supplier anything */
        String create = Files.readString(Path.of("src/main/java/com/itineraryledger/kabengosafaris/"
            + "Expense/Services/ExpensePaymentServices/ExpensePaymentCreateService.java"));
        assertFalse(create.contains("AdviceService") || create.contains("SEND_PAYMENT_ADVICE"),
            "recording a payment must not write to the supplier; a correction would write again");
    }

    @Test
    @DisplayName("the payment's own proof is offered; the bill's other paperwork is not")
    void onlyTheSlipIsSuggested() throws IOException {
        /*
         * A supplier asking "show me the transfer" wants one file. Suggesting the bill's whole
         * folder is how somebody accidentally sends a lodge our internal paperwork, so the proof of
         * THIS payment is marked suggested and everything else is merely available.
         */
        String source = Files.readString(SERVICE);
        assertTrue(source.contains("findByExpensePaymentIdOrderByCreatedAtDesc"),
            "the payment's own documents are the ones a supplier asked for");
        assertTrue(source.contains("describe(doc, true)") && source.contains("describe(doc, false)"),
            "the two kinds must be distinguishable, or the panel cannot pre-select the right one");
        assertTrue(source.contains("if (doc.getExpensePayment() != null) continue;"),
            "a document belonging to another payment must not be offered here as though it were "
                + "loose on the bill");
    }

    @Test
    @DisplayName("recipients come from the property, and the vendor is the last resort")
    void theAddressesComeFromWhoAnswersThem() throws IOException {
        /*
         * The first version read bill.getVendor().getEmail() and nothing else, so the composer
         * opened with an empty To for Osinon Tented Camp — a camp with two addresses on file,
         * reservations@ marked primary and info@ beside it. A vendor is an account we settle; the
         * addresses that get answered belong to the property.
         */
        String source = Files.readString(SERVICE);
        assertTrue(source.contains("contacts.forBilling(property)"),
            "the property's addresses come first, resolved the way a request for rooms is");
        assertTrue(source.contains("findByExpenseIdOrderByDayNumberAsc"),
            "which property is found from what the bill COVERS: a group can own a dozen camps "
                + "behind one vendor account");
        assertTrue(source.contains("if (byVendor.size() == 1) return byVendor.get(0);"),
            "falling back through the vendor is only safe when exactly one property points at it");

        int property = source.indexOf("contacts.forBilling(property)");
        int vendor = source.indexOf("bill.getVendor().getEmail()");
        assertTrue(property > 0 && vendor > property,
            "the vendor's single address must be the LAST resort, not the first");
    }

    @Test
    @DisplayName("one implementation of who to write to, shared with the availability letter")
    void theResolverIsNotCopied() throws IOException {
        String availability = Files.readString(Path.of("src/main/java/com/itineraryledger/"
            + "kabengosafaris/Safari/AvailabilityRequest/Services/AvailabilityLetterService.java"));

        assertTrue(availability.contains("contacts.forReservations(property)"),
            "the availability letter must use the shared resolver, so its behaviour and the "
                + "advice's cannot drift apart");
        assertFalse(availability.contains("private AccommodationEmail best("),
            "its private copy of the parent-group fallback, de-duplication and ordering is gone; "
                + "two copies of that would drift");

        String resolver = Files.readString(Path.of("src/main/java/com/itineraryledger/"
            + "kabengosafaris/Accommodation/Services/SupplierContactResolver.java"));
        assertTrue(resolver.contains("EmailType.BILLING") && resolver.contains("EmailType.RESERVATIONS"),
            "the difference between the two letters is the PREFERENCE, and it lives in one place");
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
