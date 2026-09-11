package com.itineraryledger.kabengosafaris.PdfDocument;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The build fails if a customer's document can print what a night costs us.
 *
 * An itinerary PDF reached a customer reading "Auto-linked alternative: MID_RANGE in Arusha,
 * STO $90" — our supplier price and our internal budget grading — because the per-night `notes`
 * field is operational by construction and four client-facing templates printed it anyway. The
 * field itself is legitimate; where it may appear is not.
 *
 * So the rule is about the AUDIENCE of the template, not about the string: the handful of documents
 * the office works from may print an operational note, and everything a customer ever sees may not.
 * Adding a template puts it on the client side by default, which is the safe default: if it really
 * is an internal sheet, name it in {@link #OPERATIONAL} and say so.
 */
class NoClientDocumentPrintsOurCostTest {

    private static final Path TEMPLATES = Paths.get("src/main/resources/templates/pdf-templates");

    /**
     * The documents nobody outside the office reads. These may show operational notes and our own
     * cost columns — that is what they are for.
     */
    private static final Set<String> OPERATIONAL = Set.of(
        "full_itinerary_accommodation_plan.html",   // the office's room list
        "full_itinerary_accommodation_choices.html",// the options we are weighing
        "safari_accommodation_plan.html",           // same, once it is a booked safari
        "full_itinerary_driver_roadbook.html",      // the driver's copy
        "full_cost_estimation_default.html"         // the internal costing sheet
    );

    /** What must not be reachable from a document a customer reads. */
    private static final Map<Pattern, String> FORBIDDEN = new LinkedHashMap<>();
    static {
        FORBIDDEN.put(
            Pattern.compile("\\$\\{[^}]*\\bacc[A-Za-z]*\\.notes\\b", Pattern.CASE_INSENSITIVE),
            "the per-night operational note — it has carried our STO price and our budget grading");
        FORBIDDEN.put(
            Pattern.compile("STO\\s*[$\\u00a3\\u20ac]"),
            "an operator cost figure");
        FORBIDDEN.put(
            Pattern.compile("\\bSTO\\b"),
            "the word STO — our pricing basis is not the customer's vocabulary");
        FORBIDDEN.put(
            Pattern.compile("\\b(MID_RANGE|MIDRANGE|ULTRA_LUXURY|BUDGET|LUXURY)\\b"),
            "an internal budget grading (the enum token, not a property's own name)");
    }

    @Test
    @DisplayName("no client-facing PDF template can print our cost or our grading")
    void noCostOnAClientDocument() throws IOException {
        List<String> offences = new ArrayList<>();

        for (Path file : templates()) {
            if (OPERATIONAL.contains(file.getFileName().toString())) continue;

            String[] lines = withoutComments(Files.readString(file)).split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                for (Map.Entry<Pattern, String> rule : FORBIDDEN.entrySet()) {
                    Matcher m = rule.getKey().matcher(lines[i]);
                    if (m.find()) {
                        offences.add(String.format("%s:%d  '%s'  ->  %s",
                            file.getFileName(), i + 1, m.group().trim(), rule.getValue()));
                        break;   // one report per line is enough to find it
                    }
                }
            }
        }

        assertTrue(offences.isEmpty(), () -> String.format("""
            %d place(s) where a customer's own document could print our cost:

            %s

            An operational note belongs on the accommodation plan, the choices sheet, the driver's
            roadbook or the cost estimation sheet — the documents in this test's OPERATIONAL list.
            If the template really is internal, add it there with a comment saying who reads it.
            Otherwise drop the field: there is no safe way to print it to a customer, and filtering
            on a prefix like "Auto-linked" is a blocklist against a string we do not own.
            """, offences.size(), String.join("\n", offences)));
    }

    /**
     * A comment explaining why a field is not printed necessarily quotes the leak it prevents, so
     * the scan must not read it back as the leak. Comments are blanked rather than removed so the
     * line numbers in a failure still point at the file.
     */
    private static String withoutComments(String html) {
        Matcher m = Pattern.compile("<!--.*?-->", Pattern.DOTALL).matcher(html);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(out, Matcher.quoteReplacement(
                m.group().replaceAll("[^\n]", " ")));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static List<Path> templates() throws IOException {
        try (Stream<Path> files = Files.list(TEMPLATES)) {
            return files.filter(f -> f.toString().endsWith(".html")).sorted().toList();
        }
    }

    @Test
    @DisplayName("every template named operational is actually shipped")
    void theOperationalListIsNotStale() throws IOException {
        List<String> missing = new ArrayList<>();
        List<String> shipped = templates().stream().map(p -> p.getFileName().toString()).toList();
        for (String name : OPERATIONAL) {
            if (!shipped.contains(name)) missing.add(name);
        }
        assertTrue(missing.isEmpty(), () -> "OPERATIONAL names a template that no longer exists, so "
            + "the exemption is silently protecting nothing: " + missing);
    }
}
