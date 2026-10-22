package com.itineraryledger.kabengosafaris.ClientPortal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Nothing a client's own page can reach knows what the trip costs us.
 *
 * <p>The quote and its day tree carry a commercial layer the customer is not party to: the
 * operator price, the margin, the agent commission, the uplift, the flag that says whether this
 * quotation is priced at cost, and the notes staff write to each other on the quote and on every
 * single day of it. All of that sits on the same objects as the trip.
 *
 * <p>So the portal has its own view model with <strong>no field that could hold any of it</strong>,
 * and this test is what keeps it that way. The alternative, which is what an itinerary PDF did
 * until somebody noticed, is to hand a template a rich object and trust every future edit not to
 * print the wrong getter. That held until a client's itinerary came out reading "Auto-linked
 * alternative: MID_RANGE in Arusha, STO $90".
 *
 * <p>The sibling of {@code NoClientDocumentPrintsOurCostTest}, which does the same job for the
 * PDFs. This one covers the pages.
 */
class NoPortalResponseCarriesOurCostTest {

    private static final Path PORTAL_JAVA =
        Path.of("src/main/java/com/itineraryledger/kabengosafaris/ClientPortal");
    private static final Path PORTAL_TEMPLATES =
        Path.of("src/main/resources/templates/portal");
    private static final Path VIEW_MODEL = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/ClientPortal/DTOs/PortalView.java");

    /** What must not be reachable from anything a client reads. */
    private static final Map<Pattern, String> FORBIDDEN = new LinkedHashMap<>();
    static {
        FORBIDDEN.put(Pattern.compile("\\bgetInternalNotes\\b|\\binternalNotes\\b"),
            "the staff note field. It exists on the quote AND on every day, one word away from "
                + "customerNotes, and it is where 'confirm before quoting' gets written");
        FORBIDDEN.put(Pattern.compile("\\bgetSpecialNotes\\b|\\bspecialNotes\\b"),
            "the per-day note. Already reached a client once on a printed itinerary, reading "
                + "'ARUSHA TOWN HAS NO MID-RANGE PROPERTY WITH RATES LOADED'");
        FORBIDDEN.put(Pattern.compile("\\bsto[A-Z]\\w*|\\bgetSto|\\bisStoRate\\b|\\bgetIsStoRate\\b"),
            "an operator cost, or the flag saying this quotation is priced at one");
        /*
         * The commercial senses of the word only. A bare "margin" cannot be forbidden on a page
         * that carries its own CSS, and a rule that fires on `margin: 0` is a rule somebody
         * switches off within a week.
         */
        FORBIDDEN.put(Pattern.compile("\\bmarginPercent\\w*|\\bgrossProfit\\b|\\bgetMargin[A-Z]\\w*"),
            "our margin");
        FORBIDDEN.put(Pattern.compile("\\bagentCommission\\w*|\\bcommissionPercentage\\b"),
            "the agent commission");
        FORBIDDEN.put(Pattern.compile("\\bmarginUplift\\w*"),
            "the uplift");
        FORBIDDEN.put(Pattern.compile("\\brackUnitPrice\\b|\\brackTotalPrice\\b|\\bgetRack"),
            "the rack/STO pair, which only makes sense beside a cost");
    }

    @Test
    @DisplayName("the portal's own view model has no field that could hold a cost")
    void theViewModelCannotCarryIt() throws IOException {
        String source = withoutComments(Files.readString(VIEW_MODEL));
        List<String> offences = new ArrayList<>();

        for (Map.Entry<Pattern, String> rule : FORBIDDEN.entrySet()) {
            Matcher matcher = rule.getKey().matcher(source);
            if (matcher.find()) {
                offences.add("PortalView carries '" + matcher.group() + "' — " + rule.getValue());
            }
        }
        assertTrue(offences.isEmpty(), () -> String.join("\n", offences));
    }

    @Test
    @DisplayName("no portal page or service reads one either")
    void nothingInThePortalReadsIt() throws IOException {
        List<String> offences = new ArrayList<>();

        for (Path file : sources()) {
            String[] lines = withoutComments(Files.readString(file)).split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                for (Map.Entry<Pattern, String> rule : FORBIDDEN.entrySet()) {
                    Matcher matcher = rule.getKey().matcher(lines[i]);
                    if (matcher.find()) {
                        offences.add(String.format("%s:%d  '%s'  ->  %s",
                            file.getFileName(), i + 1, matcher.group(), rule.getValue()));
                    }
                }
            }
        }

        if (!offences.isEmpty()) {
            fail("A client can reach these:\n  " + String.join("\n  ", offences)
                + "\n\nThe portal has its own view model so that there is nothing to remember to "
                + "leave out. If one of these is genuinely needed, it is not.");
        }
    }

    @Test
    @DisplayName("the client's figures come from the quotation, not from a second calculation")
    void theFiguresAreTheQuotedOnes() throws IOException {
        /*
         * The client's total must be the one on their quotation. Recomputing it here would
         * produce a second answer that drifts the first time anybody changes a percentage, and
         * the client would be holding two documents with two numbers and no way to tell which
         * they owe.
         */
        String view = withoutComments(Files.readString(Path.of(
            "src/main/java/com/itineraryledger/kabengosafaris/ClientPortal/Services/PortalViewService.java")));

        assertTrue(view.contains("quote.getGrandTotals()"),
            "the total shown to a client is the quotation's stored grand total");
        assertFalse(view.contains("CostEstimation") || view.contains("estimate("),
            "the portal must not price anything. Pricing belongs to the engine, and a second "
                + "caller of it is a second answer");
    }

    private List<Path> sources() throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> java = Files.walk(PORTAL_JAVA)) {
            java.filter(f -> f.toString().endsWith(".java")).forEach(files::add);
        }
        if (Files.exists(PORTAL_TEMPLATES)) {
            try (Stream<Path> html = Files.walk(PORTAL_TEMPLATES)) {
                html.filter(f -> f.toString().endsWith(".html")).forEach(files::add);
            }
        }
        assertFalse(files.isEmpty(), "found no portal sources — this guard would check nothing");
        return files;
    }

    /**
     * Comments say why a field is absent, and naming it there must not fail the build.
     *
     * <p>Both comment styles, because half these files are Java and half are templates.
     */
    private String withoutComments(String source) {
        return source
            .replaceAll("(?s)/\\*.*?\\*/", "")
            .replaceAll("(?m)^\\s*//.*$", "")
            .replaceAll("(?s)<!--.*?-->", "");
    }
}
