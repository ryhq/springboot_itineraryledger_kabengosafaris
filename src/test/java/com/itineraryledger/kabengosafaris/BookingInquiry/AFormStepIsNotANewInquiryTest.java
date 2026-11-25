package com.itineraryledger.kabengosafaris.BookingInquiry;

import com.itineraryledger.kabengosafaris.BookingInquiry.Entity.BookingInquiry;
import com.itineraryledger.kabengosafaris.Security.RateLimit.RateLimitFilter;
import com.itineraryledger.kabengosafaris.Security.RateLimit.RateLimitRule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One visitor filling in one form is now six requests, and none of them is a new enquiry.
 *
 * <p>The form used to post once, at the end. It now saves as it is typed, so a single family
 * planning a single trip sends a create and then four or five appends. Two things that used to
 * be safe stop being safe at that point, and both fail quietly.
 *
 * <p>The first is the rate limit. A rule matches on {@code startsWith} and the first match in
 * the list wins ({@link RateLimitRule#matches}), so every new path under
 * {@code /api/public/booking-inquiries} inherits the ten-an-hour ceiling meant for whole
 * enquiries unless a more specific rule is listed above it. Two families behind one hotel's
 * wifi would then be refused halfway through the form, and what they would see is a form that
 * stopped working.
 *
 * <p>The second is the acknowledgement. A required template variable that arrives blank makes
 * the renderer throw, inside a {@code CompletableFuture} whose only reaction is a logged
 * warning - the exact failure that once lost every inquiry email for months. Since a lead can
 * now be saved with a first name and no surname, {@code lastName} being required would mean
 * that every such lead is silently never acknowledged.
 */
class AFormStepIsNotANewInquiryTest {

    private static final Path SCHEMAS = Path.of("src/main/resources/schemas/email-events");

    @Test
    @DisplayName("appending to a draft has its own ceiling, listed above the one for new enquiries")
    void theStepsAreNotRationedLikeWholeEnquiries() throws Exception {
        List<RateLimitRule> rules = rules();

        int stepRule = indexOfPath(rules, "/api/public/booking-inquiries/draft/");
        int createRule = indexOfPath(rules, "/api/public/booking-inquiries/draft");
        int wholeFormRule = indexOfPath(rules, "/api/public/booking-inquiries");

        assertTrue(stepRule >= 0, "there is no rule for appending to a draft");
        assertTrue(stepRule < createRule,
            "the rule for a step sits below the rule for creating a draft, so every step "
                + "matches the create rule first and a visitor is cut off mid-form");
        assertTrue(createRule < wholeFormRule,
            "the draft rules sit below the whole-enquiry rule, so they never match at all");

        /* And the ceiling itself has to fit a real visitor: one create plus several appends. */
        assertTrue(rules.get(stepRule).limit() >= 60,
            "a step is a cheap append to a row the caller already holds the token for; "
                + "rationing it like a new enquiry refuses people halfway through");
    }

    @Test
    @DisplayName("a step resolves to the step rule, not to the one for new enquiries")
    void theFirstMatchIsTheRightMatch() throws Exception {
        List<RateLimitRule> rules = rules();

        assertEquals("form steps", firstMatch(rules, "/api/public/booking-inquiries/draft/abc-123"),
            "appending to a draft is being counted as a new booking inquiry");
        assertEquals("form steps",
            firstMatch(rules, "/api/public/booking-inquiries/draft/abc-123/complete"),
            "finishing a form the visitor has already started is not a new booking inquiry");
        assertEquals("booking inquiries", firstMatch(rules, "/api/public/booking-inquiries/draft"),
            "creating a lead is the call a stranger can reach, so it keeps the old ceiling");
        assertEquals("booking inquiries", firstMatch(rules, "/api/public/booking-inquiries"),
            "the one-shot form the deployed website still posts to must be unaffected");
    }

    @Test
    @DisplayName("a lead with no surname is still acknowledged")
    void aBlankLastNameDoesNotKillTheEmail() throws Exception {
        for (String schema : List.of("booking-inquiry-received-schema.json",
                                     "booking-inquiry-schema.json")) {
            JsonNode lastName = variable(schema, "lastName");
            assertNotNull(lastName, schema + " no longer declares lastName - has it been renamed?");
            assertTrue(!lastName.path("isRequired").asBoolean(false),
                schema + " still marks lastName as required. The form saves a lead from a first "
                    + "name and an email, so that lead would silently never be acknowledged: the "
                    + "render throws inside an async block that only logs a warning.");
        }
    }

    @Test
    @DisplayName("a lead with no surname is not addressed as 'Jane null'")
    void theDisplayNameSurvivesAMissingSurname() {
        BookingInquiry justAFirstName = new BookingInquiry();
        justAFirstName.setFirstName("Jane");
        assertEquals("Jane", justAFirstName.getDisplayName());

        BookingInquiry blankSurname = new BookingInquiry();
        blankSurname.setFirstName("Jane");
        blankSurname.setLastName("   ");
        assertEquals("Jane", blankSurname.getDisplayName());

        BookingInquiry both = new BookingInquiry();
        both.setFirstName("Jane");
        both.setLastName("Scharfen");
        assertEquals("Jane Scharfen", both.getDisplayName());
    }

    @Test
    @DisplayName("a half-filled lead has not been submitted, and a finished one has")
    void submittedIsWhatSeparatesThem() {
        BookingInquiry draft = new BookingInquiry();
        assertTrue(!draft.isSubmitted(), "a lead nobody has finished has not been submitted");

        draft.setSubmittedAt(java.time.LocalDateTime.now());
        assertTrue(draft.isSubmitted());
    }

    /* ------------------------------------------------------------------ helpers */

    @SuppressWarnings("unchecked")
    private static List<RateLimitRule> rules() throws Exception {
        Field field = RateLimitFilter.class.getDeclaredField("RULES");
        field.setAccessible(true);
        return (List<RateLimitRule>) field.get(null);
    }

    private static int indexOfPath(List<RateLimitRule> rules, String path) {
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).path().equals(path)) return i;
        }
        return -1;
    }

    /** What the filter itself would pick, by the same first-match rule. */
    private static String firstMatch(List<RateLimitRule> rules, String path) {
        return rules.stream()
            .filter(r -> r.matches("POST", path))
            .findFirst()
            .map(RateLimitRule::what)
            .orElse("(no rule)");
    }

    private static JsonNode variable(String schemaFile, String name) throws Exception {
        JsonNode root = new ObjectMapper().readTree(Files.readString(SCHEMAS.resolve(schemaFile)));
        JsonNode list = root.isArray() ? root : root.path("variables");
        for (JsonNode node : list) {
            if (name.equals(node.path("name").asText())) return node;
        }
        return null;
    }
}
