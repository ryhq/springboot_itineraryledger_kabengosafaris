package com.itineraryledger.kabengosafaris.Flight.Services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Flight.DTOs.CreateFlightRouteDTO;
import com.itineraryledger.kabengosafaris.Flight.DTOs.FlightRouteDTO;
import com.itineraryledger.kabengosafaris.Flight.DTOs.UpdateFlightRouteDTO;
import com.itineraryledger.kabengosafaris.Flight.Entity.Airline;
import com.itineraryledger.kabengosafaris.Flight.Entity.Airstrip;
import com.itineraryledger.kabengosafaris.Flight.Entity.FlightRoute;
import com.itineraryledger.kabengosafaris.Flight.Repository.AirlineRepository;
import com.itineraryledger.kabengosafaris.Flight.Repository.AirstripRepository;
import com.itineraryledger.kabengosafaris.Flight.Repository.FlightFareRepository;
import com.itineraryledger.kabengosafaris.Flight.Repository.FlightRouteRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayFlight.Repository.ItineraryDayFlightRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sectors — one airline, one origin, one destination.
 *
 * <p>The three together are the route's identity and the table enforces it, so there is no way to
 * edit them here: repointing a sector would silently move every fare and every trip already on it.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class FlightRouteService {

    private final FlightRouteRepository repository;
    private final AirlineRepository airlines;
    private final AirstripRepository airstrips;
    private final FlightFareRepository fares;
    private final ItineraryDayFlightRepository dayFlights;
    private final IdObfuscator idObfuscator;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    /*
     * What the table may sort by, in the DTO's own names, mapped to the entity path underneath.
     *
     * It used to be id and createdAt alone, so every other column's sort arrow quietly did
     * nothing: the request asked for "airlineName", the service did not recognise it, and the
     * rows came back in id order looking sorted. A control that appears to work is worse than
     * one that is absent, which is why the two name columns are marked unsortable on the panel
     * side rather than pretending.
     *
     * The dotted paths are property paths, not SQL — Spring Data joins them for us.
     */
    /** Words that join two place names on screen and name nothing themselves. */
    private static final java.util.Set<String> JOINERS =
        java.util.Set.of("to", "and", "-", "->", "→", "via");

    private static final java.util.Map<String, String> SORT_PATHS = java.util.Map.of(
        "id", "id",
        "createdAt", "createdAt",
        "sectorLabel", "originAirstrip.code",
        "airlineName", "airline.name",
        "originName", "originAirstrip.name",
        "destinationName", "destinationAirstrip.name",
        "isOnRequest", "isOnRequest",
        "minimumSeats", "minimumSeats"
    );
    private static final List<String> SORTABLE = List.copyOf(SORT_PATHS.keySet());

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getAll(String keyword, String airlineId, String originAirstripId,
                                                 String destinationAirstripId, Boolean isOnRequest, Boolean isActive,
                                                 Integer page, Integer size, String sortBy, String sortDirection) {
        Specification<FlightRoute> spec = buildSpec(keyword, airlineId, originAirstripId,
            destinationAirstripId, isOnRequest, isActive);

        /*
         * The null check is NOT redundant. SORTABLE is a List.of, which forbids nulls and
         * throws NullPointerException from contains(null) rather than answering false — and
         * sortBy is null on every request that does not ask for a sort, which is most of them.
         * Without it the whole listing answers 500.
         */
        String sortField = SORT_PATHS.getOrDefault(sortBy == null ? "" : sortBy, "id");
        Sort sort = Sort.by("desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC, sortField);
        Pageable pageable = PageRequest.of(page == null ? 0 : page, clamp(size), sort);

        Page<FlightRoute> found = repository.findAll(spec, pageable);
        Map<String, Object> payload = new HashMap<>();
        payload.put("flightRoutes", found.getContent().stream().map(this::toDTO).toList());
        payload.put("currentPage", found.getNumber());
        payload.put("totalItems", found.getTotalElements());
        payload.put("totalPages", found.getTotalPages());
        payload.put("pageSize", found.getSize());
        payload.put("validSortFields", SORTABLE);
        /* The name the caller may send back, not the entity path it resolved to. */
        payload.put("currentSortBy", SORT_PATHS.containsKey(sortBy == null ? "" : sortBy) ? sortBy : "id");
        payload.put("currentSortDirection", sortDirection);
        return ResponseEntity.ok(ApiResponse.success(200, "Flight routes retrieved successfully", payload));
    }

    @Transactional(readOnly = true)
    /**
     * One record, plus where it sits in the list you came from.
     *
     * <p>The filters are taken again rather than ignored: the arrows have to walk the SAME
     * set that was on screen. Paging from a filtered list into records that were never in it
     * is worse than having no arrows, which is the house rule for every other module.
     */
    public ResponseEntity<ApiResponse<?>> getById(String id, String keyword, String airlineId, String originAirstripId, String destinationAirstripId, Boolean isOnRequest, Boolean isActive,
                                                  String sortBy, String sortDirection) {
        Long decoded = idObfuscator.decodeId(id);
        FlightRoute route = decoded == null ? null : repository.findById(decoded).orElse(null);
        if (route == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "No flight route with that id", "FLIGHT_ROUTE_NOT_FOUND"));
        }
        String navSortBy = SORT_PATHS.getOrDefault(sortBy == null ? "" : sortBy, "id");
        Map<String, Object> nav = recordNavigation.navigate(FlightRoute.class,
            buildSpec(keyword, airlineId, originAirstripId, destinationAirstripId, isOnRequest, isActive), navSortBy, !"desc".equalsIgnoreCase(sortDirection), decoded);
        Long nextRaw = (Long) nav.get("nextRawId");
        Long prevRaw = (Long) nav.get("previousRawId");

        Map<String, Object> payload = new HashMap<>();
        payload.put("flightRoute", toDTO(route));
        payload.put("nextId", nextRaw == null ? null : idObfuscator.encodeId(nextRaw));
        payload.put("previousId", prevRaw == null ? null : idObfuscator.encodeId(prevRaw));
        payload.put("position", nav.get("position"));
        payload.put("total", nav.get("total"));
        return ResponseEntity.ok(ApiResponse.success(200, "Flight route retrieved successfully", payload));
    }

    public ResponseEntity<ApiResponse<?>> create(CreateFlightRouteDTO dto) {
        Airline airline = find(airlines, dto.getAirlineId());
        if (airline == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "No airline with that id", "AIRLINE_NOT_FOUND"));
        }
        Airstrip origin = find(airstrips, dto.getOriginAirstripId());
        Airstrip destination = find(airstrips, dto.getDestinationAirstripId());
        if (origin == null || destination == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400,
                "Both the origin and the destination must be airstrips that exist", "AIRSTRIP_NOT_FOUND"));
        }
        if (origin.getId().equals(destination.getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400,
                "A sector cannot start and end at " + origin.getCode(), "ROUTE_ENDS_ARE_THE_SAME"));
        }
        if (repository.existsByAirlineIdAndOriginAirstripIdAndDestinationAirstripId(
                airline.getId(), origin.getId(), destination.getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400,
                airline.getName() + " already has a " + origin.getCode() + " to " + destination.getCode()
                    + " sector. Departures that differ belong on its fares, not on a second route.",
                "ROUTE_ALREADY_EXISTS"));
        }

        FlightRoute route = repository.saveAndFlush(FlightRoute.builder()
            .airline(airline)
            .originAirstrip(origin)
            .destinationAirstrip(destination)
            .isOnRequest(dto.getIsOnRequest() != null && dto.getIsOnRequest())
            .minimumSeats(dto.getMinimumSeats())
            .remarks(dto.getRemarks())
            .isActive(dto.getIsActive() == null || dto.getIsActive())
            .build());
        route.setCode(route.generateCode());
        route = repository.save(route);

        Map<String, Object> payload = new HashMap<>();
        payload.put("flightRoute", toDTO(route));
        return ResponseEntity.status(201).body(ApiResponse.success(201, "Flight route created successfully", payload));
    }

    public ResponseEntity<ApiResponse<?>> update(String id, UpdateFlightRouteDTO dto) {
        Long decoded = idObfuscator.decodeId(id);
        FlightRoute route = decoded == null ? null : repository.findById(decoded).orElse(null);
        if (route == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "No flight route with that id", "FLIGHT_ROUTE_NOT_FOUND"));
        }
        if (dto.getIsOnRequest() != null) route.setIsOnRequest(dto.getIsOnRequest());
        if (dto.getMinimumSeats() != null) route.setMinimumSeats(dto.getMinimumSeats());
        if (dto.getRemarks() != null) route.setRemarks(dto.getRemarks());
        if (dto.getIsActive() != null) route.setIsActive(dto.getIsActive());

        Map<String, Object> payload = new HashMap<>();
        payload.put("flightRoute", toDTO(repository.save(route)));
        return ResponseEntity.ok(ApiResponse.success(200, "Flight route updated successfully", payload));
    }

    /**
     * Deleting a route takes its fares with it — they describe departures of this sector and mean
     * nothing without it. A route a TRIP is using is refused, because that would silently strip a
     * flight from an itinerary somebody has already quoted.
     */
    public ResponseEntity<ApiResponse<?>> delete(List<String> ids) {
        List<Map<String, Object>> skipped = new ArrayList<>();
        List<String> deleted = new ArrayList<>();
        for (String id : ids) {
            Long decoded = idObfuscator.decodeId(id);
            FlightRoute route = decoded == null ? null : repository.findById(decoded).orElse(null);
            if (route == null) {
                skipped.add(Map.of("id", id, "reason", "No flight route with that id"));
                continue;
            }
            long onTrips = dayFlights.countByFlightRouteId(route.getId());
            if (onTrips > 0) {
                skipped.add(Map.of("id", id, "code", String.valueOf(route.getCode()),
                    "reason", onTrips + " trip day(s) use this sector. Deactivate it instead."));
                continue;
            }
            repository.delete(route);
            deleted.add(id);
        }
        Map<String, Object> report = new HashMap<>();
        report.put("deletedCount", deleted.size());
        report.put("deletedIds", deleted);
        report.put("skipped", skipped);
        return ResponseEntity.ok(ApiResponse.success(200, deleted.size() + " flight route(s) deleted", report));
    }

    // ---- helpers --------------------------------------------------------------------------------

    private <T> T find(org.springframework.data.jpa.repository.JpaRepository<T, Long> repo, String obfuscated) {
        Long decoded = idObfuscator.decodeId(obfuscated);
        return decoded == null ? null : repo.findById(decoded).orElse(null);
    }

    private Specification<FlightRoute> buildSpec(String keyword, String airlineId, String originId,
                                                 String destinationId, Boolean isOnRequest, Boolean isActive) {
        Long airline = optionalId(airlineId);
        Long origin = optionalId(originId);
        Long destination = optionalId(destinationId);
        return (root, query, cb) -> {
            List<Predicate> and = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                /*
                 * Searching a sector means searching its ends, which live on another table — so the
                 * join is here and the result is distinct, or a two-ended match returns the row twice.
                 */
                var o = root.join("originAirstrip");
                var d = root.join("destinationAirstrip");
                var al = root.join("airline");

                /*
                 * Every WORD has to match something, rather than the whole phrase matching one
                 * field. A sector is displayed as "Arusha to Zanzibar", so that is what people
                 * type — and as a single LIKE it matched nothing at all, because no column holds
                 * both ends. Split into words, each must appear somewhere, and the pair narrows
                 * to the one sector instead of every flight touching Arusha.
                 *
                 * The joining words are dropped. "to" is in the label we print and in no airstrip
                 * name, so requiring it would reject the very phrase the screen suggests.
                 */
                for (String word : keyword.trim().toLowerCase().split("\\s+")) {
                    if (word.isEmpty() || JOINERS.contains(word)) continue;
                    String like = "%" + word + "%";
                    and.add(cb.or(
                        cb.like(cb.lower(o.get("code")), like), cb.like(cb.lower(o.get("name")), like),
                        cb.like(cb.lower(d.get("code")), like), cb.like(cb.lower(d.get("name")), like),
                        cb.like(cb.lower(al.get("name")), like)));
                }
                if (query != null) query.distinct(true);
            }
            if (airline != null) and.add(cb.equal(root.get("airline").get("id"), airline));
            if (origin != null) and.add(cb.equal(root.get("originAirstrip").get("id"), origin));
            if (destination != null) and.add(cb.equal(root.get("destinationAirstrip").get("id"), destination));
            if (isOnRequest != null) and.add(cb.equal(root.get("isOnRequest"), isOnRequest));
            if (isActive != null) and.add(cb.equal(root.get("isActive"), isActive));
            return and.isEmpty() ? cb.conjunction() : cb.and(and.toArray(new Predicate[0]));
        };
    }

    /**
     * Decode an optional filter id, or null when it was not supplied.
     *
     * <p>{@code IdObfuscator.decodeId} throws "Hash cannot be null or empty" on a null, and every id
     * here is an OPTIONAL query parameter that is absent on most requests — so calling it directly
     * turns the unfiltered listing, which is the one the panel opens with, into a 400. Same shape as
     * the List.of(...).contains(null) trap next door: a helper that rejects null, handed the null
     * that absence produces.
     */
    private Long optionalId(String obfuscated) {
        return obfuscated == null || obfuscated.isBlank() ? null : idObfuscator.decodeId(obfuscated);
    }

    private int clamp(Integer size) { return size == null || size < 1 ? 50 : Math.min(size, 100); }

    private FlightRouteDTO toDTO(FlightRoute r) {
        Airline airline = r.getAirline();
        Airstrip origin = r.getOriginAirstrip();
        Airstrip destination = r.getDestinationAirstrip();
        return FlightRouteDTO.builder()
            .id(idObfuscator.encodeId(r.getId()))
            .code(r.getCode())
            .airlineId(airline == null ? null : idObfuscator.encodeId(airline.getId()))
            .airlineName(airline == null ? null : airline.getName())
            .originAirstripId(origin == null ? null : idObfuscator.encodeId(origin.getId()))
            .originCode(origin == null ? null : origin.getCode())
            .originName(origin == null ? null : origin.getName())
            .destinationAirstripId(destination == null ? null : idObfuscator.encodeId(destination.getId()))
            .destinationCode(destination == null ? null : destination.getCode())
            .destinationName(destination == null ? null : destination.getName())
            .sectorLabel(r.getSectorLabel())
            .isOnRequest(r.getIsOnRequest())
            .minimumSeats(r.getMinimumSeats())
            .remarks(r.getRemarks())
            .isActive(r.getIsActive())
            .fareCount(fares.countByFlightRouteId(r.getId()))
            .createdAt(r.getCreatedAt())
            .updatedAt(r.getUpdatedAt())
            .build();
    }
}
