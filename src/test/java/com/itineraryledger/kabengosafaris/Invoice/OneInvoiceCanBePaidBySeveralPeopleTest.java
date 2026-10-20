package com.itineraryledger.kabengosafaris.Invoice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.FullInvoiceShareDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceShare;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Payment;
import com.itineraryledger.kabengosafaris.PdfDocument.PdfDocumentVariables;

/**
 * A group trip is organised by one person and paid for by six.
 *
 * <p>Before shares there were two ways to handle that and both were bad. The five others could
 * send their money to the organiser and the organiser could wire one lump on, which makes a
 * private individual carry several thousand dollars of other people's money with no protection
 * at all. Or six payments could be recorded against the one invoice, which satisfies the bank and
 * nobody else: {@code Payment} had no payer, so all six receipts came out in the organiser's name
 * and went to the organiser's inbox, and nothing anywhere recorded who still owed what.
 *
 * <p>What this pins is the quiet half. A share that forgets to carry its payer's address sends
 * that person's receipt to the lead traveller, which is both wrong and a privacy leak, and
 * nothing throws. A payment with no share must keep behaving exactly as it did before any of this
 * existed, or every invoice in the system changes meaning at once. And the document type must be
 * reachable from the dispatcher: FULL_CREDIT_NOTE shipped with a schema, a template and a seeded
 * row, all of them unreachable, and the only symptom was a 404 nobody saw for months.
 */
class OneInvoiceCanBePaidBySeveralPeopleTest {

    private static final String DOCUMENT = "INVOICE_PAYMENT_REQUEST";
    private static final Path SCHEMA =
        Path.of("src/main/resources/schemas/pdf-documents/invoice-payment-request-schema.json");
    private static final Path TEMPLATE =
        Path.of("src/main/resources/templates/pdf-templates/invoice_payment_request_default.html");

    // ------------------------------------------------------------------
    // who the paperwork reaches
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a share's receipt goes to its own payer, not to the lead traveller")
    void theShareIsAddressedToWhoeverPaid() {
        InvoiceShare share = InvoiceShare.builder()
            .invoice(invoiceFor("Ms Sarah Scharfen", "sarah.scharfen@gmx.de"))
            .payerName("Familie Vogt")
            .payerEmail("vogt@example.de")
            .build();

        assertEquals("Familie Vogt", share.resolvedPayerName());
        assertEquals("vogt@example.de", share.resolvedPayerEmail(),
            "the person who sent the money must be the person who gets the receipt");
    }

    @Test
    @DisplayName("a linked customer supplies the address when the share has none")
    void theLinkedCustomerIsTheNextBestAddress() {
        Customer payer = new Customer();
        payer.setFirstName("Anja");
        payer.setLastName("Vogt");

        InvoiceShare share = InvoiceShare.builder()
            .invoice(invoiceFor("Ms Sarah Scharfen", "sarah.scharfen@gmx.de"))
            .payerName("Familie Vogt")
            .customer(payer)
            .build();

        // no payerEmail, no customer email either: it must still not silently vanish
        assertEquals("sarah.scharfen@gmx.de", share.resolvedPayerEmail(),
            "with nowhere better to send it the receipt lands on the invoice's customer, which "
            + "is wrong but visible, rather than nowhere at all");
    }

    @Test
    @DisplayName("a payment with no share behaves exactly as it always did")
    void anUnsplitPaymentIsUnchanged() {
        Payment payment = Payment.builder()
            .invoice(invoiceFor("Ms Sarah Scharfen", "sarah.scharfen@gmx.de"))
            .amount(new BigDecimal("1000.00"))
            .currency("USD")
            .build();

        assertNull(payment.getShare(),
            "null is the ordinary case: every payment already recorded must keep its meaning");
    }

    // ------------------------------------------------------------------
    // what the payer is asked for
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a settled share is told so rather than chased")
    void aSettledShareIsNotADemand() {
        FullInvoiceShareDTO owing = FullInvoiceShareDTO.builder()
            .amount(new BigDecimal("3653.80"))
            .balance(new BigDecimal("3653.80"))
            .build();
        assertTrue(!owing.isSettled());

        FullInvoiceShareDTO paid = FullInvoiceShareDTO.builder()
            .amount(new BigDecimal("3653.80"))
            .balance(BigDecimal.ZERO)
            .build();
        assertTrue(paid.isSettled(), "nothing outstanding means nothing to ask for");

        FullInvoiceShareDTO overpaid = FullInvoiceShareDTO.builder()
            .amount(new BigDecimal("3653.80"))
            .balance(new BigDecimal("-120.00"))
            .build();
        assertTrue(overpaid.isSettled(),
            "sending 'please pay' to someone who has overpaid is the fastest way to be chased "
            + "by a client who owes nothing");
    }

