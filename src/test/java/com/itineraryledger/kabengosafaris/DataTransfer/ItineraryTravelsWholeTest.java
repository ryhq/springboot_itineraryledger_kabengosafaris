package com.itineraryledger.kabengosafaris.DataTransfer;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The rules an itinerary travels under.
 *
 * It is the first module in the bundle with real depth. An accommodation goes three levels down; an
 * itinerary goes five, and it is the first one where a partial import produces something that LOOKS
 * complete: five days of six, or a day whose lodge quietly failed to resolve, is a product somebody
 * will quote a customer from.
 */
class ItineraryTravelsWholeTest {

    private static final Path MODULE = Path.of("src/main/java/com/itineraryledger/kabengosafaris/"
        + "DataTransfer/Modules/ItineraryTransfer.java");
    private static final Path LINKS = Path.of("src/main/java/com/itineraryledger/kabengosafaris/"
        + "DataTransfer/Modules/ParkActivityTransfer.java");

    private String module() throws IOException { return Files.readString(MODULE); }

    @Test
    @DisplayName("an itinerary arrives as a DRAFT, whatever it was where it came from")
    void nothingArrivesPublished() throws IOException {
        String source = module();
        assertTrue(source.contains("setStatus(Itinerary.ItineraryStatus.DRAFT)"),
            "every imported itinerary must be a draft: publishing is this company saying it has "
                + "checked the trip and will sell it, which another company cannot assert for it");
        assertTrue(source.contains("\"code\", \"status\""),
            "status must be excluded from the copied columns, or the incoming value would overwrite "
                + "the DRAFT that was just set");
    }

    @Test
    @DisplayName("a missing reference refuses the whole itinerary rather than writing part of one")
    void partialImportsAreRefused() throws IOException {
        String source = module();
        assertTrue(source.contains("class Missing extends RuntimeException"),
            "there must be one way to abandon an itinerary, used by every resolution");
        assertTrue(source.contains("catch (Missing missing)") && source.contains("outcome.unresolved("),
            "an abandoned itinerary is reported as unresolved with the missing reference named");

        /* Every reference an itinerary makes has to be able to refuse. */
        for (String refusal : new String[] {
            "no park '", "no activity '", "no tariff '", "no accommodation '",
            "has no room type '", "has no room standard '", "has no board type '",
            "no guest residency called '", "no guest age band called '",
        }) {
            assertTrue(source.contains(refusal), "nothing refuses with: " + refusal);
        }
    }

    @Test
    @DisplayName("the code is the identity, and a code taken locally does not silently overwrite")
    void identityIsTheCode() throws IOException {
        String source = module();
        assertTrue(source.contains("itineraries.findByCode(code)"),
            "identity is the code, which is the only stable name an itinerary has");
        assertTrue(source.contains("codeTakenByAnother"),
            "same code and a different name is a local collision, not the same itinerary: codes are "
                + "derived from a row id, so two companies produce the same one for different trips");
    }

    @Test
    @DisplayName("prices do not travel; the receiving company prices it with its own rates")
    void pricesDoNotTravel() throws IOException {
        String source = module();
        for (String priced : new String[] {"stoRate", "rackRate", "stoTotalPrice", "currency"}) {
            assertFalse(source.contains(priced),
                "an itinerary must carry structure only: " + priced + " belongs to the receiving "
                    + "company's own rate sheet, or the same trip would quote in another company's money");
        }
    }

    @Test
    @DisplayName("the park-activity links travel, because an in-park activity is silently dropped without them")
    void theLinksTravelToo() throws IOException {
        /*
         * The panel's own endpoint answers 200 and stores NOTHING when a park is not recorded as
         * offering an activity. That is how an optional Maasai boma visit vanished from a freshly
         * built itinerary while the run log said it had been added. The link has to arrive first,
         * and the itinerary module has to refuse loudly rather than repeat the silence.
         */
        assertTrue(Files.exists(LINKS), "the park-activities module has gone");
        assertTrue(module().contains("does not offer '"),
            "importing an in-park activity with no link must refuse, not skip quietly");
        assertTrue(module().contains("\"park-activities\""),
            "itineraries must declare park-activities as a dependency, or the links arrive after "
                + "the itineraries that need them");
    }

    @Test
    @DisplayName("order is carried, because a day out of sequence is a different trip")
    void orderSurvives() throws IOException {
        String source = module();
        assertTrue(source.contains("OrderByDayNumberAsc"), "days must be exported in day order");
        assertTrue(source.contains("OrderBySortOrderAsc"),
            "park visits and activities carry a sort order that has to survive the journey");
    }
}
