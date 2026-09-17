package com.itineraryledger.kabengosafaris.Flight;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.Flight.CostEstimation.FlightFarePricer;
import com.itineraryledger.kabengosafaris.Flight.Entity.Airline;
import com.itineraryledger.kabengosafaris.Flight.Entity.FlightFare;
import com.itineraryledger.kabengosafaris.Flight.Entity.FlightRoute;
import com.itineraryledger.kabengosafaris.Flight.Enums.MarkupType;

/**
 * The flight sum, pinned.
 *
 * <p>Three cost-estimation stacks will call {@link FlightFarePricer}, so every one of them inherits
 * whatever this class allows. The cases here are the ones that go quietly wrong: a tax that gets
 * marked up, a child charged an adult's percentage, a markup read from the wrong level of the
 * cascade, and a gross fare mistaken for a cost.
 */
class AFlightIsPricedAtCostPlusMarkupTest {

    /** Air Excel, Arusha to Zanzibar: gross 316, tax 25, and a net fare the office supplies. */
    private FlightFare arushaToZanzibar(String net) {
        return FlightFare.builder()
            .flightRoute(FlightRoute.builder().isOnRequest(false).build())
            .netFare(net == null ? null : new BigDecimal(net))
            .grossFare(new BigDecimal("316.00"))
            .taxesAndFees(new BigDecimal("25.00"))
            .childPercent(new BigDecimal("70.00"))
            .currency("USD")
            .validFrom(LocalDate.of(2025, 6, 1))
            .validTo(LocalDate.of(2027, 5, 31))
            .build();
    }

    @Test
    @DisplayName("a percentage markup applies to the fare and never to the tax")
    void taxIsNeverMarkedUp() {
        var priced = FlightFarePricer.price(arushaToZanzibar("300.00"),
            Airline.builder().name("Air Excel").markupType(MarkupType.PERCENT)
                .markupValue(new BigDecimal("13")).build(),
            2, 0, null, null, LocalDate.of(2026, 9, 22));

        assertEquals(new BigDecimal("39.00"), priced.markupPerAdult(), "13% of 300");
        /*
         * 300 + 39 + 25. If the tax were inside the markup base this would be 392.25, and the
         * airline's own invoice would disagree with ours by 3.25 a head on a number they also print.
         */
        assertEquals(new BigDecimal("364.00"), priced.sellingPerAdult());
        assertEquals(new BigDecimal("728.00"), priced.totalSelling(), "two adults");
        assertEquals(new BigDecimal("50.00"), priced.totalTax());
        assertTrue(priced.warnings().isEmpty(), () -> "unexpected: " + priced.warnings());
    }

    @Test
    @DisplayName("a fixed markup is per ticket, and a child's ticket is a ticket")
    void fixedMarkupIsPerPerson() {
        var priced = FlightFarePricer.price(arushaToZanzibar("300.00"),
            Airline.builder().name("Air Excel").markupType(MarkupType.AMOUNT)
                .markupValue(new BigDecimal("50")).build(),
            2, 1, null, null, null);

        assertEquals(new BigDecimal("50.00"), priced.markupPerAdult());
        assertEquals(new BigDecimal("50.00"), priced.markupPerChild(),
            "a fixed markup is the work of issuing a ticket, which a child's ticket also takes");
        assertEquals(new BigDecimal("150.00"), priced.totalMarkup(), "three tickets");
    }

    @Test
    @DisplayName("a child pays 70% of the fare, and a percentage markup follows the child's fare")
    void childFareAndItsMarkup() {
        var priced = FlightFarePricer.price(arushaToZanzibar("300.00"),
            Airline.builder().markupType(MarkupType.PERCENT).markupValue(new BigDecimal("13")).build(),
            0, 1, null, null, null);

        assertEquals(new BigDecimal("210.00"), priced.netPerChild(), "70% of 300");
        assertEquals(new BigDecimal("27.30"), priced.markupPerChild(),
            "13% of the CHILD's 210, not of the adult's 300");
        assertEquals(new BigDecimal("262.30"), priced.sellingPerChild(), "210 + 27.30 + 25 tax");
    }

    @Test
    @DisplayName("the markup cascade reads the flight, then the fare, then the airline")
    void theCascade() {
        Airline airline = Airline.builder().name("Air Excel")
            .markupType(MarkupType.PERCENT).markupValue(new BigDecimal("13")).build();
        FlightFare fare = arushaToZanzibar("300.00");

        var fromAirline = FlightFarePricer.resolveMarkup(null, null, fare, airline);
        assertEquals(new BigDecimal("13"), fromAirline.value());
        assertEquals("Air Excel", fromAirline.source());

        fare.setMarkupType(MarkupType.PERCENT);
        fare.setMarkupValue(new BigDecimal("20"));
        assertEquals(new BigDecimal("20"), FlightFarePricer.resolveMarkup(null, null, fare, airline).value(),
            "the fare overrides the airline");

        var fromLine = FlightFarePricer.resolveMarkup(MarkupType.AMOUNT, new BigDecimal("50"), fare, airline);
        assertEquals(MarkupType.AMOUNT, fromLine.type(), "the flight overrides both");
        assertEquals("this flight", fromLine.source());

        assertTrue(FlightFarePricer.resolveMarkup(null, null, arushaToZanzibar("300.00"), null).isNone(),
            "no markup anywhere is a legitimate answer: the client is passed the airline's price");
    }

    @Test
    @DisplayName("a gross fare is not a cost, so a fare without a net one prices nothing")
    void grossIsNotCost() {
        var priced = FlightFarePricer.price(arushaToZanzibar(null),
            Airline.builder().markupType(MarkupType.PERCENT).markupValue(new BigDecimal("13")).build(),
            2, 0, null, null, null);

        assertEquals(0, priced.totalSelling().signum(),
            "the published 316 is what a client pays booking direct, not what we pay");
        assertFalse(priced.isPriceable());
        assertTrue(priced.warnings().stream().anyMatch(w -> w.contains("No net fare")),
            () -> "must say so loudly: " + priced.warnings());
    }

    @Test
    @DisplayName("flying out of the operating window warns but does not change the price")
    void outOfSeasonIsAWarningNotAPrice() {
        FlightFare fare = arushaToZanzibar("300.00");
        fare.setOperatingMonths("6,7,8,9,10,11");

        var inSeason = FlightFarePricer.price(fare, null, 1, 0, null, null, LocalDate.of(2026, 9, 22));
        var outOfSeason = FlightFarePricer.price(fare, null, 1, 0, null, null, LocalDate.of(2026, 4, 22));

        assertEquals(inSeason.sellingPerAdult(), outOfSeason.sellingPerAdult(),
            "the operating window governs availability, never price");
        assertTrue(inSeason.warnings().isEmpty());
        assertTrue(outOfSeason.warnings().stream().anyMatch(w -> w.contains("does not normally fly")));
    }

    @Test
    @DisplayName("the accommodation rack rule must not reach a flight")
    void noRackMultiplier() {
        var priced = FlightFarePricer.price(arushaToZanzibar("300.00"), null, 1, 0, null, null, null);
        /*
         * 300 × 1.3 would be 390, before tax. Air Excel publishes this seat at 316; selling it at
         * 415 is not a price anybody pays, and it is exactly what happens if somebody reuses
         * rackFromSto here because every other rate in the system goes through it.
         */
        assertEquals(new BigDecimal("325.00"), priced.sellingPerAdult(), "300 + no markup + 25 tax");
        assertNotEquals(0, priced.sellingPerAdult().compareTo(new BigDecimal("415.00")));
    }
}
