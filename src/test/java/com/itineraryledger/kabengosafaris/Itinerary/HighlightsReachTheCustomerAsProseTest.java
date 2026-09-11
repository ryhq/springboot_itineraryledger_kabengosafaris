package com.itineraryledger.kabengosafaris.Itinerary;

import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * "Trip Highlights" was printing JSON at customers.
 *
 * <p>An itinerary stores this field as a JSON array, because that is the shape the website wants.
 * The PDF templates printed the field straight out, so page one of every itinerary document ever
 * sent read {@code ["Tarangire elephants", "Ngorongoro Crater descent"]} — brackets, quotes and
 * commas included. 46 of the 60 itineraries in the catalogue are stored that way, so this was not
 * an edge case; it was the normal case.
 */
class HighlightsReachTheCustomerAsProseTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates/pdf-templates");

    @Test
    @DisplayName("a JSON array becomes the items, without its punctuation")
    void jsonBecomesAList() {
        FullItineraryDTO dto = new FullItineraryDTO();
        dto.setHighlights("[\"Tarangire elephants\", \"A full day in the Ngorongoro Crater\"]");

        assertEquals(List.of("Tarangire elephants", "A full day in the Ngorongoro Crater"),
            dto.getHighlightsList());
    }

    @Test
    @DisplayName("the same on the DTO the safari document reaches an itinerary through")
    void theNestedDtoAgrees() {
        ItineraryDTO dto = new ItineraryDTO();
        dto.setHighlights("[\"Two nights in the Serengeti\"]");

        assertEquals(List.of("Two nights in the Serengeti"), dto.getHighlightsList());
    }

    @Test
    @DisplayName("a plain sentence is left as one item, not chopped up")
    void plainTextSurvives() {
        FullItineraryDTO dto = new FullItineraryDTO();
        dto.setHighlights("Serengeti, Ngorongoro and the coast");

        assertEquals(List.of("Serengeti, Ngorongoro and the coast"), dto.getHighlightsList(),
            "commas inside a written sentence are punctuation, not separators");
    }

    @Test
    @DisplayName("several lines become several items")
    void linesBecomeItems() {
        FullItineraryDTO dto = new FullItineraryDTO();
        dto.setHighlights("Crater descent\nTwo nights on the plains\n");

        assertEquals(List.of("Crater descent", "Two nights on the plains"), dto.getHighlightsList());
    }

    @Test
    @DisplayName("nothing, or broken JSON, never puts braces in front of a customer")
    void nothingLeaksWhenItCannotBeRead() {
        FullItineraryDTO empty = new FullItineraryDTO();
        assertTrue(empty.getHighlightsList().isEmpty(), "null highlights print nothing at all");

        empty.setHighlights("   ");
        assertTrue(empty.getHighlightsList().isEmpty());

        FullItineraryDTO broken = new FullItineraryDTO();
        broken.setHighlights("[\"unterminated");
        /*
         * Shown as written rather than swallowed: a highlight somebody typed is worth more to the
         * reader than a blank box, and a stray bracket is at least visible enough to get fixed.
         */
        assertEquals(List.of("[\"unterminated"), broken.getHighlightsList());
    }

    @Test
    @DisplayName("no PDF template prints an itinerary's highlights field raw")
    void noTemplatePrintsTheRawField() throws Exception {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.list(TEMPLATES)) {
            for (Path template : files.filter(f -> f.toString().endsWith(".html")).toList()) {
                String html = Files.readString(template);
                for (String raw : List.of("${itinerary.highlights}", "${safari.itinerary.highlights}")) {
                    if (html.contains(raw)) {
                        offenders.add(template.getFileName() + " prints " + raw);
                    }
                }
            }
        }
        assertTrue(offenders.isEmpty(),
            "print highlightsList and iterate it, or the reader sees the JSON: " + offenders);
    }
}
