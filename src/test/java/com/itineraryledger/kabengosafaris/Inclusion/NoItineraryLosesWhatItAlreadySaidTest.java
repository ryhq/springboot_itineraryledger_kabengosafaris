package com.itineraryledger.kabengosafaris.Inclusion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.Inclusion.Entity.InclusionItem;
import com.itineraryledger.kabengosafaris.Inclusion.Services.ItineraryInclusionReader;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryInclusion.Entity.ItineraryInclusion;

/**
 * Sixty-two live itineraries carry a promise a customer has already been shown. None of them may
 * lose a word of it on the day the catalogue arrives.
 *
 * <p>The safety is one rule: {@link ItineraryInclusionReader} answers from the catalogue rows when
 * there are any, and from the old text columns when there are none. That makes the backfill safe by
 * construction rather than by having been perfect — and it makes the whole change reversible, since
 * nothing overwrote the columns it falls back to.
 *
 * <p>The rule has a sharp edge, which is what most of this pins: the fallback is per itinerary, not
 * per line. Convert three of an itinerary's four lines and the fallback stops, taking the fourth
 * with it. Three itineraries have their entire promise typed as a single 300-character paragraph,
 * so those must be left whole rather than half-converted.
 */
class NoItineraryLosesWhatItAlreadySaidTest {

    private final ItineraryInclusionReader reader = new ItineraryInclusionReader();

    /** The exact text 58 live itineraries carry, so the assertions are about real data. */
    private static final String HOUSE_INCLUDED = String.join("\n",
        "Private 4×4 safari vehicle with pop-up roof",
        "Professional multilingual safari guide",
        "All park, conservation & crater-service fees");
    private static final String HOUSE_EXCLUDED = String.join("\n",
        "International flights & visas",
        "Travel & medical insurance");

    @Test
    @DisplayName("an itinerary with no rows still says exactly what it always said")
    void theFallbackServesTheTypedText() {
        Itinerary legacy = Itinerary.builder()
            .inclusions(HOUSE_INCLUDED)
            .exclusions(HOUSE_EXCLUDED)
            .build();

        assertTrue(reader.isLegacy(legacy));
        assertIterableEquals(
            List.of("Private 4×4 safari vehicle with pop-up roof",
                    "Professional multilingual safari guide",
                    "All park, conservation & crater-service fees"),
            reader.included(legacy));
        assertIterableEquals(
            List.of("International flights & visas", "Travel & medical insurance"),
            reader.excluded(legacy));
    }

    @Test
    @DisplayName("once it has rows, the rows are the answer and the old text is never consulted")
    void rowsWinOverTheTypedText() {
        /*
         * Both present at once is exactly the state a half-finished backfill would leave. If the
         * reader ever merged the two, every converted itinerary would print its promise twice.
         */
        Itinerary converted = Itinerary.builder()
            .inclusions("SHOULD NEVER BE READ")
            .exclusions("NOR THIS")
            .build();
        converted.addInclusion(row("Private 4×4 safari vehicle with pop-up roof", true, 1));
        converted.addInclusion(row("International flights & visas", false, 2));

        assertFalse(reader.isLegacy(converted));
        assertIterableEquals(List.of("Private 4×4 safari vehicle with pop-up roof"),
            reader.included(converted));
        assertIterableEquals(List.of("International flights & visas"),
            reader.excluded(converted));
    }

    @Test
    @DisplayName("a line printed on a customer's document is not the complement of the other list")
    void eachSideIsStatedNotDerived() {
        /*
         * A line that is on neither list belongs on neither list. Deriving "not included" as
         * everything not ticked would have meant one catalogue addition rewriting the exclusion
         * paragraph of every itinerary at once — a sentence sent to a paying customer that nobody
         * wrote and nobody reviewed.
         */
        Itinerary trip = Itinerary.builder().build();
        trip.addInclusion(row("Drinking water on game drives", true, 1));

        assertIterableEquals(List.of("Drinking water on game drives"), reader.included(trip));
        assertTrue(reader.excluded(trip).isEmpty(),
            "nothing may appear under \"not included\" that was not put there");
    }

    @Test
    @DisplayName("a line whose catalogue item was disabled stops printing, on both sides")
    void aDisabledLineStopsPrinting() {
        Itinerary trip = Itinerary.builder().build();
        ItineraryInclusion live = row("Flying-doctors emergency evacuation cover", true, 1);
        ItineraryInclusion retired = row("Drinks & premium beverages", false, 2);
        retired.getInclusionItem().setIsActive(false);
        trip.addInclusion(live);
        trip.addInclusion(retired);

        assertIterableEquals(List.of("Flying-doctors emergency evacuation cover"),
            reader.included(trip));
        assertTrue(reader.excluded(trip).isEmpty(),
            "a retired line must stop printing on the not-included side too, not only the included one");
    }

    @Test
    @DisplayName("the print order is the row's own, so reordering the catalogue changes nothing here")
    void theOrderIsTheRowsOwn() {
        /*
         * The collection is @OrderBy("sortOrder ASC, id ASC") in the database. This asserts the
         * reader preserves whatever order it is handed rather than sorting by label or by the
         * catalogue's displayOrder, either of which would silently reorder 65 printed promises.
         */
        Itinerary trip = Itinerary.builder().build();
        trip.addInclusion(row("Zebra", true, 1));
        trip.addInclusion(row("Aardvark", true, 2));

        assertIterableEquals(List.of("Zebra", "Aardvark"), reader.included(trip));
    }

    @Test
    @DisplayName("splitting is byte-compatible with what the website has been rendering")
    void splittingMatchesTheOldBehaviour() {
        /* Windows line endings, blank lines and stray spaces all occur in the live columns. */
        assertIterableEquals(List.of("One", "Two"),
            ItineraryInclusionReader.splitLines("One\r\n\r\n  Two  \n"));
        assertEquals(0, ItineraryInclusionReader.splitLines("   ").size());
        assertEquals(0, ItineraryInclusionReader.splitLines(null).size());
    }

    private ItineraryInclusion row(String label, boolean included, int order) {
        return ItineraryInclusion.builder()
            .inclusionItem(InclusionItem.builder().label(label).isActive(true).build())
            .isIncluded(included)
            .sortOrder(order)
            .build();
    }
}
