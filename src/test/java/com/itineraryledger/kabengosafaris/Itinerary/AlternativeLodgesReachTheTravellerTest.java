package com.itineraryledger.kabengosafaris.Itinerary;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A lodge a trip OFFERS is still a lodge the trip stays at, as far as a traveller is concerned.
 *
 * <p>Two places used to disagree, and both filtered alternatives away: the public itinerary showed
 * only the lodge a night is priced on, so day one of the fourteen-day Northern Tanzania trip read
 * "Outpost Lodge" when the office offers Outpost Lodge <em>or</em> Kahawa House; and "Safaris that
 * stay here" asked the same question with the same filter, so Kahawa House's own page listed no
 * trips at all.
 *
 * <p>Measured across the live catalogue that filter left <b>43 of 64</b> lodge pages with an empty
 * carousel: pages that rank in search, describe a property well, then have nothing to sell.
 *
 * <p>Source-level assertions, like the published-trip test beside it, because the mistake is a
 * clause quietly returning to a WHERE and that is visible in the file long before anyone notices
 * an empty carousel on a page they never visit.
 */
class AlternativeLodgesReachTheTravellerTest {

    private static final Path REPOSITORY = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/Itinerary/ItineraryDay/"
            + "ItineraryDayAccommodation/Repository/ItineraryDayAccommodationRepository.java");

    private static final Path PUBLIC_SERVICE = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/Public/Services/PublicItineraryService.java");

    @Test
    @DisplayName("the safaris-that-stay-here query counts alternatives too")
    void carouselQueryIncludesAlternatives() throws IOException {
        String source = Files.readString(REPOSITORY);
        int start = source.indexOf("findActiveItineraryIdsByAccommodationId");
        assertTrue(start > 0, "the carousel query has been renamed; this test needs following");

        String query = source.substring(Math.max(0, start - 900), start);
        assertFalse(query.contains("da.isAlternative IS NULL OR da.isAlternative = false"),
            "findActiveItineraryIdsByAccommodationId is filtering alternatives out again.\n"
                + "That empties 'Safaris that stay here' on every lodge only ever offered as an\n"
                + "option: 43 of 64 lodge pages, last time it was measured.");
    }

    @Test
    @DisplayName("the public itinerary sends every lodge a day offers, not only the priced one")
    void publicDaysCarryAlternatives() throws IOException {
        String source = Files.readString(PUBLIC_SERVICE);
        int start = source.indexOf("List<PublicItineraryDTO.DayAccommodationDTO> accDTOs");
        assertTrue(start > 0, "the public day-accommodation mapping has moved; this test needs following");
        String block = source.substring(start, Math.min(source.length(), start + 1600));

        assertFalse(block.contains(".filter(da -> !Boolean.TRUE.equals(da.getIsAlternative()))"),
            "The public itinerary is dropping alternative lodges again. A reader then sees a\n"
                + "narrower trip than the one we sell, and the offered lodge gets no mention.");

        assertTrue(block.contains(".isAlternative("),
            "Alternatives are sent but not flagged, so the website cannot tell the traveller which\n"
                + "lodge the price is based on. That is worse than not showing them at all.");
    }

    @Test
    @DisplayName("an alternative's nights are counted on their own, not borrowed from the primary")
    void alternativeNightsAreCountedSeparately() throws IOException {
        assertTrue(Files.readString(PUBLIC_SERVICE).contains("nightsByAlternativeAcc"),
            "Alternatives are sharing the primary's night tally. A lodge offered on two nights\n"
                + "would report whatever the priced lodge had, and a lodge that is primary once and\n"
                + "an alternative once would claim two priced nights.");
    }
}
