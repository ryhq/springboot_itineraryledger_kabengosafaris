package com.itineraryledger.kabengosafaris.Inclusion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.Inclusion.Entity.InclusionItem;
import com.itineraryledger.kabengosafaris.Inclusion.Entity.InclusionsSource;
import com.itineraryledger.kabengosafaris.Inclusion.Services.InclusionSnapshotService;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryInclusion.Entity.ItineraryInclusion;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.QuoteInclusion.Entity.QuoteInclusion;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.SafariInclusion.Entity.SafariInclusion;

/**
 * Itinerary → Quotation → Safari → Invoice: each copies from the one above it and owns what it has.
 *
 * <p>The worked example is the one the owner described. Five lines on the itinerary; two more added
 * while negotiating the quote; the safari and the invoice that follow carry seven, not five. That
 * only works if each document holds its own rows and each copy reads its immediate parent.
 *
 * <p>Two of these pin mistakes that are one careless line away. A safari built from a quote must
 * read the QUOTE — the template already gets this wrong for highlights at
 * {@code full_safari_modern.html:718}, where it renders {@code safari.itinerary.highlightsList}. And
 * a document already sent must be immune to a later catalogue edit, which is the whole reason these
 * rows carry text rather than a foreign key.
 */
class ThePromiseTravelsDownTheChainTest {

    private final InclusionSnapshotService snapshot = new InclusionSnapshotService();

    @Test
    @DisplayName("an edit on the quote reaches the safari and the invoice, and never the itinerary")
    void theChainFlowsOneWay() {
        Itinerary itinerary = itineraryWith(
            "Private 4×4 safari vehicle with pop-up roof",
            "Professional multilingual safari guide",
            "All park, conservation & crater-service fees",
            "Accommodation with meals as listed in the itinerary",
            "Airport transfers on arrival & departure");

        Quote quote = new Quote();
        assertEquals(5, snapshot.itineraryToQuote(itinerary, quote));

        /* Two lines negotiated onto the quote, exactly as a salesperson would add them. */
        quote.addInclusion(QuoteInclusion.builder()
            .label("Return flight Seronera to Zanzibar").isIncluded(true).sortOrder(6).build());
        quote.addInclusion(QuoteInclusion.builder()
            .label("Zanzibar hotel transfers").isIncluded(true).sortOrder(7).build());

        Safari safari = new Safari();
        assertEquals(7, snapshot.quoteToSafari(quote, safari), "the safari inherits the quote's seven");

        Invoice invoice = new Invoice();
        assertEquals(7, snapshot.toInvoice(safari, quote, invoice), "and so does the bill");

        assertEquals(5, itinerary.getInclusionList().size(),
            "the template must be untouched — the next customer's quote starts from five again");
    }

    @Test
    @DisplayName("a safari built from a quote reads the quote, not the template behind it")
    void theSafariReadsTheQuote() {
        /*
         * The itinerary says one thing and the accepted quote says another. Reading the itinerary
         * here would deliver the trip against a promise the customer never saw.
         */
        Itinerary itinerary = itineraryWith("Half board at the coast");
        Quote quote = new Quote();
        quote.addInclusion(QuoteInclusion.builder()
            .label("Full board throughout").isIncluded(true).sortOrder(1).build());

        Safari safari = new Safari();
        snapshot.quoteToSafari(quote, safari);

        assertIterableEquals(List.of("Full board throughout"), labels(safari));
        assertTrue(itinerary.getInclusionList().size() == 1, "sanity: the template still says its own thing");
    }

    @Test
    @DisplayName("a quote already sent is not rewritten by a later catalogue edit")
    void aSentQuoteIsFrozen() {
        InclusionItem item = item("All park, conservation & crater-service fees");
        Itinerary itinerary = new Itinerary();
        itinerary.addInclusion(ItineraryInclusion.builder()
            .inclusionItem(item).isIncluded(true).sortOrder(1).build());

        Quote quote = new Quote();
        snapshot.itineraryToQuote(itinerary, quote);

        /* Somebody tidies the wording, and disables the line entirely, months later. */
        item.setLabel("Park fees where stated");
        item.setIsActive(false);

        assertIterableEquals(List.of("All park, conservation & crater-service fees"), labels(quote),
            "the customer's copy says the old words; ours must say the same or ours looks altered");
    }

