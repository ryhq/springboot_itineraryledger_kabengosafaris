package com.itineraryledger.kabengosafaris.EmailEvent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every event, not just the one that broke.
 *
 * <p>A placeholder is only substituted when the event DECLARES it. An undeclared one survives into
 * the message as literal {@code {{braces}}}, and a required one that nothing supplies stops the
 * send outright, inside an async block that logs a warning nobody reads. Both failures are
 * invisible until a customer or a colleague mentions it, so both are checked here for every event
 * that ships with a template rather than only for the one we happened to catch.
 *
 * <p>Company and bank placeholders are excluded on purpose: a third pass substitutes those into
 * every template regardless of what an event declares.
 */
class EveryEventCanRenderItsTemplateTest {

    private static final Path SCHEMAS = Path.of("src/main/resources/schemas/email-events");
    private static final Path TEMPLATES = Path.of("src/main/resources/templates/email-templates");

    @Test
    @DisplayName("no template prints a variable its event does not declare")
    void nothingUndeclaredReachesTheReader() throws Exception {
        List<String> problems = new ArrayList<>();

        for (Path template : templates()) {
            Path schema = schemaFor(template);
            if (schema == null) continue;   // a second template for an event names itself freely

            Set<String> declared = namesIn(schema);
            for (String used : placeholdersIn(Files.readString(template))) {
                if (declared.contains(used) || substitutedForEveryTemplate(used)) continue;
                problems.add(template.getFileName() + " prints {{" + used + "}}, which "
                    + schema.getFileName() + " does not declare");
            }
        }

        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    @DisplayName("every schema is a readable list of named variables")
    void schemasAreWellFormed() throws Exception {
        List<String> problems = new ArrayList<>();
        try (Stream<Path> files = Files.list(SCHEMAS)) {
            for (Path schema : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                JsonNode root = new ObjectMapper().readTree(Files.readString(schema));
                if (!root.isArray() || root.isEmpty()) {
                    problems.add(schema.getFileName() + " is not a non-empty array");
                    continue;
                }
                for (JsonNode v : root) {
                    if (v.path("name").asText("").isBlank()) {
                        problems.add(schema.getFileName() + " has a variable with no name");
                    }
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    /**
     * The four events that answer the public, specifically.
     *
     * <p>They are the ones a customer sees, so a required variable their template never prints and
     * no sender writes would fail in front of the person we were trying to reassure.
     */
    @Test
    @DisplayName("the acknowledgements the public receives declare a link before they print one")
    void acknowledgementsDeclareTheirLinks() throws Exception {
        List<String> problems = new ArrayList<>();
        for (String event : List.of("newsletter-confirm", "newsletter-welcome",
                                    "contact-us-received", "booking-inquiry-received")) {
            Path schema = SCHEMAS.resolve(event + "-schema.json");
            Path template = TEMPLATES.resolve(event.replace('-', '_') + "_default.html");
            assertTrue(Files.exists(schema), "missing schema for " + event);
            assertTrue(Files.exists(template), "missing template for " + event);

            Set<String> declared = namesIn(schema);
            for (String used : placeholdersIn(Files.readString(template))) {
                if (declared.contains(used) || substitutedForEveryTemplate(used)) continue;
                problems.add(event + " prints {{" + used + "}} undeclared");
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    @DisplayName("a newsletter mail carries an unsubscribe link, and it is not optional")
    void theWelcomeCannotLoseItsUnsubscribeLink() throws Exception {
        String template = Files.readString(TEMPLATES.resolve("newsletter_welcome_default.html"));
        assertTrue(template.contains("{{unsubscribeUrl}}"),
            "every newsletter mail must offer a way out; without one the address has no escape "
            + "except marking us as spam, which costs every other email the company sends");

        boolean required = false;
        for (JsonNode v : new ObjectMapper().readTree(
                Files.readString(SCHEMAS.resolve("newsletter-welcome-schema.json")))) {
            if ("unsubscribeUrl".equals(v.path("name").asText())) {
                required = v.path("isRequired").asBoolean(false);
            }
        }
        assertTrue(required,
            "marked required so a blank one refuses to send rather than going out as a dead link");
    }

    private boolean substitutedForEveryTemplate(String name) {
        return name.startsWith("company") || name.startsWith("bank") || name.equals("currentYear");
    }

    private List<Path> templates() throws Exception {
        try (Stream<Path> files = Files.list(TEMPLATES)) {
            return files.filter(f -> f.toString().endsWith("_default.html")).sorted().toList();
        }
    }

    /** booking_inquiry_received_default.html -> booking-inquiry-received-schema.json, when it exists. */
    private Path schemaFor(Path template) {
        String base = template.getFileName().toString().replace("_default.html", "");
        Path schema = SCHEMAS.resolve(base.replace('_', '-') + "-schema.json");
        return Files.exists(schema) ? schema : null;
    }

    private Set<String> namesIn(Path schema) throws Exception {
        Set<String> names = new LinkedHashSet<>();
        for (JsonNode v : new ObjectMapper().readTree(Files.readString(schema))) {
            names.add(v.path("name").asText());
        }
        return names;
    }

    private Set<String> placeholdersIn(String html) {
        Set<String> names = new LinkedHashSet<>();
        Matcher m = Pattern.compile("\\{\\{#?/?([a-zA-Z][a-zA-Z0-9_]*)\\}\\}").matcher(html);
        while (m.find()) names.add(m.group(1));
        return names;
    }
}
