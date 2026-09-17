package com.itineraryledger.kabengosafaris.Flight.CostEstimation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.itineraryledger.kabengosafaris.Flight.Entity.Airline;
import com.itineraryledger.kabengosafaris.Flight.Entity.FlightFare;
import com.itineraryledger.kabengosafaris.Flight.Enums.MarkupType;

/**
 * What a flight costs, and what we sell it for. The whole of the arithmetic, in one place.
 *
 * <p>This class knows nothing about itineraries, quotes or safaris — no {@code FullItineraryDTO}, no
 * {@code FullSafariDTO}, no Spring. That is the point. There are three cost-estimation stacks in
 * this codebase and the same sum would otherwise be written three times: the 70% child fare, the
 * markup that must not touch the tax, the cascade from line to fare to airline. Those are the parts
 * that go quietly wrong, so they live here once and the day-tree plumbing around them is the only
 * thing that gets duplicated.
 *
 * <h2>The sum</h2>
 *
 * <pre>
 *   adult   = netFare                        + markup(netFare)        + taxes
 *   child   = netFare × childPercent / 100   + markup(child net)      + taxes
 * </pre>
 *
 * <h2>Three rules that are easy to get wrong</h2>
 *
 * <ol>
 *   <li><strong>Taxes are never marked up.</strong> They are collected on the airline's behalf and
 *       the invoice has to reconcile against theirs. Marking them up quietly inflates a number the
 *       airline will also print, on a document the client may hold both halves of.</li>
 *   <li><strong>A fixed markup is per person, children included.</strong> It is a handling fee for
 *       issuing a ticket, and a child's ticket takes the same work as an adult's. A percentage, by
 *       contrast, follows the child's own lower fare.</li>
 *   <li><strong>The house 1.3 rack multiplier has no business here.</strong> It is an accommodation
 *       rule. A 316 fare at 1.3 is 411 for a seat the airline publishes at 316.</li>
 * </ol>
 */
public final class FlightFarePricer {

    private FlightFarePricer() {}

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int SCALE = 2;

    /** The markup actually in force for one flight, and where it came from. */
    public record ResolvedMarkup(MarkupType type, BigDecimal value, String source) {
        public boolean isNone() { return type == null || value == null; }
    }

    /**
     * A priced flight.
     *
     * @param warnings things a human should see before this reaches a client — no net fare, flying
     *                 out of season, a route that is on request only. Never a reason to refuse: the
     *                 house convention everywhere else is to warn loudly and let the office decide.
     */
    public record PricedFlight(
        BigDecimal netPerAdult,
        BigDecimal netPerChild,
        BigDecimal markupPerAdult,
        BigDecimal markupPerChild,
        BigDecimal taxPerPerson,
        BigDecimal sellingPerAdult,
        BigDecimal sellingPerChild,
        int adults,
        int children,
        BigDecimal totalNet,
        BigDecimal totalMarkup,
        BigDecimal totalTax,
        BigDecimal totalSelling,
        String currency,
        ResolvedMarkup markup,
        List<String> warnings
    ) {
        public boolean isPriceable() { return totalSelling != null && totalSelling.signum() > 0; }
    }

    /**
     * The cascade: a flight line's own markup beats the fare's, which beats the airline's.
     *
     * <p>Null all the way up means no markup, which is a legitimate answer — a trip where the client
     * is being passed the airline's price at cost.
     */
    public static ResolvedMarkup resolveMarkup(MarkupType lineType, BigDecimal lineValue,
                                               FlightFare fare, Airline airline) {
        if (lineType != null && lineValue != null) {
            return new ResolvedMarkup(lineType, lineValue, "this flight");
        }
        if (fare != null && fare.hasMarkup()) {
            return new ResolvedMarkup(fare.getMarkupType(), fare.getMarkupValue(), "the fare");
        }
        if (airline != null && airline.hasMarkup()) {
            return new ResolvedMarkup(airline.getMarkupType(), airline.getMarkupValue(), airline.getName());
        }
        return new ResolvedMarkup(null, null, "none");
    }

