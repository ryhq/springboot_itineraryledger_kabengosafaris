package com.itineraryledger.kabengosafaris.Flight;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A sector has to be findable by the words the screen puts on it.
 *
 * <p>The picker labels a sector "Arusha to Zanzibar". Typing that back matched nothing, because
 * the search was one LIKE against each column in turn and no column holds both ends of a flight.
 * The user sees a name, types the name, and is told there is no such sector.
 *
 * <p>Two things fix it and both are easy to undo by accident, so both are pinned here: the
 * keyword is split into words with every word required, and the joining words are dropped. "to"
 * is in the label we print and in no airstrip name, so requiring it rejects the exact phrase the
 * screen suggested.
 *
 * <p>Source text rather than a query, because this needs no database to go wrong and the whole
 * failure was visible in the one method.
 */
class ASectorIsFoundByWhatItIsCalledTest {

    private static final Path SERVICE = Path.of("src/main/java/com/itineraryledger/kabengosafaris/"
        + "Flight/Services/FlightRouteService.java");

    @Test
    @DisplayName("the sector search requires every word, not the whole phrase")
    void everyWordMustMatch() throws IOException {
        String source = Files.readString(SERVICE);

        assertTrue(source.contains("keyword.trim().toLowerCase().split("),
            "the keyword must be split into words; as a single LIKE, \"Arusha to Zanzibar\" matches "
                + "no column, because no column holds both ends of a sector");

        int spec = source.indexOf("private Specification<FlightRoute> buildSpec");
        assertTrue(spec > 0, "buildSpec has moved; this test must move with it");
        String body = source.substring(spec);

        assertFalse(body.contains("String like = \"%\" + keyword.trim().toLowerCase() + \"%\";"),
            "the whole phrase must not be one LIKE again — that is the bug this replaced");
    }

    @Test
    @DisplayName("the words that join two place names are ignored")
    void joinersAreDropped() throws IOException {
        String source = Files.readString(SERVICE);

        assertTrue(source.contains("JOINERS"),
            "there must be a joiner list, or every word being required rejects \"Arusha to "
                + "Zanzibar\" on the word \"to\"");
        assertTrue(source.contains("JOINERS.contains(word)"),
            "and the loop must actually skip them");

        int set = source.indexOf("JOINERS =");
        String decl = source.substring(set, source.indexOf(';', set));
        assertTrue(decl.contains("\"to\""),
            "\"to\" above all: it is the word the picker's own label uses");
    }
}
