package com.itineraryledger.kabengosafaris.Quote;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The same quote, totalled twice, has to give the same answer.
 *
 * <p>It did not. One quote held 12,439.78 and 12,439.74 depending only on which save last touched
 * it, because the two ways of writing its lines rounded differently: the per-line writer rounded
 * the UNIT to cents and then multiplied by the quantity, pushing the error through the quantity,
 * while the condensed writer summed exact totals and rounded once. Four cents is small; a total
 * that changes when nobody edited anything is not.
 *
 * <p>These tests pin the arithmetic rather than the services, because the arithmetic is the bug and
 * it is where a future change would reintroduce it.
 */
class OneQuoteHasOneTotalTest {

    /** What the old per-line writer did: round the unit, then multiply. */
    private BigDecimal roundUnitThenMultiply(BigDecimal unit, int qty, BigDecimal multiplier) {
        return unit.multiply(multiplier).setScale(2, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(qty));
    }

    /** What both writers do now: inflate the exact total, round once. */
    private BigDecimal roundTheTotalOnce(BigDecimal exactTotal, BigDecimal multiplier) {
        return exactTotal.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    /** The markup, built the way the service builds it: scale 6. */
    private BigDecimal multiplierFor(String percent) {
        return BigDecimal.ONE.add(
            new BigDecimal(percent).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("rounding the unit and multiplying disagrees with rounding the total")
    void thisIsTheDefect() {
        // a park fee whose unit price is a blended average across pax categories
        BigDecimal exactTotal = new BigDecimal("3412.24");
        int qty = 15;
        BigDecimal avgUnit = exactTotal.divide(BigDecimal.valueOf(qty), 2, RoundingMode.HALF_UP);
        BigDecimal markup = multiplierFor("30");

        BigDecimal oldWay = roundUnitThenMultiply(avgUnit, qty, markup);
        BigDecimal newWay = roundTheTotalOnce(exactTotal, markup);

        assertTrue(oldWay.compareTo(newWay) != 0,
            "if these ever agree the example has stopped exercising the bug");
        assertEquals(0, new BigDecimal("4435.91").compareTo(newWay),
            "the exact total inflated once: " + newWay);
    }

    @Test
    @DisplayName("the per-line and condensed routes now agree on the same data")
    void theTwoWritersAgree() {
        // three lines, as they would arrive from the estimator
        List<BigDecimal> lineTotals = List.of(
            new BigDecimal("7120.00"), new BigDecimal("3412.24"), new BigDecimal("2645.50"));
        BigDecimal markup = multiplierFor("30");

        // per-line: each line inflated and rounded once, then summed
        BigDecimal perLine = lineTotals.stream()
            .map(t -> roundTheTotalOnce(t, markup))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // condensed: summed first, then inflated and rounded once
        BigDecimal condensed = roundTheTotalOnce(
            lineTotals.stream().reduce(BigDecimal.ZERO, BigDecimal::add), markup);

        assertTrue(perLine.subtract(condensed).abs().compareTo(new BigDecimal("0.02")) <= 0,
            "rounding once per line and once overall may differ by a cent or two, but never by "
            + "more, and never by the quantity-amplified amount the old unit-first path produced: "
            + perLine + " vs " + condensed);
    }

    @Test
    @DisplayName("a subtotal normalised to cents re-sums to itself")
    void resummingIsStable() {
        // an unnormalised price, as the per-line writer used to leave behind
        BigDecimal messy = new BigDecimal("1481.333333");

        BigDecimal summedRaw = BigDecimal.ZERO.add(messy).add(messy).add(messy);
        BigDecimal summedNormalised = BigDecimal.ZERO
            .add(messy.setScale(2, RoundingMode.HALF_UP))
            .add(messy.setScale(2, RoundingMode.HALF_UP))
            .add(messy.setScale(2, RoundingMode.HALF_UP));

        assertTrue(summedRaw.setScale(2, RoundingMode.HALF_UP)
                .compareTo(summedNormalised) != 0,
            "the raw sum and the normalised sum differ, which is exactly how one stored quote "
            + "produced two totals depending on which save ran last");
        assertEquals(0, new BigDecimal("4443.99").compareTo(summedNormalised));
    }

    @Test
    @DisplayName("a unit price derived from the total is only for display")
    void theUnitIsDerived() {
        BigDecimal total = new BigDecimal("4435.91");
        int qty = 15;

        BigDecimal unit = total.divide(BigDecimal.valueOf(qty), 2, RoundingMode.HALF_UP);

        assertEquals(0, new BigDecimal("295.73").compareTo(unit));
        assertTrue(unit.multiply(BigDecimal.valueOf(qty)).compareTo(total) != 0,
            "a derived unit price must never be multiplied back up to recover the total. That "
            + "reconstruction is what made park fees wrong by a flat amount across every line.");
    }
}
