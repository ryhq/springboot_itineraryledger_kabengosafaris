package com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemType;

/**
 * Moving a trip to another date reprices it. It must not also quietly remove our margin.
 *
 * <p>A new version with a new start date is the ordinary way a postponement is handled: the client
 * cannot travel in March after all, so June's rates apply. That path did not price the quotation —
 * it asked the ITINERARY estimator for a fresh estimate and wrote those figures onto the version
 * line by line. The agent's commission and the margin uplift are applied by
 * {@code QuoteCostEstimationService}, which that path never went through, so the new version came
 * out at cost plus the default margin with the markup gone, and read exactly like a correct one.
 * The only clue was a total that had moved for two reasons when the client had been told about one.
 *
 * <p>The same line threw away the customer's own trip. The itinerary is the product as it is sold
 * to strangers; the quotation's day tree is their copy of it, carrying the lodge they asked to swap
 * and the party that is actually travelling. Re-reading the itinerary handed them back the generic
 * product, and after the portal let clients make those swaps themselves, it would have discarded
 * their own work.
 *
 * <p>Both halves are fixed by pricing the version's own tree, through the one service that knows
 * about markup. These tests hold the markup down with arithmetic, and hold the route to it down by
 * reading the source: the arithmetic is only reached if the version path actually calls it.
 */
class ANewVersionAtANewDateKeepsItsMarkupTest {

    private static final Path VERSION_SERVICE = Path.of("src/main/java/com/itineraryledger/"
        + "kabengosafaris/Quote/Services/QuoteServices/QuoteVersionService.java");
    private static final Path COST_SERVICE = Path.of("src/main/java/com/itineraryledger/"
        + "kabengosafaris/Quote/Services/QuoteServices/QuoteCostEstimationService.java");

    /* Nulls throughout: computeMarkupMultiplier is arithmetic over the Quote it is handed. */
    private final QuoteCostEstimationService pricing = new QuoteCostEstimationService(
        null, null, null, null, null, null);

    // ------------------------------------------------------------------ what was being lost

    @Test
    @DisplayName("the markup the old path dropped is worth real money on a real quotation")
    void theMarkupIsNotARoundingDifference() {
        Quote quote = new Quote();
        quote.setAgentCommissionPercentage(new BigDecimal("10"));
        quote.setMarginUpliftPercentage(new BigDecimal("15"));

        BigDecimal multiplier = pricing.computeMarkupMultiplier(quote, QuoteItemType.ACCOMMODATION);

        assertTrue(multiplier.compareTo(BigDecimal.ONE) > 0,
            "if this is 1 the test proves nothing, and neither did the bug");

        BigDecimal atCost = new BigDecimal("18313.40");
        BigDecimal sold = atCost.multiply(multiplier);
        assertTrue(sold.subtract(atCost).compareTo(new BigDecimal("1000")) > 0,
            "a postponed trip was being requoted several thousand dollars under its own price");
    }

    // ------------------------------------------------------------------ the route to it

    @Test
    @DisplayName("a new date reprices the version's own trip, through the service that marks up")
    void theNewDateGoesThroughThePricingService() throws IOException {
        String source = Files.readString(VERSION_SERVICE);

        assertTrue(source.contains("quoteCostEstimation.recalculate(newQuote.getId())"),
            "repricing anywhere but here is repricing without the commission and the uplift");
        assertTrue(source.contains("dateChanged && daysCopied > 0"),
            "the good path must be chosen whenever the version carries a trip to price");
    }

    @Test
    @DisplayName("the itinerary is read only when there is no day tree to price instead")
    void theItineraryIsTheLastResortAndNotTheFirst() throws IOException {
        String source = Files.readString(VERSION_SERVICE);

        int good = source.indexOf("dateChanged && daysCopied > 0");
        int legacy = source.indexOf("else if (dateChanged && original.getItinerary() != null)");

        assertTrue(good > 0 && legacy > good,
            "the itinerary branch is for quotes written before the day tree existed, so it must "
                + "sit behind the branch that prices the trip the customer actually has");
    }

