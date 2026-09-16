package com.itineraryledger.kabengosafaris.DataTransfer;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;

/**
 * A module may reuse a column's name for its own nested data, and importing must not choke on it.
 *
 * <p>An itinerary's promise used to be the free-text column {@code inclusions}. It is rows now, and
 * the exporter writes them as an array under that same key — the wording is the only name a line has
 * that means anything in another company, so the array is what travels.
 *
 * <p>{@link Scalars#apply} then handed the array to Jackson for a {@code String} field that still
 * exists on the entity, and Jackson threw:
 *
 * <pre>
 *   Could not apply budgetCategory, carCount, …, inclusions to Itinerary
 *   at Itinerary["inclusions"]: Cannot deserialize value of type `java.lang.String`
 *   from Array value (token `JsonToken.START_ARRAY`)
 * </pre>
 *
 * <p>The throw happens per itinerary inside the one transaction the whole bundle runs in, so it took
 * every other module with it: importing a bundle containing itineraries answered 500 outright, in
 * both companies, and the message named fourteen fields while blaming none of them. It survived
 * review because every test in this package reads the module's source text, and source text cannot
 * see a shape mismatch.
 *
 * <p>This is the same family as {@link RateRowsCarryNamesTest} — a field that is known and the wrong
 * shape — and it is caught the same way, by removing it before Jackson is asked.
 */
class ANestedArrayNeverLandsOnAColumnTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private static final Path MODULE = Path.of("src/main/java/com/itineraryledger/kabengosafaris/"
        + "DataTransfer/Modules/ItineraryTransfer.java");

    /** An itinerary row in the shape the exporter actually writes one. */
    private ObjectNode exportedItineraryRow() {
        ObjectNode row = mapper.createObjectNode();
        row.put("name", "9 Days Safari & Zanzibar");
        row.put("totalDays", 9);
        row.put("isActive", true);

        ArrayNode inclusions = row.putArray("inclusions");
        ObjectNode line = inclusions.addObject();
        line.put("item", "Private 4×4 safari vehicle with pop-up roof");
        line.put("included", true);
        line.put("order", 1);

        /* Nested collections the module writes itself, as they appear beside the promise. */
        row.putArray("days");
        row.putArray("pax");
        return row;
    }

    @Test
    @DisplayName("an array under a column's name is dropped, not forced into the column")
    void theArrayThatAnsweredFiveHundred() {
        Itinerary itinerary = new Itinerary();

        assertDoesNotThrow(() -> Scalars.apply(mapper, exportedItineraryRow(), itinerary),
            "a nested array under a scalar column's name must be dropped; forcing it into the "
                + "column throws, and one throw fails the whole bundle");

        assertEquals("9 Days Safari & Zanzibar", itinerary.getName(),
            "the real columns beside it must still be applied");
        assertEquals(9, itinerary.getTotalDays());
        assertNull(itinerary.getInclusions(),
            "the legacy column must be left alone — the rows are the promise now");
    }

    @Test
    @DisplayName("a nested object under a column's name is dropped too")
    void objectsAsWellAsArrays() {
        Itinerary itinerary = new Itinerary();
        ObjectNode row = mapper.createObjectNode();
        row.put("name", "Ngorongoro day trip");
        row.putObject("inclusions").put("anything", "at all");

        assertDoesNotThrow(() -> Scalars.apply(mapper, row, itinerary));
        assertEquals("Ngorongoro day trip", itinerary.getName());
        assertNull(itinerary.getInclusions());
    }

    @Test
    @DisplayName("the itinerary module keeps the legacy columns out of the scalar copy")
    void theModuleSaysSoAtItsCallSite() throws IOException {
        String source = Files.readString(MODULE);

        assertTrue(source.contains("Scalars.of(mapper, itinerary, \"inclusions\", \"exclusions\")"),
            "the export must not write the legacy text under the key the rows use, or the row's "
                + "shape depends on which of two lines ran last");
        assertTrue(source.contains("\"code\", \"status\", \"inclusions\", \"exclusions\""),
            "the import must exclude them at the call site as well as rely on the guard, so the "
                + "next reader sees that the key is nested data this class writes itself");
    }

    @Test
    @DisplayName("re-importing replaces the promise instead of appending a second copy")
    void overwritingDoesNotDoubleTheList() throws IOException {
        assertTrue(Files.readString(MODULE).contains("itineraryInclusions.deleteByItineraryId"),
            "the overwrite branch deletes the days and the pax; without the inclusions beside them "
                + "a second import prints every line of the promise twice");
    }

    @Test
    @DisplayName("an itinerary whose promise is still prose does not lose it")
    void theParagraphShapedOnesSurvive() throws IOException {
        String source = Files.readString(MODULE);

        assertTrue(source.contains("legacyInclusions") && source.contains("legacyExclusions"),
            "the few itineraries the backfill left as prose have no rows, so the array is empty and "
                + "the text is all they have; it has to travel under its own key");
        assertTrue(source.contains("if (inclusionRows.isEmpty())"),
            "and only for those — where rows exist they are the promise, and the old text is "
                + "residue that V18 removes");
    }
}
