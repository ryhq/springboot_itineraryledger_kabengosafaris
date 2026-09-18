package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Calculators;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.Flight.CostEstimation.FlightFarePricer;
import com.itineraryledger.kabengosafaris.Flight.Entity.FlightFare;
import com.itineraryledger.kabengosafaris.Flight.Enums.MarkupType;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CostLineItemDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CostItemType;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.ExclusionReason;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core.RateIssueLoggerService;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * What the flights on a day cost.
 *
 * <p>Thin on purpose. Every figure comes from {@link FlightFarePricer}, which knows nothing about
 * itineraries and is shared with the safari side: the 70% child fare, the tax that must stay out of
 * the markup base, and the markup cascade all live there and are tested there. This class does the
 * day-tree plumbing and nothing else, so the sum cannot drift between the two callers.
 *
 * <p>Alternatives go to {@code excludedLineItems} exactly as an alternative lodge does — costed and
 * shown, never added to the total.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlightCostCalculator {

    private final RateIssueLoggerService rateIssueLogger;

    private static final String DEFAULT_CURRENCY = "USD";

    /** The flights actually being paid for on this day. */
    public List<CostLineItemDTO> calculateForDay(
            FullItineraryDTO.DayDTO day,
            LocalDate dayDate,
            List<FullItineraryDTO.PaxDTO> paxList
    ) {
        List<CostLineItemDTO> items = new ArrayList<>();
        if (day.getFlights() == null || day.getFlights().isEmpty()) return items;

        for (FullItineraryDTO.DayFlightDTO flight : day.getFlights()) {
            if (Boolean.TRUE.equals(flight.getIsAlternative())) continue;
            if (Boolean.FALSE.equals(flight.getIsIncludedInPrice())) continue;
            CostLineItemDTO item = price(flight, day, dayDate, paxList, null);
            if (item != null) items.add(item);
        }
        return items;
    }

    /**
     * The flights costed but deliberately kept out of the total.
     *
     * <p>A separate method writing into a separate field, like every other calculator here: the
     * aggregators sum {@code lineItems} and only {@code lineItems}, so an alternative can never
     * reach a total by accident.
     */
    public List<CostLineItemDTO> calculateExcludedForDay(
            FullItineraryDTO.DayDTO day,
            LocalDate dayDate,
            List<FullItineraryDTO.PaxDTO> paxList
    ) {
        List<CostLineItemDTO> items = new ArrayList<>();
        if (day.getFlights() == null || day.getFlights().isEmpty()) return items;

        for (FullItineraryDTO.DayFlightDTO flight : day.getFlights()) {
            ExclusionReason reason = null;
            if (Boolean.TRUE.equals(flight.getIsAlternative())) {
                reason = ExclusionReason.ALTERNATIVE_ACCOMMODATION;
            } else if (Boolean.FALSE.equals(flight.getIsIncludedInPrice())) {
                reason = ExclusionReason.NOT_INCLUDED_IN_PRICE;
            }
            if (reason == null) continue;
            CostLineItemDTO item = price(flight, day, dayDate, paxList, reason);
            if (item != null) items.add(item);
        }
        return items;
    }

    // ---- one line -------------------------------------------------------------------------------

    private CostLineItemDTO price(FullItineraryDTO.DayFlightDTO flight,
                                  FullItineraryDTO.DayDTO day,
                                  LocalDate dayDate,
                                  List<FullItineraryDTO.PaxDTO> paxList,
                                  ExclusionReason exclusionReason) {

        int adults = 0;
        int children = 0;
        /*
         * A flight is per SEAT, so the split between adults and children matters — a child flies at
         * 70% of the fare. Everything else on a day is per room or per vehicle and can work from a
         * head count, which is why no other calculator needs this.
         */
        if (paxList != null) {
            for (FullItineraryDTO.PaxDTO pax : paxList) {
                int count = pax.getCount() == null ? 0 : pax.getCount();
                if (isChild(pax)) children += count;
                else adults += count;
            }
        }
        /* An explicit passenger count on the line overrides the trip's pax — a positioning leg some
         * of the party takes, or one seat bought for a guide. Treated as adults: it is a seat count
         * and nobody typing it means "and some of them are children". */
        if (flight.getPassengerCount() != null && flight.getPassengerCount() > 0) {
            adults = flight.getPassengerCount();
            children = 0;
        }
        if (adults + children == 0) return null;

        /*
         * The pricer takes entities, so the DTO's flattened figures are rebuilt into one. Cheaper
         * than loading the fare again, and it keeps the arithmetic in the one place that is tested.
         */
        FlightFare fare = FlightFare.builder()
            .netFare(flight.getNetFare())
            .taxesAndFees(flight.getTaxesAndFees() == null ? BigDecimal.ZERO : flight.getTaxesAndFees())
            .childPercent(flight.getChildPercent())
            .currency(flight.getCurrency() == null ? DEFAULT_CURRENCY : flight.getCurrency())
            .operatingMonths(flight.getOperatingMonths())
            .minimumSeats(flight.getMinimumSeats())
            .validFrom(LocalDate.MIN)
            .validTo(LocalDate.MAX)
            .build();

        MarkupType markupType = null;
        if (flight.getMarkupType() != null && !flight.getMarkupType().isBlank()) {
            try {
                markupType = MarkupType.valueOf(flight.getMarkupType());
            } catch (IllegalArgumentException ignored) {
                /* A stored value nothing recognises means no markup, not a crash mid-estimate. */
            }
        }

        var priced = FlightFarePricer.price(fare, null, adults, children,
            markupType, flight.getMarkupValue(), dayDate);

        String name = (flight.getAirlineName() == null ? "Flight" : flight.getAirlineName())
            + " " + (flight.getSectorLabel() == null ? "" : flight.getSectorLabel())
            + (flight.getEtd() == null
                ? (flight.getDepartureLabel() == null ? "" : ", " + flight.getDepartureLabel())
                : ", " + flight.getEtd());

        /*
         * Every warning the pricer raised becomes a rate issue, so "no net fare", "on request only"
         * and "out of season" all surface on the Cost tab beside the missing accommodation rates
         * rather than in a log nobody reads.
         */
        for (String warning : priced.warnings()) {
            rateIssueLogger.logStated(CostItemType.FLIGHT, name.trim(), flight.getFlightFareId(),
                day.getDayNumber(), warning);
        }

        return CostLineItemDTO.builder()
            .dayNumber(day.getDayNumber())
            .itemType(CostItemType.FLIGHT)
            .itemName(name.trim())
            .itemId(flight.getFlightFareId())
            .entryId(flight.getId())
            .chargingBasis("Per Person")
            .quantity(adults + children)
            .stoUnitPrice(priced.sellingPerAdult())
            .rackUnitPrice(priced.sellingPerAdult())
            /*
             * STO and rack carry the same figure. A flight has no rack multiplier — that is an
             * accommodation rule, and 316 x 1.3 is 411 for a seat the airline publishes at 316 — so
             * the markup is already inside both and there is no second number to show.
             */
            .stoTotalPrice(priced.totalSelling())
            .rackTotalPrice(priced.totalSelling())
            .currency(priced.currency())
            .notes(flightNote(priced, children, flight.getMarkupSource()))
            .exclusionReason(exclusionReason)
            .build();
    }

    /** True for a pax band the office has named as a child or an infant. */
    private boolean isChild(FullItineraryDTO.PaxDTO pax) {
        String age = pax.getAgeCategoryName();
        if (age == null) return false;
        String lower = age.toLowerCase();
        return lower.contains("child") || lower.contains("infant") || lower.contains("youth");
    }

    /**
     * @param resolvedSource where the markup came from, as worked out once when the DTO was built.
     *                       The pricer is handed that already-resolved markup as this line's own, so
     *                       it would report "this flight" for every flight — naming the airline's 13%
     *                       as a per-trip override, which is exactly the disagreement between the
     *                       cost sheet and the panel that resolving the cascade in one place was
     *                       meant to prevent.
     */
    private String flightNote(FlightFarePricer.PricedFlight priced, int children, String resolvedSource) {
        StringBuilder note = new StringBuilder();
        note.append("Fare ").append(priced.currency()).append(' ').append(priced.netPerAdult());
        if (priced.markup().isNone()) note.append(", no markup");
        else note.append(", markup from ")
            .append(resolvedSource == null || resolvedSource.isBlank()
                ? priced.markup().source() : resolvedSource);
        note.append(", tax ").append(priced.taxPerPerson()).append(" per person (never marked up)");
        if (children > 0) {
            note.append(". ").append(children).append(" child fare(s) at ")
                .append(priced.currency()).append(' ').append(priced.netPerChild());
        }
        return note.toString();
    }
}
