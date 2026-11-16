package com.itineraryledger.kabengosafaris.Safari;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A safari is driven in the cars its quote was priced for.
 *
 * <p>An itinerary is a product, written around a reference party - ITI-8D7N-1004 carries one
 * vehicle because it was written for two guests. A quote then sets its own: eight guests need two.
 * Conversion read the <b>itinerary's</b> figure, so QT-1003-1026-1, priced and sold with two
 * vehicles, became SAF-8D7N-01002 planned with one.
 *
 * <p>The money and the operations both go wrong, in opposite directions. The quote charged the
 * Ngorongoro crater service fee and the game drives twice over, per vehicle; the booking costs them
 * once. And the list that tells somebody what to send to Kilimanjaro on 31 December said one car
 * for eight people, which is a conversation at the airport rather than a line in a spreadsheet.
 *
 * <p>Pinned as a source check rather than a behavioural one because constructing a Quote, its
 * itinerary, its customer and the whole day snapshot to assert a single integer would test the
 * builder, not the decision. What is worth protecting is that this line reads from the quote.
 */
class ABookingTravelsInTheCarsItWasQuotedTest {

    private static final Path SERVICE = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/Safari/Services/SafariCreateService.java");

    @Test
    @DisplayName("converting a quote takes the quote's vehicle count, not the itinerary's")
    void conversionCarriesTheQuotesVehicleCount() throws IOException {
        String body = Files.readString(SERVICE);

        /*
         * The itinerary stays in the expression as the fallback, for a quote written before the
         * field existed, so this looks for the quote being asked FIRST rather than for the
         * itinerary being absent.
         */
        Pattern fromQuote = Pattern.compile(
            "\\.carCount\\(\\s*quote\\.getCarCount\\(\\)\\s*!=\\s*null", Pattern.DOTALL);

        assertTrue(fromQuote.matcher(body).find(),
            "createSafariFromQuote must take the QUOTE's carCount. Reading the itinerary's turns a "
                + "booking sold with two vehicles into one planned with a single car.");
    }

    @Test
    @DisplayName("the itinerary's count is still the fallback, never the first answer")
    void theItineraryIsOnlyTheFallback() throws IOException {
        String body = Files.readString(SERVICE);
        int fromQuote = body.indexOf(".carCount(quote.getCarCount()");
        assertTrue(fromQuote >= 0, "the quote-first branch is gone");

        /*
         * createSafariFromItinerary - a safari raised directly from a product, with no quote - is
         * a different method and legitimately reads the itinerary. Only the conversion path is
         * asserted here.
         */
        assertTrue(body.contains("itinerary.getCarCount()"),
            "the itinerary fallback was removed; a quote with no vehicle count recorded would "
                + "then convert to a safari with none either");
    }
}
