package com.itineraryledger.kabengosafaris.DataTransfer.Modules;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itineraryledger.kabengosafaris.DataTransfer.ModuleTransfer;
import com.itineraryledger.kabengosafaris.DataTransfer.Scalars;
import com.itineraryledger.kabengosafaris.DataTransfer.TransferContext;
import com.itineraryledger.kabengosafaris.DataTransfer.TransferFile;
import com.itineraryledger.kabengosafaris.Flight.Entity.Airline;
import com.itineraryledger.kabengosafaris.Flight.Entity.Airstrip;
import com.itineraryledger.kabengosafaris.Flight.Entity.FlightFare;
import com.itineraryledger.kabengosafaris.Flight.Entity.FlightRoute;
import com.itineraryledger.kabengosafaris.Flight.Repository.AirlineRepository;
import com.itineraryledger.kabengosafaris.Flight.Repository.AirstripRepository;
import com.itineraryledger.kabengosafaris.Flight.Repository.FlightFareRepository;
import com.itineraryledger.kabengosafaris.Flight.Repository.FlightRouteRepository;

import lombok.RequiredArgsConstructor;

/**
 * Airlines, the strips they fly between, their sectors and their fares.
 *
 * <p>Supporting, and it travels because an itinerary can name a sector. Without it, importing a
 * flying trip into another company would refuse every itinerary that has a flight on it — the same
 * failure the inclusion catalogue had to solve when the promise became rows.
 *
 * <h2>Matched on natural names, never on ids</h2>
 *
 * <p>An obfuscated id means nothing in another installation, and these records have better names
 * anyway: an airstrip has the airline's own three-letter code, an airline has its name, and a
 * sector is the triple (airline, origin, destination). Those are stable across companies in a way
 * a row id never is.
 *
 * <h2>A fare is matched, but never insisted on</h2>
 *
 * <p>Fares are the one part that may legitimately differ between two companies: each negotiates its
 * own net rates with the same airline, and one may hold a contract the other does not. So a fare is
 * created if it is missing and left ALONE if it exists — this company's own negotiated price is not
 * the exporting company's to overwrite.
 *
 * <p>The same reasoning is why an itinerary naming a departure this company has not got is not
 * refused: see ItineraryTransfer, where the flight keeps its sector and loses only the chosen
 * departure, which the day then prices off the cheapest live fare and says so.
 */
@Component
@RequiredArgsConstructor
public class FlightTransfer implements ModuleTransfer {

    private final AirlineRepository airlines;
    private final AirstripRepository airstrips;
    private final FlightRouteRepository routes;
    private final FlightFareRepository fares;
    private final ObjectMapper mapper;

    @Override public String name() { return "flights"; }
    @Override public String label() { return "Airlines, sectors and fares"; }
    /* Before itineraries (70), like the other catalogues an itinerary names. */
    @Override public int order() { return 26; }
    @Override public boolean isSupporting() { return true; }
    @Override public long count() { return routes.count(); }

    @Override
    public String detail() {
        long n = fares.count();
        return n + (n == 1 ? " fare" : " fares");
    }

    @Override
    public JsonNode export(boolean includeImages, List<TransferFile> files) {
        ArrayNode rows = mapper.createArrayNode();

        for (Airline airline : airlines.findAll()) {
            /* `code` is derived from a row id, so it is generated fresh on the receiving side. */
            ObjectNode airlineRow = Scalars.of(mapper, airline, "code");

            ArrayNode routeRows = airlineRow.putArray("routes");
            for (FlightRoute route : routes.findByAirlineIdOrderByIdAsc(airline.getId())) {
                ObjectNode routeRow = Scalars.of(mapper, route, "code");
                /* Both ends by their airline code, which is the only name that means anything there. */
                routeRow.put("origin", route.getOriginAirstrip() == null
                    ? null : route.getOriginAirstrip().getCode());
                routeRow.put("destination", route.getDestinationAirstrip() == null
                    ? null : route.getDestinationAirstrip().getCode());

                ArrayNode fareRows = routeRow.putArray("fares");
                for (FlightFare fare : fares.findByFlightRouteIdOrderByEtdAsc(route.getId())) {
                    fareRows.add(Scalars.of(mapper, fare, "code"));
                }
                routeRows.add(routeRow);
            }
            rows.add(airlineRow);
        }

        /*
         * Airstrips travel as their own list, not only as the ends of sectors: a strip a company
         * has recorded but nothing yet flies to is still worth carrying, and rebuilding the list
         * from route ends would silently drop it.
         */
        ObjectNode envelope = mapper.createObjectNode();
        ArrayNode stripRows = envelope.putArray("airstrips");
        for (Airstrip strip : airstrips.findAll()) {
            stripRows.add(Scalars.of(mapper, strip));
        }
        envelope.set("airlines", rows);

        ArrayNode wrapper = mapper.createArrayNode();
        wrapper.add(envelope);
        return wrapper;
    }

