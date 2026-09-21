package com.itineraryledger.kabengosafaris.Flight;

import com.itineraryledger.kabengosafaris.Flight.Entity.FlightFare;
import com.itineraryledger.kabengosafaris.Flight.Entity.FlightRoute;

/**
 * How a flight is named to a human.
 *
 * <p>Three entities carry a flight — an itinerary day, a quote day, a safari day — and all three
 * want the same sentence: "Air Excel ARS to ZNZ, 14:00". Written three times it drifts, and it
 * drifts somewhere visible, because this string is what appears on a cost line, in a dropdown and
 * on a client's document. One of the three ending up without its departure time is the kind of
 * difference nobody reports and everybody notices.
 */
public final class FlightLabels {

    private FlightLabels() {}

    /** The airline, the sector, and the departure if one has been picked. */
    public static String displayName(FlightRoute route, FlightFare fare) {
        if (route == null) return "Flight";
        StringBuilder out = new StringBuilder();
        if (route.getAirline() != null) out.append(route.getAirline().getName()).append(' ');
        out.append(route.getSectorLabel());
        if (fare != null && fare.getEtd() != null) out.append(", ").append(fare.getEtd());
        return out.toString();
    }
}
