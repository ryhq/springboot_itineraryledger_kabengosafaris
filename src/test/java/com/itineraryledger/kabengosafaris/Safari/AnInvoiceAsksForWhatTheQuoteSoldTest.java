package com.itineraryledger.kabengosafaris.Safari;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The invoice asks for what the quote sold.
 *
 * <p>There are three costing engines, not two, and this is the third. The Cost tab runs
 * {@code SafariCostEstimationOrchestrator}; a quote's items come from
 * {@code ItineraryCostEstimationService}; and an <b>invoice</b> is built from
 * {@code SafariCostEstimationService}. Fixing the first two left this one untouched, and it is the
 * one the customer is asked to pay from.
 *
 * <p>Two defects reached real invoices on 15 November 2026, both silent:
 *
 * <ul>
 *   <li><b>INV-000102</b> read <b>USD 246,877.73</b> against a booking of USD 24,655.40 plus
 *       TZS 222,222.00. The per-person loop summed every pax band into one figure and stamped it
 *       with whichever rate it read last, so the shillings were billed as dollars — a bill ten
 *       times the trip.</li>
 *   <li><b>INV-000103</b> read <b>USD 2,531.10</b> against USD 3,295.26. This service never priced
 *       flights at all, so the Arusha to Zanzibar sector showed on the Cost tab at USD 764.16 and
 *       was simply absent from the invoice.</li>
 * </ul>
 *
 * <p>The second is the dangerous shape: nothing looked wrong. An invoice that is short a sector
 * reads exactly like an invoice that is complete, and it is the customer's copy.
 */
class AnInvoiceAsksForWhatTheQuoteSoldTest {

    private static final Path SERVICE = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/Safari/Services/SafariCostEstimationService.java");

    @Test
    @DisplayName("the engine behind invoices keeps each currency apart")
    void everyBandIsBucketedByItsOwnCurrency() throws IOException {
        String body = Files.readString(SERVICE);

        /*
         * The shape that was wrong: a single running total, reassigned `currency` per band. The
         * shape that is right: a map keyed on the rate's own currency.
         */
        assertTrue(body.contains("totalByCurrency.merge(rateCurrency"),
            "the per-person loops must accumulate per currency; summing the bands and keeping the "
                + "last rate's currency bills shillings as dollars");

        Pattern lastRateWins = Pattern.compile(
            "totalPrice = totalPrice\\.add\\(paxCost\\);[\\s\\S]{0,200}?currency = rate\\.getCurrency\\(\\)");
        assertTrue(!lastRateWins.matcher(body).find(),
            "a per-person loop is summing into one total and reassigning the currency per band, "
                + "which is how USD 24,655 became USD 246,877");
    }

    @Test
    @DisplayName("the engine behind invoices prices the flights")
    void flightsAreCosted() throws IOException {
        String body = Files.readString(SERVICE);

        assertTrue(body.contains("calculateFlightCosts("),
            "this service must price flight sectors. The invoice generator has always asked for "
                + "getFlightCosts(); when nothing filled it, the sector vanished from the bill.");
        assertTrue(body.contains(".flightCosts(flightCosts)"),
            "the priced flights must reach the response, or they are computed and dropped");
    }

    @Test
    @DisplayName("flights reach the subtotal, not just their own breakdown")
    void flightsReachTheSubtotal() throws IOException {
        String body = Files.readString(SERVICE);

        /*
         * The breakdown and the subtotal are separate, and a line that reaches only the first is
         * visible on screen and absent from the total. The itinerary service already carried this
         * exact warning about its own flight rollup.
         */
        int flights = body.indexOf("if (flightCosts.getItems() != null)");
        int subtotal = body.indexOf("BigDecimal subtotal = subtotalByCurrency");
        assertTrue(flights >= 0 && flights < subtotal,
            "flightCosts must be folded into subtotalByCurrency before the subtotal is taken");
    }
}
