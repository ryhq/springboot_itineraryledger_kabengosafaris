package com.itineraryledger.kabengosafaris.Flight;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A day's tree is copied in seven places, and a copier that forgets a child drops it in silence.
 *
 * <p>Nothing fails when it happens. The duplicated itinerary opens, the generated quote prices, the
 * safari runs — they are simply missing a leg, and the first person to notice is a client at an
 * airstrip with no seat. The inclusion work hit this exact failure twice before it was believed.
 *
 * <p>So the rule is structural: <b>wherever a copier carries accommodations, it must carry
 * flights.</b> Accommodations are the reference child because every one of these seven copies them
 * — they are the oldest and most complete member of the day tree — so "handles accommodations" is a
 * reliable way to ask "is this a day-tree copier?" without maintaining a hand-written list that the
 * eighth copier would not be added to.
 *
 * <p>This reads source text rather than running the copiers. That is a real limit and it is worth
 * naming: it proves the branch exists, not that it is correct. It is the cheap half of the
 * guarantee, and the half that actually decays — a new copier written next year is far likelier to
 * omit flights entirely than to copy them wrongly.
 */
class EveryDayTreeCopierCarriesFlightsTest {

    private static final Path SRC = Path.of("src/main/java/com/itineraryledger/kabengosafaris");

    /** The seven, and what each one would silently lose. */
    private static final Map<String, String> COPIERS = new LinkedHashMap<>() {{
        put("Itinerary/Services/ItineraryDuplicateService.java",
            "a duplicated itinerary would lose its flights");
        put("Itinerary/ItineraryDay/Services/ItineraryDayDuplicateService.java",
            "a duplicated day would lose its flights");
        put("DataTransfer/Modules/ItineraryTransfer.java",
            "an exported itinerary would arrive in the other company without its flights");
        put("Quote/Services/QuoteServices/QuoteFromItineraryGenerationService.java",
            "the quote built from a flying itinerary would not carry the flight");
        put("Quote/Services/QuoteServices/QuoteResyncAndTemplateService.java",
            "re-syncing a quote to its itinerary would strip the flights it already had");
        put("Safari/Services/SafariCreateService.java",
            "the safari sold from the quote would run without the seats it was sold with");
    }};

    @Test
    @DisplayName("every copier that carries accommodations carries flights too")
    void noneOfThemForgets() throws IOException {
        for (var entry : COPIERS.entrySet()) {
            Path file = SRC.resolve(entry.getKey());
            assertTrue(Files.exists(file), file + " has moved; this test's list must move with it");

            String source = Files.readString(file);
            assertTrue(source.contains("Accommodation"),
                file.getFileName() + " no longer copies accommodations, so it is no longer the "
                    + "shape this test assumes — check whether it is still a day-tree copier");
            assertTrue(source.contains("Flight"),
                file.getFileName() + " copies accommodations but not flights: "
                    + entry.getValue());
        }
    }

    @Test
    @DisplayName("SafariCreateService copies flights on BOTH of its paths")
    void theSafariHasTwoWaysIn() throws IOException {
        String source = Files.readString(SRC.resolve("Safari/Services/SafariCreateService.java"));

        /*
         * A safari is built either straight from an itinerary or from an accepted quote, and the
         * two walk different trees. Wiring only the itinerary path leaves every safari that came
         * from a quote — which is most of them — without its flights.
         */
        assertTrue(source.contains("copyDayFlights("),
            "the itinerary path must copy flights");
        assertTrue(source.contains("copyQuoteDayFlights("),
            "the quote path must copy flights as well; it is the path most safaris take");
    }

    @Test
    @DisplayName("the transfer module both writes flights and reads them back")
    void exportWithoutImportIsWorseThanNeither() throws IOException {
        String source = Files.readString(SRC.resolve("DataTransfer/Modules/ItineraryTransfer.java"));

        assertTrue(source.contains("dayRow.putArray(\"flights\")"),
            "the export must write the day's flights");
        assertTrue(source.contains("dayRow.path(\"flights\")"),
            "and the import must read them back; Scalars drops container nodes it was not told "
                + "about, so an export with no matching import loses them without a word");
        assertTrue(source.contains("\"accommodations\", \"flights\""),
            "\"flights\" must be named at the Scalars.apply call site as well as caught by the "
                + "container guard — the guard is the net, the exclusion is the intent");
        assertTrue(source.contains("\"flights\","),
            "and the flight catalogue must be in requires(), or an itinerary naming a sector "
                + "arrives before the sector exists and refuses itself");
    }
}
