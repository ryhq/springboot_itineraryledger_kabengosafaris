package com.itineraryledger.kabengosafaris.Quote;

import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemType;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemTypeScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A tax over the whole of a safari quote is wrong in both directions.
 *
 * <p>Park, crater and conservation fees are government charges. There is no VAT of ours inside them
 * to pass on, so charging 18% on top of a park fee invents a liability nobody owes and the client
 * pays it. Accommodation is where a tax, or a provision against a supplier invoicing beyond
 * contract, actually belongs.
 *
 * <p>On a real quote — the VENOT family's 13,177.74 — a blanket 18% takes 2,372. Aimed at the beds
 * alone it takes 1,282. The difference is what the client is charged for a risk their park fees do
 * not carry.
 */
class CategoryScopedTaxTest {

    @Test
    @DisplayName("saying nothing still means every line, so old quotes keep their totals")
    void silenceMeansEverything() {
        assertTrue(QuoteItemTypeScope.covers(null, QuoteItemType.PARK_FEE));
        assertTrue(QuoteItemTypeScope.covers("", QuoteItemType.ACCOMMODATION));
        assertTrue(QuoteItemTypeScope.covers("   ", QuoteItemType.ACTIVITY));
        assertEquals(EnumSet.allOf(QuoteItemType.class), QuoteItemTypeScope.parse(null),
            "a quote written before this said nothing, and nothing has always meant all of them");
    }

    @Test
    @DisplayName("a named scope covers what it names and nothing else")
    void aNamedScopeIsExact() {
        assertTrue(QuoteItemTypeScope.covers("ACCOMMODATION", QuoteItemType.ACCOMMODATION));
        assertFalse(QuoteItemTypeScope.covers("ACCOMMODATION", QuoteItemType.PARK_FEE),
            "this is the whole point: the park fee must not be taxed with the bed");
        assertFalse(QuoteItemTypeScope.covers("ACCOMMODATION", QuoteItemType.ACTIVITY));

        assertTrue(QuoteItemTypeScope.covers("ACCOMMODATION,ACTIVITY", QuoteItemType.ACTIVITY));
        assertFalse(QuoteItemTypeScope.covers("ACCOMMODATION,ACTIVITY", QuoteItemType.PARK_FEE));
    }

    @Test
    @DisplayName("it is forgiving about how somebody types it")
    void spacingAndCaseDoNotMatter() {
        assertTrue(QuoteItemTypeScope.covers(" accommodation , activity ", QuoteItemType.ACTIVITY));
        assertEquals("ACCOMMODATION,ACTIVITY",
            QuoteItemTypeScope.canonical(QuoteItemTypeScope.parse("activity,accommodation")),
            "stored one way whatever was typed, so two quotes with the same scope compare equal");
    }

    @Test
    @DisplayName("a scope naming every category is stored as nothing")
    void everythingIsStoredAsNull() {
        assertEquals(null, QuoteItemTypeScope.canonical(EnumSet.allOf(QuoteItemType.class)),
            "otherwise the same meaning would be written two different ways");
        assertEquals(null, QuoteItemTypeScope.canonical(EnumSet.noneOf(QuoteItemType.class)));
    }

    @Test
    @DisplayName("a scope of only unknown names taxes everything, never nothing")
    void anUnreadableScopeFailsUpward() {
        /*
         * If a category is renamed one day, a scope naming the old name must not silently tax
         * nothing: a figure that is too high gets questioned by whoever reads the quote, one that
         * is too low gets invoiced, sent, and lost.
         */
        assertEquals(EnumSet.allOf(QuoteItemType.class), QuoteItemTypeScope.parse("SOMETHING_ELSE"));
        assertTrue(QuoteItemTypeScope.covers("SOMETHING_ELSE", QuoteItemType.PARK_FEE));
    }

    @Test
    @DisplayName("a line with no category is outside a named scope")
    void anUntypedLineIsNotSweptIn() {
        assertFalse(QuoteItemTypeScope.covers("ACCOMMODATION", null),
            "a line that cannot say what it is must not be taxed as though it were a bed");
        assertTrue(QuoteItemTypeScope.covers(null, null),
            "but with no scope at all, everything counts, as it always did");
    }

    @Test
    @DisplayName("it reads back in words, for a note on the quote")
    void itExplainsItself() {
        assertEquals("every line", QuoteItemTypeScope.describe(null));
        assertEquals("accommodation only", QuoteItemTypeScope.describe("ACCOMMODATION"));
        assertEquals("accommodation and park fee only",
            QuoteItemTypeScope.describe("PARK_FEE,ACCOMMODATION"));
    }
}
