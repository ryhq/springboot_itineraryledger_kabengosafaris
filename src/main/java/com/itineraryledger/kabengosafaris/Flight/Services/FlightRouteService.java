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

    private static final List<String> SORTABLE = List.of("id", "createdAt");

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
        String sortField = sortBy != null && SORTABLE.contains(sortBy) ? sortBy : "id";
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
        payload.put("currentSortBy", sortField);
        payload.put("currentSortDirection", sortDirection);
        return ResponseEntity.ok(ApiResponse.success(200, "Flight routes retrieved successfully", payload));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getById(String id) {
        Long decoded = idObfuscator.decodeId(id);
        FlightRoute route = decoded == null ? null : repository.findById(decoded).orElse(null);
        if (route == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "No flight route with that id", "FLIGHT_ROUTE_NOT_FOUND"));
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("flightRoute", toDTO(route));
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
        Long airline = idObfuscator.decodeId(airlineId);
        Long origin = idObfuscator.decodeId(originId);
        Long destination = idObfuscator.decodeId(destinationId);
        return (root, query, cb) -> {
            List<Predicate> and = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                /*
                 * Searching a sector means searching its ends, which live on another table — so the
                 * join is here and the result is distinct, or a two-ended match returns the row twice.
                 */
                String like = "%" + keyword.trim().toLowerCase() + "%";
                var o = root.join("originAirstrip");
                var d = root.join("destinationAirstrip");
                var al = root.join("airline");
                and.add(cb.or(
                    cb.like(cb.lower(o.get("code")), like), cb.like(cb.lower(o.get("name")), like),
                    cb.like(cb.lower(d.get("code")), like), cb.like(cb.lower(d.get("name")), like),
                    cb.like(cb.lower(al.get("name")), like)));
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
