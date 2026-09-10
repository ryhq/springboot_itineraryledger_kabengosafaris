package com.itineraryledger.kabengosafaris.BookingInquiry.Services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itineraryledger.kabengosafaris.BookingInquiry.Entity.BookingInquiry;
import com.itineraryledger.kabengosafaris.EmailEvent.EmailEventVariables;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * A booking inquiry that nobody is told about is a lost booking.
 *
 * <p>Every inquiry notification since the feature shipped failed, and none of them made a sound.
 * The event's stored catalogue demanded a variable named {@code inquiryId}; the sender supplies
 * {@code inquiryCode} and the template prints {@code inquiryCode}. Nothing produced or consumed
 * {@code inquiryId}, so {@code validateRequiredVariables} threw on every single render, inside a
 * {@code CompletableFuture} whose catch logs a warning and returns. The delivery log holds no
 * booking inquiry email at all.
 *
 * <p>The three ways that mismatch can happen are each checked here, because the cost of missing it
 * is a customer who wrote to us and was never answered.
 */
class EveryInquiryEmailCanActuallyRenderTest {

    private static final Path TEMPLATE =
        Path.of("src/main/resources/templates/email-templates/booking_inquiry_default.html");

    @Test
    @DisplayName("the sender supplies every variable the schema marks required")
    void theSenderSatisfiesTheSchema() throws Exception {
        /*
         * The bare inquiry is the point: a web form gives us a name, an email and a head count,
         * and the office must still be told. Every optional field is deliberately left unset, the
         * shape of INQ-0001-08-26, which is the inquiry that went unanswered.
         */
        BookingInquiry bare = new BookingInquiry();
        bare.setCode("INQ-0001-08-26");
        bare.setFirstName("Rukaiya");
        bare.setLastName("Docrat");
        bare.setEmail("someone@example.com");
        bare.setAdults(15);
        bare.setChildren(0);

        BookingInquiryService service =
            mock(BookingInquiryService.class, withSettings().defaultAnswer(inv -> inv.callRealMethod()));
        Map<String, String> supplied = service.buildNotificationVariables(bare);

        List<String> missing = new ArrayList<>();
        for (String required : requiredVariables()) {
            String value = supplied.get(required);
            // the renderer rejects blank as well as absent, so this test must too
            if (value == null || value.isBlank()) missing.add(required);
        }

        assertTrue(missing.isEmpty(),
            "the renderer refuses to send when a required variable is missing or blank, and says "
            + "so only to the log. Not supplied by BookingInquiryService: " + missing);
    }

    @Test
    @DisplayName("every placeholder the template prints is declared in the schema")
    void theTemplateAsksForNothingUndeclared() throws Exception {
        Set<String> declared = declaredVariables();
        Set<String> used = placeholdersIn(Files.readString(TEMPLATE));

        List<String> undeclared = used.stream()
            .filter(name -> !declared.contains(name))
            .filter(name -> !resolvedByTheCompanyPass(name))
            .sorted().toList();

        assertTrue(undeclared.isEmpty(),
            "only declared variables are substituted, so an undeclared one reaches the reader as "
            + "literal {{braces}}: " + undeclared);
    }

    @Test
    @DisplayName("nothing is required that neither the template nor the sender knows about")
    void noPhantomRequirements() throws Exception {
        Set<String> used = placeholdersIn(Files.readString(TEMPLATE));

        BookingInquiry bare = new BookingInquiry();
        bare.setCode("INQ-0001-08-26");
        bare.setFirstName("A");
        bare.setLastName("B");
        bare.setEmail("a@b.co");
        bare.setAdults(1);
        bare.setChildren(0);
        BookingInquiryService service =
            mock(BookingInquiryService.class, withSettings().defaultAnswer(inv -> inv.callRealMethod()));
        Set<String> supplied = service.buildNotificationVariables(bare).keySet();

        List<String> phantom = requiredVariables().stream()
            .filter(name -> !used.contains(name) && !supplied.contains(name))
            .sorted().toList();

        assertTrue(phantom.isEmpty(),
            "a variable nothing writes and nothing prints can only block the send: " + phantom);
    }

    /**
     * The company's own placeholders are substituted by a third pass that runs for every template,
     * so they are deliberately absent from each event's schema. Excluding them here keeps this
     * test about the event's own variables, which are the ones an event can get wrong.
     */
    private boolean resolvedByTheCompanyPass(String name) {
        return name.startsWith("company") || name.startsWith("bank") || name.equals("currentYear");
    }

    private Set<String> placeholdersIn(String html) {
        Set<String> names = new LinkedHashSet<>();
        Matcher m = Pattern.compile("\\{\\{([a-zA-Z][a-zA-Z0-9_]*)\\}\\}").matcher(html);
        while (m.find()) names.add(m.group(1));
        return names;
    }

    private List<String> requiredVariables() throws Exception {
        List<String> names = new ArrayList<>();
        for (JsonNode v : schema()) {
            if (v.path("isRequired").asBoolean(false)) names.add(v.path("name").asText());
        }
        return names;
    }

    private Set<String> declaredVariables() throws Exception {
        Set<String> names = new LinkedHashSet<>();
        for (JsonNode v : schema()) names.add(v.path("name").asText());
        return names;
    }

    private JsonNode schema() throws Exception {
        return new ObjectMapper().readTree(EmailEventVariables.getVariablesForEvent("BOOKING_INQUIRY"));
    }
}
