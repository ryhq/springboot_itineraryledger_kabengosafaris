package com.itineraryledger.kabengosafaris.PdfDocument;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A document that states a price states what the price covers.
 *
 * <p>Written from the gap it closes. A quote for a real trip was sent in September priced at
 * 5,629 USD with no statement anywhere on it of what that bought — and it was not an oversight by
 * whoever produced it: no template could print the lines, because the field was absent from every
 * PDF DTO and every schema. The only "what's included" prose in the whole document pipeline was a
 * hardcoded paragraph on the cost-estimation sheet, which customers never see.
 *
 * <p>The exempt list is the same idea as {@code NoClientDocumentPrintsOurCostTest.OPERATIONAL}: a
 * template that is not a customer's statement of price names itself here and says why, rather than
 * quietly failing to carry something it should.
 */
class EveryClientDocumentSaysWhatIsIncludedTest {

    private static final Path TEMPLATES = Paths.get("src/main/resources/templates/pdf-templates");

    /**
     * Templates that must NOT carry the block, each for its own reason.
     *
     * <p>Add to this list only with a reason. "It was easier" is not one — the default for anything
     * a customer reads beside a figure is that it explains the figure.
     */
    private static final Set<String> EXEMPT = Set.of(
        /* Operational sheets for our own people; the guide and the office already know. */
        "full_itinerary_accommodation_plan.html",
        "full_itinerary_accommodation_choices.html",
        "full_itinerary_driver_roadbook.html",
        "safari_accommodation_plan.html",
        /* Internal costings. They carry our margin; they are not a statement to anybody. */
        "full_cost_estimation_default.html",
        /* Its own hardcoded paragraph already says it, and it is a working sheet, not a document. */
        "full_cost_estimation_client_price_sheet.html",
        /* Money already received. A receipt restates nothing; it acknowledges. */
        "payment_receipt_default.html",
        "payment_receipt_prof.html",
        "payment_receipt_simple.html",
        /* A correction to a bill already sent. What the trip covered was stated on that bill. */
        "full_credit_note_default.html"
    );

    @Test
    @DisplayName("every client-facing template prints what the price covers, and what it does not")
    void everyClientTemplateCarriesTheBlock() throws IOException {
        List<String> offences = new ArrayList<>();

        for (Path file : templates()) {
            String name = file.getFileName().toString();
            if (EXEMPT.contains(name)) continue;

            String source = Files.readString(file);
            if (!source.contains(".inclusionsList")) {
                offences.add(name + ": never iterates inclusionsList, so it prints a price with no "
                    + "statement of what it buys");
            }
            if (!source.contains(".exclusionsList")) {
                offences.add(name + ": never iterates exclusionsList — half the promise is the half "
                    + "that prevents an argument later");
            }
            /*
             * An unguarded block prints a heading over nothing on every legacy document, which
             * looks worse than saying nothing at all.
             */
            if (source.contains(".inclusionsList") && !source.contains("#lists.isEmpty")) {
                offences.add(name + ": prints the block without a #lists.isEmpty guard, so a "
                    + "document with no lines gets an empty heading");
            }
        }

        assertTrue(offences.isEmpty(), () -> String.format("""
            %d client-facing template(s) do not say what their price covers:

              %s

            Add the block, or name the file in EXEMPT with the reason it is not a customer's
            statement of price.
            """, offences.size(), String.join("\n  ", offences)));
    }

    @Test
    @DisplayName("a quote, safari or invoice prints its OWN promise, never the template's")
    void noDocumentReachesThroughToTheItinerary() throws IOException {
        List<String> offences = new ArrayList<>();

        for (Path file : templates()) {
            String name = file.getFileName().toString();
            if (name.startsWith("full_itinerary")) continue;   // the itinerary IS the itinerary

            String source = Files.readString(file);
            for (String reach : List.of(
                "itinerary.inclusionsList", "itinerary.exclusionsList", "itinerary.inclusions")) {
                if (source.contains(reach)) {
                    offences.add(name + ": reads " + reach);
                }
            }
        }

        assertTrue(offences.isEmpty(), () -> String.format("""
            %d template(s) reach through to the itinerary for what the price covers:

              %s

            A quote, safari or invoice holds its OWN snapshot, and the two disagree the moment
            anybody edits the quote — which is the whole point of the chain. Reading the template
            here would print a promise the customer never agreed to.

            This is not hypothetical: full_safari_modern.html already does exactly this for
            highlights, rendering safari.itinerary.highlightsList against a DTO that has no such
            getter.
            """, offences.size(), String.join("\n  ", offences)));
    }

    @Test
    @DisplayName("the exempt list names only templates that still exist")
    void theExemptListIsNotStale() throws IOException {
        List<String> missing = new ArrayList<>();
        List<String> present = templates().stream().map(p -> p.getFileName().toString()).toList();
        for (String name : EXEMPT) {
            if (!present.contains(name)) missing.add(name);
        }
        assertTrue(missing.isEmpty(), () ->
            "EXEMPT names a template that no longer exists, so the exemption is hiding nothing and "
            + "would silently exempt a future file of the same name: " + String.join(", ", missing));
    }

    private List<Path> templates() throws IOException {
        try (Stream<Path> files = Files.walk(TEMPLATES)) {
            return files.filter(p -> p.toString().endsWith(".html")).sorted().toList();
        }
    }
}