    @Override
    public void importInto(JsonNode data, TransferContext context) {
        var outcome = context.getReport().forModule(name());
        if (data == null || !data.isArray() || data.isEmpty()) return;
        JsonNode envelope = data.get(0);

        /* Airstrips first: a sector names two of them. */
        Map<String, Airstrip> stripsByCode = new HashMap<>();
        for (Airstrip existing : airstrips.findAll()) {
            stripsByCode.put(existing.getCode().toUpperCase(), existing);
        }
        for (JsonNode row : envelope.path("airstrips")) {
            String code = row.path("code").asText(null);
            if (code == null || code.isBlank()) {
                outcome.unresolved("(unnamed airstrip)", "an airstrip with no code cannot be matched");
                continue;
            }
            if (stripsByCode.containsKey(code.toUpperCase())) {
                outcome.skip(code, "this company already has it");
                continue;
            }
            Airstrip strip = new Airstrip();
            Scalars.apply(mapper, row, strip);
            strip.setSlug(uniqueStripSlug(strip.getName(), code));
            strip = airstrips.save(strip);
            stripsByCode.put(code.toUpperCase(), strip);
            outcome.created();
        }

        for (JsonNode airlineRow : envelope.path("airlines")) {
            String name = airlineRow.path("name").asText(null);
            if (name == null || name.isBlank()) {
                outcome.unresolved("(unnamed airline)", "an airline with no name cannot be matched");
                continue;
            }

            Airline airline = airlines.findByNameIgnoreCase(name).orElse(null);
            if (airline == null) {
                airline = new Airline();
                Scalars.apply(mapper, airlineRow, airline, "routes");
                airline.setSlug(uniqueAirlineSlug(name));
                airline = airlines.save(airline);
                outcome.created();
            } else {
                /*
                 * Left alone on purpose, even when overwriting is allowed. The markup on this record
                 * is what THIS company adds to a fare — its own commercial decision — and importing
                 * another company's trips must not quietly reprice every flight it already sells.
                 */
                outcome.skip(name, "this company already has it, and its markup is its own");
            }

            for (JsonNode routeRow : airlineRow.path("routes")) {
                String originCode = routeRow.path("origin").asText(null);
                String destinationCode = routeRow.path("destination").asText(null);
                Airstrip origin = originCode == null ? null : stripsByCode.get(originCode.toUpperCase());
                Airstrip destination = destinationCode == null
                    ? null : stripsByCode.get(destinationCode.toUpperCase());
                if (origin == null || destination == null) {
                    outcome.unresolved(originCode + " to " + destinationCode,
                        "one end of this sector is an airstrip this company has not got");
                    continue;
                }

                FlightRoute route = routes
                    .findByAirlineIdAndOriginAirstripIdAndDestinationAirstripId(
                        airline.getId(), origin.getId(), destination.getId())
                    .orElse(null);
                if (route == null) {
                    route = new FlightRoute();
                    Scalars.apply(mapper, routeRow, route, "fares", "origin", "destination");
                    route.setAirline(airline);
                    route.setOriginAirstrip(origin);
                    route.setDestinationAirstrip(destination);
                    route = routes.saveAndFlush(route);
                    route.setCode(route.generateCode());
                    route = routes.save(route);
                    outcome.created();
                }

                /*
                 * Existing fares are read once per sector rather than queried per row. The
                 * accommodation rate import learned this the expensive way: three round trips per
                 * row turned a 5,394-row import into 274 seconds.
                 */
                Map<String, FlightFare> existingFares = new HashMap<>();
                for (FlightFare existing : fares.findByFlightRouteIdOrderByEtdAsc(route.getId())) {
                    existingFares.put(fareKey(existing), existing);
                }

                for (JsonNode fareRow : routeRow.path("fares")) {
                    FlightFare candidate = new FlightFare();
                    Scalars.apply(mapper, fareRow, candidate);
                    String key = fareKey(candidate);
                    if (existingFares.containsKey(key)) {
                        /* This company's own negotiated price is not the exporter's to overwrite. */
                        outcome.skip(route.getSectorLabel() + " " + key, "this fare is already here");
                        continue;
                    }
                    candidate.setFlightRoute(route);
                    FlightFare saved = fares.saveAndFlush(candidate);
                    saved.setCode(saved.generateCode());
                    fares.save(saved);
                    existingFares.put(key, saved);
                    outcome.created();
                }
            }
        }
    }

    /**
     * What makes a published departure the same departure.
     *
     * <p>The same four columns the table's unique constraint uses, and for the same reason: one
     * sector can publish the same time with a different arrival in each season, so the operating
     * window is part of the identity rather than decoration.
     */
    private String fareKey(FlightFare fare) {
        return String.join("|",
            String.valueOf(fare.getValidFrom()),
            fare.getEtd() == null ? "TBC" : fare.getEtd().toString(),
            fare.getOperatingMonths() == null ? "any" : fare.getOperatingMonths());
    }

    private String uniqueAirlineSlug(String name) {
        String base = slugify(name, "airline");
        String slug = base;
        int n = 2;
        while (airlines.existsBySlug(slug)) slug = base + "-" + n++;
        return slug;
    }

    private String uniqueStripSlug(String name, String code) {
        String base = slugify(name == null ? code : name, "airstrip");
        String slug = base;
        int n = 2;
        while (airstrips.existsBySlug(slug)) slug = base + "-" + n++;
        return slug;
    }

    private String slugify(String value, String fallback) {
        String base = java.text.Normalizer.normalize(value == null ? "" : value,
                java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(java.util.Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
        return base.isBlank() ? fallback : base;
    }
}
