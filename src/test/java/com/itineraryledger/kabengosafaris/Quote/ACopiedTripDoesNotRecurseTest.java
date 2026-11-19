package com.itineraryledger.kabengosafaris.Quote;

import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteTreeCopyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Copying a quotation's trip onto a new version must not recurse forever.
 *
 * <p>A quote holds its days and every day holds its quote back. Both classes are {@code @Data},
 * so Lombok writes {@code equals}, {@code hashCode} and {@code toString} over <b>every</b> field
 * including that back-reference - and a cycle between two such classes is unbounded recursion the
 * moment anything compares, hashes or prints one.
 *
 * <p>This is not theoretical. Creating a new version of a quotation failed with a bare
 * StackOverflowError for every quote in both companies, which is why no quotation had ever
 * reached v2: the error arrived as "An unexpected error occurred" with nothing naming the cause,
 * because an {@code Error} is not an {@code Exception} and slipped past every catch on the way
 * out. Found on 2026-11-19 from the audit log, which had recorded the return value the HTTP
 * response never carried.
 *
 * <p>The guard is deliberately on the plain objects. It needs no database, no Spring context and
 * no fixtures, so it runs in milliseconds and fails the build the moment somebody adds a
 * bidirectional {@code @Data} pair back into this tree.
 */
class ACopiedTripDoesNotRecurseTest {

    private static Quote quoteWithDays(int dayCount) {
        Quote quote = Quote.builder()
            .id(1L)
            .quoteCode("QT-TEST-1")
            .title("A trip")
            .days(new ArrayList<>())
            .build();

        for (int n = 1; n <= dayCount; n++) {
            QuoteDay day = QuoteDay.builder()
                .id((long) n)
                .dayNumber(n)
                .dayTag("Day " + n)
                .title("Day " + n)
                .isOvernight(true)
                .build();
            /* The back-reference is the whole point: without it there is no cycle to find. */
            day.setQuote(quote);
            quote.getDays().add(day);
        }
        return quote;
    }

    @Test
    @DisplayName("a quote and its days can be hashed without recursing")
    void hashingDoesNotRecurse() {
        Quote quote = quoteWithDays(8);
        assertDoesNotThrow(quote::hashCode,
            "Quote.hashCode walks its days, and each day walks back to the quote");
        assertDoesNotThrow(() -> quote.getDays().get(0).hashCode(),
            "QuoteDay.hashCode walks its quote, and the quote walks back to its days");
    }

    @Test
    @DisplayName("a quote and its days can be printed without recursing")
    void printingDoesNotRecurse() {
        Quote quote = quoteWithDays(8);
        assertDoesNotThrow(quote::toString,
            "a log line that prints a quote must not hang the thread it runs on");
        assertDoesNotThrow(() -> quote.getDays().get(0).toString());
    }

    @Test
    @DisplayName("a quote and its days can be compared without recursing")
    void comparingDoesNotRecurse() {
        Quote a = quoteWithDays(8);
        Quote b = quoteWithDays(8);
        assertDoesNotThrow(() -> a.equals(b),
            "List.contains and List.remove compare days, which compare their quotes");
        assertDoesNotThrow(() -> a.getDays().contains(b.getDays().get(0)));
    }

    @Test
    @DisplayName("the whole trip copies onto a new version")
    void copyTreeDoesNotRecurse() {
        Quote source = quoteWithDays(8);
        Quote target = Quote.builder()
            .id(2L)
            .quoteCode("QT-TEST-2")
            .title("A trip, version 2")
            .days(new ArrayList<>())
            .build();

        QuoteTreeCopyService copier = new QuoteTreeCopyService();

        int copied = assertDoesNotThrow(() -> copier.copyTree(source, target),
            "creating a new version of a quotation must not overflow the stack");

        assertEquals(8, copied, "every day travels");
        List<QuoteDay> days = target.getDays();
        assertEquals(8, days.size());
        for (int i = 0; i < days.size(); i++) {
            assertEquals(i + 1, days.get(i).getDayNumber(), "the copy keeps the day's position");
            assertEquals(target, days.get(i).getQuote(), "and belongs to the new version");
        }
    }
}