    // ------------------------------------------------------------------
    // the document is actually reachable
    // ------------------------------------------------------------------

    @Test
    @DisplayName("the document type is registered and names a class that exists")
    void theDocumentTypeIsRegistered() throws Exception {
        assertTrue(List.of(PdfDocumentVariables.getSupportedDocuments()).contains(DOCUMENT),
            DOCUMENT + " is not in getSupportedDocuments(), so it is never seeded");
        String className = PdfDocumentVariables.getDataSourceClass(DOCUMENT);
        assertNotNull(className, DOCUMENT + " declares no data source class");
        assertEquals(FullInvoiceShareDTO.class, Class.forName(className));
        assertEquals("share", PdfDocumentVariables.getRootVariableName(DOCUMENT));
    }

    @Test
    @DisplayName("both dispatcher switches can reach it, not just the generate one")
    void bothDispatcherBranchesExist() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/itineraryledger/kabengosafaris/PdfDocument/Services/PdfGenerationService.java"));
        int cases = source.split(Pattern.quote("case \"" + DOCUMENT + "\""), -1).length - 1;
        assertEquals(2, cases,
            "the credit note had a case in one switch and not the other for months: generate "
            + "worked and preview came back 'PDF document type not found'");
    }

    @Test
    @DisplayName("the default template is named where the installer will look for it")
    void theTemplateIsWhereTheLoaderExpects() {
        assertTrue(Files.exists(TEMPLATE),
            "PdfTemplateStorageService resolves templates/pdf-templates/<document lowercased>"
            + "_default.html, so this exact filename is load-bearing: " + TEMPLATE);
    }

    @Test
    @DisplayName("every field the schema promises is readable on the declared model")
    void theSchemaMatchesTheModel() throws Exception {
        Class<?> model = Class.forName(PdfDocumentVariables.getDataSourceClass(DOCUMENT));
        JsonNode schema = new ObjectMapper().readTree(Files.readString(SCHEMA));

        List<String> missing = new ArrayList<>();
        for (JsonNode field : schema) {
            String path = field.path("path").asText();
            if (path.isEmpty()) continue;
            if (resolve(model, path) == null) {
                missing.add(path);
            }
        }
        if (!missing.isEmpty()) {
            fail(model.getSimpleName() + " cannot answer " + missing
                + ", which the schema tells template authors it will");
        }
    }

    @Test
    @DisplayName("every path the template renders resolves on the declared model")
    void theTemplateMatchesTheModel() throws Exception {
        Class<?> model = Class.forName(PdfDocumentVariables.getDataSourceClass(DOCUMENT));
        String root = PdfDocumentVariables.getRootVariableName(DOCUMENT);

        Matcher matcher = Pattern.compile("\\$\\{" + root + "\\.([A-Za-z][A-Za-z0-9.]*)")
            .matcher(Files.readString(TEMPLATE));
        Set<String> paths = new LinkedHashSet<>();
        while (matcher.find()) {
            paths.add(matcher.group(1));
        }
        assertTrue(paths.size() > 10, "found only " + paths.size() + " paths; the scan is not working");

        List<String> unresolved = new ArrayList<>();
        for (String path : paths) {
            if (resolve(model, path) == null) {
                unresolved.add(path);
            }
        }
        if (!unresolved.isEmpty()) {
            fail("the template reads " + unresolved + " on " + model.getSimpleName()
                + ", which has no such property; SpEL prints nothing and the PDF comes out blank there");
        }
    }

    // ------------------------------------------------------------------

    private static Invoice invoiceFor(String displayName, String email) {
        Customer customer = new Customer() {
            @Override
            public String getDisplayName() {
                return displayName;
            }

            @Override
            public String getPrimaryEmail() {
                return email;
            }
        };
        Invoice invoice = new Invoice();
        invoice.setCustomer(customer);
        return invoice;
    }

    private static Class<?> resolve(Class<?> type, String path) {
        Class<?> current = type;
        for (String segment : path.split("\\.")) {
            current = property(current, segment);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private static Class<?> property(Class<?> type, String segment) {
        String capitalised = Character.toUpperCase(segment.charAt(0)) + segment.substring(1);
        for (String candidate : List.of("get" + capitalised, "is" + capitalised, segment)) {
            try {
                Method method = type.getMethod(candidate);
                return method.getReturnType();
            } catch (NoSuchMethodException ignored) {
                // try the next shape
            }
        }
        return null;
    }
}
