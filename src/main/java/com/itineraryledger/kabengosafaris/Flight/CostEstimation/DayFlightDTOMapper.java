package com.itineraryledger.kabengosafaris.Flight.CostEstimation;

import java.util.function.LongFunction;

import com.itineraryledger.kabengosafaris.Flight.Entity.FlightFare;
import com.itineraryledger.kabengosafaris.Flight.Entity.FlightRoute;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO;

/**
 * One flight row, as the cost engine wants to read it — written once for the three day trees.
 *
 * <p>An itinerary day, a quote day and a safari day each have their own flight entity. They are
 * three unrelated Java types holding the same eight columns, because each document keeps its own
 * copy of the trip: a sent quote must not change when the itinerary behind it is edited. That is
 * right, and it means the mapping to {@link FullItineraryDTO.DayFlightDTO} would otherwise be
 * written out three times.
 *
 * <p>Which is worth avoiding for one specific reason, not on principle. Twenty-odd of these fields
 * are inert — a label, a code, a note — and a copy that omits one is obvious. Three are not:
 * <b>netFare</b>, <b>taxesAndFees</b> and the resolved <b>markup</b> are what the calculator does
 * arithmetic on. A converter that resolves the cascade slightly differently from its neighbour
 * produces a quote whose cost sheet disagrees with the itinerary it came from, by a few dollars,
 * with no error anywhere. {@link FlightFarePricer} exists so the sum is written once; this exists
 * so its inputs are gathered once.
 *
 * <p>The line's own columns are passed in rather than read off an interface. Giving three entities
 * a shared interface purely to satisfy this mapper would put a coupling in the domain to save a
 * line here.
 */
public final class DayFlightDTOMapper {

    private DayFlightDTOMapper() {}

    /** The per-line columns, which are all the three entities do not share a type for. */
    public record LineValues(
        Long id,
        Integer passengerCount,
        Integer sortOrder,
        String notes,
        Boolean isAlternative,
        Boolean isIncludedInPrice
    ) {}

    /**
     * @param fare   the chosen departure, or the fallback the caller resolved for a line that has
     *               none. An itinerary is a product with no dates, so the planner picks a sector
     *               and the office picks the departure later; until then the cheapest live fare is
     *               the honest figure. Null is allowed and prices nothing, loudly.
     * @param markup already resolved down the line → fare → airline cascade, by
     *               {@link FlightFarePricer#resolveMarkup}. Resolved by the caller rather than here
     *               because the caller is the only one that knows the line's own override.
     */
    public static FullItineraryDTO.DayFlightDTO map(
            LineValues line,
            FlightRoute route,
            FlightFare fare,
            FlightFarePricer.ResolvedMarkup markup,
            LongFunction<String> encodeId) {

        var airline = route == null ? null : route.getAirline();

        return FullItineraryDTO.DayFlightDTO.builder()
            .id(line.id() == null ? null : encodeId.apply(line.id()))
            .flightRouteId(route == null ? null : encodeId.apply(route.getId()))
            .flightFareId(fare == null ? null : encodeId.apply(fare.getId()))
            .airlineName(airline == null ? null : airline.getName())
            .sectorLabel(route == null ? null : route.getSectorLabel())
            .originCode(route == null || route.getOriginAirstrip() == null
                ? null : route.getOriginAirstrip().getCode())
            .destinationCode(route == null || route.getDestinationAirstrip() == null
                ? null : route.getDestinationAirstrip().getCode())
            .etd(fare == null || fare.getEtd() == null ? null : fare.getEtd().toString())
            .eta(fare == null || fare.getEta() == null ? null : fare.getEta().toString())
            .departureLabel(fare == null ? null : fare.getDepartureLabel())
            .netFare(fare == null ? null : fare.getNetFare())
            .taxesAndFees(fare == null ? null : fare.getTaxesAndFees())
            .childPercent(fare == null ? null : fare.getChildPercent())
            .currency(fare == null ? null : fare.getCurrency())
            .markupType(markup == null || markup.isNone() ? null : markup.type().name())
            .markupValue(markup == null ? null : markup.value())
            .markupSource(markup == null ? null : markup.source())
            .passengerCount(line.passengerCount())
            .sortOrder(line.sortOrder())
            .notes(line.notes())
            .isAlternative(line.isAlternative())
            .isIncludedInPrice(line.isIncludedInPrice())
            .isOnRequest(route == null ? null : route.getIsOnRequest())
            /*
             * The fare's minimum overrides the sector's: a sector may fly with two, while one
             * particular departure on it is a larger aircraft that will not go with fewer than four.
             */
            .minimumSeats(fare != null && fare.getMinimumSeats() != null
                ? fare.getMinimumSeats() : (route == null ? null : route.getMinimumSeats()))
            .operatingMonths(fare == null ? null : fare.getOperatingMonths())
            .build();
    }
}
