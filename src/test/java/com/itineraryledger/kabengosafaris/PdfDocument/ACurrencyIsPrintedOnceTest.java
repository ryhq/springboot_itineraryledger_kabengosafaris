package com.itineraryledger.kabengosafaris.PdfDocument;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A money cell names its currency once.
 *
 * <p>The {@code formatted*} fields carry the ISO code — one dialect everywhere, so an invoice and
 * its own receipt stop disagreeing. Several templates were already printing the code themselves,
 * either in a label above the figure or as a column header, and once the formatter started
 * including it those documents read <b>USD USD 9,500.70</b>.
 *
 * <p>So the rule is positional, not stylistic: <b>wherever a template prints the currency itself,
 * the figure beside it must be bare.</b> A per-currency line-item cell therefore formats the raw
 * number; a label-and-value summary row, which prints no code of its own, uses {@code formatted*}
 * and keeps it.
 *
 * <p>This is easy to undo by copying a neighbouring line, and the damage is a client document that
 * says the currency twice, so it is pinned rather than remembered.
 */
class ACurrencyIsPrintedOnceTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    /**
     * A cell that prints the currency itself, then a formatted value that also carries it.
     *
     * <p>Deliberately narrow: it looks for the two together inside one element, which is the shape
     * that actually misprints, rather than for either on its own.
     */
    private static final Pattern LABEL_THEN_FORMATTED = Pattern.compile(
        "currency-label[^>]*>.{0,200}?formatted(Unit|Total)Price", Pattern.DOTALL);

    @Test
    @DisplayName("no template prints a currency label beside a value that already carries one")
    void theCodeIsNotPrintedTwice() throws IOException {
        List<String> offenders = new ArrayList<>();

        try (Stream<Path> files = Files.walk(TEMPLATES)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".html")).toList()) {
                String body = Files.readString(file);
                Matcher m = LABEL_THEN_FORMATTED.matcher(body);
                if (m.find()) {
                    offenders.add(TEMPLATES.relativize(file).toString());
                }
            }
        }

        assertTrue(offenders.isEmpty(),
            "These print the currency themselves AND use a formatted value that already carries "
                + "it, so the document reads 'USD USD 9,500.70'. Format the raw number instead: "
                + offenders);
    }

    @Test
    @DisplayName("a per-currency line-item cell formats the raw number, not the formatted field")
    void lineItemCellsAreBare() throws IOException {
        /*
         * These five lay their line items out with the currency as a column header or a label, so
         * every one of their item cells must be a bare figure. Named explicitly: a new template
         * copied from one of them is caught by the test above instead.
         */
        List<String> columnLaidOut = List.of(
            "pdf-templates/full_quote_default.html",
            "pdf-templates/full_invoice_default.html",
            "pdf-templates/full_quote_multicurrency.html",
            "pdf-templates/full_credit_note_default.html",
            "pdf-templates/invoice_simple.html");

        List<String> offenders = new ArrayList<>();
        for (String rel : columnLaidOut) {
            String body = Files.readString(TEMPLATES.resolve(rel));
            if (body.contains("price.formattedUnitPrice") || body.contains("price.formattedTotalPrice")) {
                offenders.add(rel);
            }
        }

        assertTrue(offenders.isEmpty(),
            "A line-item cell under a currency column must print the bare number, because the "
                + "column header or label already names the currency: " + offenders);
    }
}