    @Test
    @DisplayName("a line disabled before the copy is dropped; one disabled after it is not")
    void disablingActsOnNewPaperworkOnly() {
        InclusionItem live = item("Drinking water on game drives");
        InclusionItem retired = item("Complimentary sundowner");
        retired.setIsActive(false);

        Itinerary itinerary = new Itinerary();
        itinerary.addInclusion(ItineraryInclusion.builder().inclusionItem(live).isIncluded(true).sortOrder(1).build());
        itinerary.addInclusion(ItineraryInclusion.builder().inclusionItem(retired).isIncluded(true).sortOrder(2).build());

        Quote quote = new Quote();
        assertEquals(1, snapshot.itineraryToQuote(itinerary, quote),
            "a retired line must not reach a new quote");
        assertIterableEquals(List.of("Drinking water on game drives"), labels(quote));
    }

    @Test
    @DisplayName("the invoice falls back to the quote for a trip booked before any of this existed")
    void theInvoiceFallsBackRatherThanThrowing() {
        Quote quote = new Quote();
        quote.addInclusion(QuoteInclusion.builder()
            .label("All park, conservation & crater-service fees").isIncluded(true).sortOrder(1).build());

        Safari legacySafari = new Safari();   // no rows: created before the chain
        Invoice invoice = new Invoice();

        assertEquals(1, snapshot.toInvoice(legacySafari, quote, invoice));
        assertIterableEquals(List.of("All park, conservation & crater-service fees"), labelsOf(invoice));

        /* And neither present is an invoice that says less, never an exception. */
        Invoice bare = new Invoice();
        assertEquals(0, snapshot.toInvoice(new Safari(), null, bare));
        assertTrue(bare.getInclusionList().isEmpty());
    }

    @Test
    @DisplayName("every copy stamps where it came from, or Reset has nothing honest to say")
    void everyCopyIsStamped() {
        Itinerary itinerary = itineraryWith("Professional multilingual safari guide");
        Quote quote = new Quote();
        snapshot.itineraryToQuote(itinerary, quote);

        assertEquals(InclusionsSource.INHERITED, quote.getInclusionsSource());
        assertNotNull(quote.getInclusionsSyncedAt(),
            "without a date, a screen cannot say whether this still matches its parent");

        Safari safari = new Safari();
        snapshot.quoteToSafari(quote, safari);
        assertEquals(InclusionsSource.INHERITED, safari.getInclusionsSource());
        assertNotNull(safari.getInclusionsSyncedAt());
    }

    @Test
    @DisplayName("the claim travels with the wording, so a sent quote's warnings cannot change")
    void theClaimIsSnapshottedToo() {
        /*
         * Read live, narrowing a catalogue item's scope would change which contradictions a sent
         * quote reports — a warning somebody cleared before sending would reappear afterwards,
         * about a document they can no longer change.
         */
        InclusionItem item = item("All park, conservation & crater-service fees");
        item.setClaimAppliesTo("PARK_FEE");

        Itinerary itinerary = new Itinerary();
        itinerary.addInclusion(ItineraryInclusion.builder()
            .inclusionItem(item).isIncluded(true).sortOrder(1).build());

        Quote quote = new Quote();
        snapshot.itineraryToQuote(itinerary, quote);
        item.setClaimAppliesTo("ACCOMMODATION");

        assertEquals("PARK_FEE", quote.getInclusionList().get(0).getClaimAppliesTo());
    }

    /* ------------------------------------------------------------------ helpers */

    private Itinerary itineraryWith(String... labels) {
        Itinerary itinerary = new Itinerary();
        int order = 1;
        for (String label : labels) {
            itinerary.addInclusion(ItineraryInclusion.builder()
                .inclusionItem(item(label)).isIncluded(true).sortOrder(order++).build());
        }
        return itinerary;
    }

    private InclusionItem item(String label) {
        return InclusionItem.builder().label(label).isActive(true).build();
    }

    private List<String> labels(Quote quote) {
        return quote.getInclusionList().stream().map(QuoteInclusion::getLabel).toList();
    }

    private List<String> labels(Safari safari) {
        return safari.getInclusionList().stream().map(SafariInclusion::getLabel).toList();
    }

    private List<String> labelsOf(Invoice invoice) {
        return invoice.getInclusionList().stream().map(row -> row.getLabel()).toList();
    }
}
