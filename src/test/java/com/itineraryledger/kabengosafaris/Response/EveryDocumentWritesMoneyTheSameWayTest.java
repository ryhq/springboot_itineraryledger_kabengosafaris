package com.itineraryledger.kabengosafaris.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One invoice, one way of writing what is owed.
 *
 * <p>The same money was formatted six different times in three dialects. An invoice PDF said
 * <b>$2,700.00</b>, the receipt for that very invoice said <b>USD 2,700.00</b>, and the client
 * portal said something else again. Nobody had decided this; the method had simply been copied and
 * drifted. A customer holding all three has to work out whether they are three debts.
 *
 * <p>So: the ISO code in front, grouped thousands, two decimals, and nothing that renders a
 * currency symbol — {@code $} belongs to a dozen currencies and Java writes TZS as {@code TSh},
 * which a German client has no reason to read as money at all.
 */
class EveryDocumentWritesMoneyTheSameWayTest {

    // ── the dialect ─────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("the code goes in front, grouped thousands, two decimals")
    void oneDialect() {
        assertEquals("USD 2,700.00", MoneyFormat.format("USD", new BigDecimal("2700")));
        assertEquals("TZS 46,020.00", MoneyFormat.format("TZS", new BigDecimal("46020")));
        assertEquals("USD 0.50", MoneyFormat.format("usd", new BigDecimal("0.495")));
    }

    @Test
    @DisplayName("an amount whose currency nobody recorded does not get given one")
    void aMissingCurrencyIsNotUsd() {
        String written = MoneyFormat.format(null, new BigDecimal("2700"));
        assertEquals("2,700.00", written);
        assertTrue(!written.contains("USD"),
            "inventing USD is how a shilling balance gets paid in dollars");
    }

    @Test
    @DisplayName("no amount means no figure, not zero")
    void nullMeansNull() {
        /* Zero would read as a settled balance. */
        assertNull(MoneyFormat.format("USD", null));
    }

    @Test
    @DisplayName("both argument orders agree")
    void theTwoSpellingsAgree() {
        assertEquals(MoneyFormat.format("USD", new BigDecimal("12.5")),
            MoneyFormat.of(new BigDecimal("12.5"), "USD"));
    }

    // ── nobody reintroduces a currency symbol ───────────────────────────────────────────────────

    @Test
    @DisplayName("no service formats money with a currency symbol")
    void noSymbolDialectComesBack() throws IOException {
        Path src = Path.of("src/main/java");
        List<String> offenders = new ArrayList<>();

        try (Stream<Path> files = Files.walk(src)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                if (file.getFileName().toString().equals("MoneyFormat.java")) {
                    continue;
                }
                String body = Files.readString(file);
                if (body.contains("getCurrencyInstance")) {
                    offenders.add(src.relativize(file).toString());
                }
            }
        }

        assertTrue(offenders.isEmpty(),
            "These format money with a currency symbol instead of MoneyFormat, so the same debt "
                + "reads differently on different documents: " + offenders);
    }
}
