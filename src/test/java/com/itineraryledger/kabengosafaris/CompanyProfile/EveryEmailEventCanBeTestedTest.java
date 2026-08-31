package com.itineraryledger.kabengosafaris.CompanyProfile;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
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

/**
 * That "Send me a test" works for every email the product can send.
 *
 * The test send is the only way to see a template as a recipient will, and the only safe way — it
 * goes to the signed-in address and nowhere else. An event with no sample data does not degrade
 * gracefully: it answers 501 with "Please implement a test case in
 * EmailTemplateTestService.generateTestEmailData()", which is a message written for whoever built
 * the system, shown to whoever is editing the wording.
 *
 * That happened for BILL_DUE_REMINDER the day it shipped. Fixing it turned up three more — the
 * invoice, credit note and payment receipt letters had never had sample data either, so the button
 * had been refusing on the three most customer-facing templates in the product without anybody
 * noticing.
 *
 * Source-level, because the alternative is a Spring context and a mail account.
 */
class EveryEmailEventCanBeTestedTest {

    private static final Path CATALOGUE = Path.of("src/main/java/com/itineraryledger/"
        + "kabengosafaris/EmailEvent/EmailEventVariables.java");
    private static final Path TEST_SERVICE = Path.of("src/main/java/com/itineraryledger/"
        + "kabengosafaris/EmailEvent/Services/EmailTemplateTestService.java");

    /** Every event the product registers, from the array the catalogue keeps. */
    private Set<String> registeredEvents() throws IOException {
        String source = Files.readString(CATALOGUE);
        int from = source.indexOf("\"USER_REGISTRATION\"");
        assertTrue(from > 0, "the event list has moved — this test reads it from EmailEventVariables");

        Set<String> events = new LinkedHashSet<>();
        Matcher m = Pattern.compile("\"([A-Z][A-Z_]+)\"").matcher(source.substring(from));
        while (m.find()) events.add(m.group(1));
        return events;
    }

    @Test
    @DisplayName("every registered event has sample data, so its template can be tested")
    void nothingRefusesTheTestButton() throws IOException {
        Set<String> events = registeredEvents();
        String service = Files.readString(TEST_SERVICE);

        Set<String> handled = new LinkedHashSet<>();
        Matcher cases = Pattern.compile("case \"([A-Z][A-Z_]+)\":").matcher(service);
        while (cases.find()) handled.add(cases.group(1));

        List<String> missing = new ArrayList<>();
        for (String event : events) {
            if (!handled.contains(event)) missing.add(event);
        }

        assertFalse(events.isEmpty(), "no events were read — the catalogue's shape has changed");
        assertTrue(missing.isEmpty(),
            "These events have no sample data, so \"Send me a test\" answers 501 with a message "
                + "addressed to whoever built the system: " + missing
                + ". Add a case to EmailTemplateTestService.generateTestEmailData().");
    }

    @Test
    @DisplayName("no sample names a company — the fixtures are literals like any other string")
    void samplesNameNobody() throws IOException {
        /*
         * The same rule the templates live under. A sample vendor called after a real company would
         * be a company name compiled into the jar, which is what CompanyLiteralsInTemplatesTest
         * exists to prevent — and a fixture is the easiest place to forget it.
         */
        String service = Files.readString(TEST_SERVICE);
        int from = service.indexOf("generateBillDueReminderTestData");
        assertTrue(from > 0, "the bill reminder fixture has gone");

        assertTrue(service.contains("companyIdentityService.snapshot().email()"),
            "a letter's own address must come from the company record, never a literal");
    }
}