    @Test
    @DisplayName("a hand-written line survives the date change")
    void aTypedLineIsNotLostWithTheSeason() throws IOException {
        String source = Files.readString(VERSION_SERVICE);
        int body = source.indexOf("private int repriceAtTheNewDate");
        assertTrue(body > 0, "the repricing step has been renamed or removed");

        String method = source.substring(body, source.indexOf("\n    private int regenerateItems", body));
        assertTrue(method.contains("deepCopyItems(original, newQuote)"),
            "copying first is what keeps a transfer or a negotiated supplement on the version: "
                + "recalculate replaces only the lines it can regenerate");

        int copy = method.indexOf("deepCopyItems");
        int reprice = method.indexOf("recalculate");
        assertTrue(copy < reprice, "copying after the repricing would overwrite the new prices");
        assertTrue(method.indexOf("flush()") > copy && method.indexOf("flush()") < reprice,
            "recalculate deletes the derived lines with a query, so the copies must be in the "
                + "database before it runs or it will delete nothing and double the lines");
    }

    @Test
    @DisplayName("an estimator that cannot answer leaves a version with prices, not an empty one")
    void aFailedRepriceStillLeavesTheLines() throws IOException {
        String source = Files.readString(VERSION_SERVICE);
        int body = source.indexOf("private int repriceAtTheNewDate");
        String method = source.substring(body, source.indexOf("\n    private int regenerateItems", body));

        assertTrue(method.contains("log.warn"),
            "a version that could not be repriced carries the old season's prices and nobody "
                + "can see that from the document");
        assertFalse(method.contains("return 0;"),
            "returning nothing is what the old path did on failure: a version with no lines at all");
    }

    // ------------------------------------------------------------------ the arithmetic is where we think

    @Test
    @DisplayName("the markup really does live on the path the version now takes")
    void theMultipliersAreAppliedWhereWeSendIt() throws IOException {
        String source = Files.readString(COST_SERVICE);

        int recalc = source.indexOf("public int recalculate(Long quoteId)");
        int persist = source.indexOf("persistItems", recalc);
        assertTrue(recalc > 0 && persist > recalc, "recalculate no longer writes the items");

        int writer = source.indexOf("private int persistItems");
        assertTrue(source.indexOf("computeMarkupMultiplier", writer) > writer,
            "if the multipliers leave persistItems, sending the version here fixes nothing");

        assertTrue(source.contains("quote.getSafariStartDate()"),
            "the new season is only read if the quote's own start date is what it prices at");
    }

    @Test
    @DisplayName("the version carries the new date and the rate basis before it is priced")
    void theVersionKnowsWhatItIsBeingPricedAs() throws IOException {
        String source = Files.readString(VERSION_SERVICE);

        assertTrue(source.contains(".safariStartDate(resolvedStartDate)"),
            "recalculate prices at the quote's own start date, so the override has to be on it");
        assertTrue(source.contains(".isStoRate(resolvedStoRate)"),
            "and a quote with no recorded basis is priced at our cost");
    }

    @Test
    @DisplayName("commission and uplift are both carried into the new version")
    void bothHalvesOfTheMarkupTravel() throws IOException {
        String source = Files.readString(VERSION_SERVICE);

        assertTrue(source.contains(".agentCommissionPercentage(original.getAgentCommissionPercentage())"));
        assertTrue(source.contains(".marginUpliftPercentage(original.getMarginUpliftPercentage())"));
        assertTrue(source.contains(".marginUpliftAppliesTo(original.getMarginUpliftAppliesTo())"),
            "the scope travels with its percentage, or the uplift lands on categories nobody "
                + "scoped it to");

        Quote carried = new Quote();
        carried.setAgentCommissionPercentage(new BigDecimal("10"));
        assertEquals(0, pricing.computeMarkupMultiplier(carried, QuoteItemType.ACCOMMODATION)
            .compareTo(new BigDecimal("1.10")),
            "copying the percentage is only useful if the pricing path reads it");
    }
}