    /** Apply a resolved markup to one person's fare. Returns zero when there is no markup. */
    public static BigDecimal markupOn(BigDecimal fareForOnePerson, ResolvedMarkup markup) {
        if (markup == null || markup.isNone() || fareForOnePerson == null) return BigDecimal.ZERO;
        if (markup.type() == MarkupType.PERCENT) {
            return fareForOnePerson.multiply(markup.value())
                .divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
        }
        /* AMOUNT: a flat fee per ticket, the same for a child as for an adult. */
        return markup.value().setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Price one flight for a party.
     *
     * @param travelDate the day this is flown, used only to check the fare's windows. May be null
     *                   on an itinerary, which is a product without dates.
     */
    public static PricedFlight price(FlightFare fare, Airline airline, int adults, int children,
                                     MarkupType lineMarkupType, BigDecimal lineMarkupValue,
                                     LocalDate travelDate) {
        List<String> warnings = new ArrayList<>();
        ResolvedMarkup markup = resolveMarkup(lineMarkupType, lineMarkupValue, fare, airline);

        if (fare == null) {
            warnings.add("No fare chosen for this flight, so it is not priced.");
            return empty(warnings, markup, "USD", adults, children);
        }

        String currency = fare.getCurrency() == null ? "USD" : fare.getCurrency();

        /*
         * The net fare is what a quote is built from. The Air Excel sheet publishes GROSS only, so a
         * freshly imported fare has none until the office loads the airline's net rates — and a trip
         * priced off a gross fare would quietly undercut its own cost. Warn and price nothing.
         */
        if (!fare.hasNetFare()) {
            warnings.add("No net fare loaded for this flight"
                + (fare.getGrossFare() != null
                    ? " — the airline's published gross is " + currency + " " + fare.getGrossFare()
                      + ", which is not what we pay and is not quotable."
                    : "."));
            return empty(warnings, markup, currency, adults, children);
        }

        if (travelDate != null && !fare.coversDate(travelDate)) {
            warnings.add("This fare is contracted " + fare.getValidFrom() + " to " + fare.getValidTo()
                + ", which does not cover " + travelDate + ".");
        }
        if (travelDate != null && !fare.fliesIn(travelDate)) {
            warnings.add("This route does not normally fly in " + travelDate.getMonth()
                + " — confirm with the airline. The fare itself does not change.");
        }
        if (fare.getFlightRoute() != null && Boolean.TRUE.equals(fare.getFlightRoute().getIsOnRequest())) {
            warnings.add("This sector is flown on request only, so a seat is not guaranteed.");
        }
        Integer minimumSeats = fare.getMinimumSeats() != null
            ? fare.getMinimumSeats()
            : (fare.getFlightRoute() == null ? null : fare.getFlightRoute().getMinimumSeats());
        if (minimumSeats != null && adults + children < minimumSeats) {
            warnings.add("This sector needs a minimum of " + minimumSeats + " seats and the party is "
                + (adults + children) + ".");
        }

        BigDecimal netAdult = fare.getNetFare().setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal childPercent = fare.getChildPercent() == null
            ? new BigDecimal("70.00") : fare.getChildPercent();
        BigDecimal netChild = netAdult.multiply(childPercent)
            .divide(HUNDRED, SCALE, RoundingMode.HALF_UP);

        BigDecimal markupAdult = markupOn(netAdult, markup);
        BigDecimal markupChild = markupOn(netChild, markup);

        BigDecimal tax = fare.getTaxesAndFees() == null
            ? BigDecimal.ZERO : fare.getTaxesAndFees().setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal sellAdult = netAdult.add(markupAdult).add(tax);
        BigDecimal sellChild = netChild.add(markupChild).add(tax);

        BigDecimal a = BigDecimal.valueOf(adults);
        BigDecimal c = BigDecimal.valueOf(children);

        return new PricedFlight(
            netAdult, netChild, markupAdult, markupChild, tax, sellAdult, sellChild,
            adults, children,
            netAdult.multiply(a).add(netChild.multiply(c)).setScale(SCALE, RoundingMode.HALF_UP),
            markupAdult.multiply(a).add(markupChild.multiply(c)).setScale(SCALE, RoundingMode.HALF_UP),
            tax.multiply(a.add(c)).setScale(SCALE, RoundingMode.HALF_UP),
            sellAdult.multiply(a).add(sellChild.multiply(c)).setScale(SCALE, RoundingMode.HALF_UP),
            currency, markup, warnings);
    }

    private static PricedFlight empty(List<String> warnings, ResolvedMarkup markup, String currency,
                                      int adults, int children) {
        BigDecimal z = BigDecimal.ZERO.setScale(SCALE);
        return new PricedFlight(z, z, z, z, z, z, z, adults, children, z, z, z, z,
            currency, markup, warnings);
    }
}
