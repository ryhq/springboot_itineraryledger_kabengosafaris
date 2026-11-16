package com.itineraryledger.kabengosafaris.Quote;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A quotation that has been decided no longer stands as written.
 *
 * <p>{@code isValid} is a stored flag and it drifted. Nothing cleared it when a quotation was
 * cancelled, rejected or converted; the nightly {@code QuoteExpiryScheduler} only sweeps READY and
 * SENT, so a DRAFT that ran out months ago kept it; and {@code QuoteEditPolicy} refuses every
 * change once a status is decided, so it could never be corrected afterwards either. On Kabengo,
 * 20 of 26 quotations carried it, including cancelled, rejected and converted ones, and one that
 * expired five months earlier.
 *
 * <p>The moment the decision is recorded is the only point that still can clear it, so that is
 * where it is cleared.
 *
 * <p>ACCEPTED is deliberately absent: an accepted quotation does still stand, and is waiting to
 * become a booking.
 */
class AClosedQuotationStopsStandingTest {

    private static final Path SERVICE = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/Quote/Services/"
            + "QuoteServices/QuoteStatusService.java");

    @Test
    @DisplayName("cancel, reject, expire and convert all clear the validity flag")
    void everyClosingTransitionClearsIt() throws IOException {
        String body = Files.readString(SERVICE);

        for (String status : List.of("REJECTED", "CANCELLED", "EXPIRED", "CONVERTED")) {
            int set = body.indexOf("quote.setStatus(QuoteStatus." + status + ");");
            assertTrue(set >= 0, status + " transition is gone");

            /*
             * The clear must sit between setting the status and the save, so it cannot be
             * separated from the decision it belongs to.
             */
            int save = body.indexOf("quoteRepository.save(quote)", set);
            String between = body.substring(set, save);
            assertTrue(between.contains("setIsValid(false)"),
                status + " records the decision but leaves isValid standing. The edit policy "
                    + "refuses changes to a decided quotation, so nothing downstream can fix it.");
        }
    }

    @Test
    @DisplayName("an accepted quotation still stands")
    void acceptedIsNotCleared() throws IOException {
        String body = Files.readString(SERVICE);
        int accepted = body.indexOf("quote.setStatus(QuoteStatus.ACCEPTED);");
        assertTrue(accepted >= 0, "the accept transition is gone");

        int save = body.indexOf("quoteRepository.save(quote)", accepted);
        assertTrue(!body.substring(accepted, save).contains("setIsValid(false)"),
            "an accepted quotation is waiting to become a booking and still stands as written");
    }
}
