package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayFlight.Services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Flight.CostEstimation.FlightFarePricer;
import com.itineraryledger.kabengosafaris.Flight.Entity.FlightFare;
import com.itineraryledger.kabengosafaris.Flight.Entity.FlightRoute;
import com.itineraryledger.kabengosafaris.Flight.Enums.MarkupType;
import com.itineraryledger.kabengosafaris.Flight.Repository.FlightFareRepository;
import com.itineraryledger.kabengosafaris.Flight.Repository.FlightRouteRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayFlight.DTOs.CreateItineraryDayFlightDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayFlight.DTOs.ItineraryDayFlightDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayFlight.DTOs.UpdateItineraryDayFlightDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayFlight.Entity.ItineraryDayFlight;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayFlight.Repository.ItineraryDayFlightRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Repository.ItineraryDayRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Flights on a day of an itinerary. */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ItineraryDayFlightService {

    private final ItineraryDayFlightRepository repository;
    private final ItineraryDayRepository days;
    private final FlightRouteRepository routes;
    private final FlightFareRepository fares;
    private final IdObfuscator idObfuscator;

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getAll(String dayIdObfuscated) {
        Long dayId = idObfuscator.decodeId(dayIdObfuscated);
        List<ItineraryDayFlight> rows = repository.findByItineraryDayIdOrderBySortOrderAscIdAsc(dayId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("flights", rows.stream().map(this::toDTO).toList());
        payload.put("totalItems", rows.size());
        return ResponseEntity.ok(ApiResponse.success(200, "Flights retrieved successfully", payload));
    }

    public ResponseEntity<ApiResponse<?>> create(String dayIdObfuscated, CreateItineraryDayFlightDTO dto) {
        Long dayId = idObfuscator.decodeId(dayIdObfuscated);
        ItineraryDay day = days.findById(dayId).orElse(null);
        if (day == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "No day with that id", "DAY_NOT_FOUND"));
        }
        Long routeId = idObfuscator.decodeId(dto.getFlightRouteId());
        FlightRoute route = routeId == null ? null : routes.findById(routeId).orElse(null);
        if (route == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "No sector with that id", "FLIGHT_ROUTE_NOT_FOUND"));
        }

        FlightFare fare = null;
        if (dto.getFlightFareId() != null && !dto.getFlightFareId().isBlank()) {
            Long fareId = idObfuscator.decodeId(dto.getFlightFareId());
            fare = fareId == null ? null : fares.findById(fareId).orElse(null);
            if (fare == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "No fare with that id", "FLIGHT_FARE_NOT_FOUND"));
            }
            /*
             * A departure that belongs to a different sector would price this day off a flight it is
             * not taking — and nothing downstream would notice, because both ids resolve.
             */
            if (fare.getFlightRoute() == null || !fare.getFlightRoute().getId().equals(route.getId())) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400,
                    "That departure belongs to a different sector", "FARE_NOT_ON_THIS_ROUTE"));
            }
        }

        MarkupType markupType;
        try {
            markupType = parseMarkup(dto.getMarkupType(), dto.getMarkupValue());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage(), "MARKUP_INVALID"));
        }

        int nextOrder = (int) repository.countByItineraryDayId(dayId) + 1;

        ItineraryDayFlight flight = ItineraryDayFlight.builder()
            .itineraryDay(day)
            .flightRoute(route)
            .flightFare(fare)
            .passengerCount(dto.getPassengerCount())
            .isAlternative(dto.getIsAlternative() != null && dto.getIsAlternative())
            .isIncludedInPrice(dto.getIsIncludedInPrice() == null || dto.getIsIncludedInPrice())
            .markupType(markupType)
            .markupValue(markupType == null ? null : dto.getMarkupValue())
            .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : nextOrder)
            .notes(dto.getNotes())
            .build();

        Map<String, Object> payload = new HashMap<>();
        payload.put("flight", toDTO(repository.save(flight)));
        return ResponseEntity.status(201).body(ApiResponse.success(201, "Flight added to the day", payload));
    }

    public ResponseEntity<ApiResponse<?>> update(String id, UpdateItineraryDayFlightDTO dto) {
        Long decoded = idObfuscator.decodeId(id);
        ItineraryDayFlight flight = decoded == null ? null : repository.findById(decoded).orElse(null);
        if (flight == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "No flight with that id", "DAY_FLIGHT_NOT_FOUND"));
        }
        if (dto.getFlightRouteId() != null) {
            Long routeId = idObfuscator.decodeId(dto.getFlightRouteId());
            FlightRoute route = routeId == null ? null : routes.findById(routeId).orElse(null);
            if (route == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "No sector with that id", "FLIGHT_ROUTE_NOT_FOUND"));
            }
            flight.setFlightRoute(route);
            /* The chosen departure belonged to the old sector, so it cannot survive the change. */
            flight.setFlightFare(null);
        }
        if (dto.getFlightFareId() != null) {
            if (dto.getFlightFareId().isBlank()) {
                flight.setFlightFare(null);
            } else {
                Long fareId = idObfuscator.decodeId(dto.getFlightFareId());
                FlightFare fare = fareId == null ? null : fares.findById(fareId).orElse(null);
                if (fare == null) {
                    return ResponseEntity.badRequest().body(ApiResponse.error(400, "No fare with that id", "FLIGHT_FARE_NOT_FOUND"));
                }
                if (fare.getFlightRoute() == null
                        || !fare.getFlightRoute().getId().equals(flight.getFlightRoute().getId())) {
                    return ResponseEntity.badRequest().body(ApiResponse.error(400,
                        "That departure belongs to a different sector", "FARE_NOT_ON_THIS_ROUTE"));
                }
                flight.setFlightFare(fare);
            }
        }
        if (dto.getPassengerCount() != null) flight.setPassengerCount(dto.getPassengerCount());
        if (dto.getIsAlternative() != null) flight.setIsAlternative(dto.getIsAlternative());
        if (dto.getIsIncludedInPrice() != null) flight.setIsIncludedInPrice(dto.getIsIncludedInPrice());
        if (dto.getSortOrder() != null) flight.setSortOrder(dto.getSortOrder());
        if (dto.getNotes() != null) flight.setNotes(dto.getNotes());

        if (dto.getMarkupType() != null) {
            if (dto.getMarkupType().isBlank()) {
                flight.setMarkupType(null);
                flight.setMarkupValue(null);
            } else {
                try {
                    BigDecimal value = dto.getMarkupValue() != null ? dto.getMarkupValue() : flight.getMarkupValue();
                    flight.setMarkupType(parseMarkup(dto.getMarkupType(), value));
                    flight.setMarkupValue(value);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage(), "MARKUP_INVALID"));
                }
            }
        } else if (dto.getMarkupValue() != null && flight.getMarkupType() != null) {
            flight.setMarkupValue(dto.getMarkupValue());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("flight", toDTO(repository.save(flight)));
        return ResponseEntity.ok(ApiResponse.success(200, "Flight updated", payload));
    }

    /** Exactly one primary per day: the same rule the accommodation lines follow. */
    public ResponseEntity<ApiResponse<?>> makePrimary(String id) {
        Long decoded = idObfuscator.decodeId(id);
        ItineraryDayFlight chosen = decoded == null ? null : repository.findById(decoded).orElse(null);
        if (chosen == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "No flight with that id", "DAY_FLIGHT_NOT_FOUND"));
        }
        Long dayId = chosen.getItineraryDay().getId();
        for (ItineraryDayFlight sibling : repository.findByItineraryDayIdOrderBySortOrderAscIdAsc(dayId)) {
            boolean shouldBeAlternative = !sibling.getId().equals(chosen.getId());
            if (!shouldBeAlternative == sibling.isPrimary()) continue;
            sibling.setIsAlternative(shouldBeAlternative);
            repository.save(sibling);
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("flight", toDTO(repository.save(chosen)));
        return ResponseEntity.ok(ApiResponse.success(200, "Flight is now the primary for this day", payload));
    }

    public ResponseEntity<ApiResponse<?>> delete(List<String> ids) {
        List<String> deleted = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();
        for (String id : ids) {
            Long decoded = idObfuscator.decodeId(id);
            ItineraryDayFlight flight = decoded == null ? null : repository.findById(decoded).orElse(null);
            if (flight == null) {
                skipped.add(Map.of("id", id, "reason", "No flight with that id"));
                continue;
            }
            repository.delete(flight);
            deleted.add(id);
        }
        Map<String, Object> report = new HashMap<>();
        report.put("deletedCount", deleted.size());
        report.put("deletedIds", deleted);
        report.put("skipped", skipped);
        return ResponseEntity.ok(ApiResponse.success(200, deleted.size() + " flight(s) removed", report));
    }

    // ---- helpers --------------------------------------------------------------------------------

    private MarkupType parseMarkup(String type, BigDecimal value) {
        if (type == null || type.isBlank()) return null;
        MarkupType parsed;
        try {
            parsed = MarkupType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Markup type must be PERCENT or AMOUNT, not \"" + type + "\"");
        }
        if (value == null) throw new IllegalArgumentException("A markup of " + parsed + " needs a value");
        return parsed;
    }

    private ItineraryDayFlightDTO toDTO(ItineraryDayFlight flight) {
        FlightRoute route = flight.getFlightRoute();
        FlightFare fare = flight.getFlightFare();
        if (fare == null && route != null) {
            /* No departure chosen yet: the cheapest live one is what this day would cost today. */
            fare = fares.findLiveForRoute(route.getId()).stream().findFirst().orElse(null);
        }
        var priced = FlightFarePricer.price(fare, route == null ? null : route.getAirline(),
            1, 0, flight.getMarkupType(), flight.getMarkupValue(), null);

        return ItineraryDayFlightDTO.builder()
            .id(idObfuscator.encodeId(flight.getId()))
            .itineraryDayId(idObfuscator.encodeId(flight.getItineraryDay().getId()))
            .flightRouteId(route == null ? null : idObfuscator.encodeId(route.getId()))
            .flightFareId(flight.getFlightFare() == null
                ? null : idObfuscator.encodeId(flight.getFlightFare().getId()))
            .airlineName(route == null || route.getAirline() == null ? null : route.getAirline().getName())
            .sectorLabel(route == null ? null : route.getSectorLabel())
            .etd(fare == null || fare.getEtd() == null ? null : fare.getEtd().toString())
            .eta(fare == null || fare.getEta() == null ? null : fare.getEta().toString())
            .departureLabel(fare == null ? null : fare.getDepartureLabel())
            .displayName(flight.getDisplayName())
            .passengerCount(flight.getPassengerCount())
            .isAlternative(flight.getIsAlternative())
            .isIncludedInPrice(flight.getIsIncludedInPrice())
            .markupType(flight.getMarkupType() == null ? null : flight.getMarkupType().name())
            .markupValue(flight.getMarkupValue())
            .markupSource(priced.markup().source())
            .sortOrder(flight.getSortOrder())
            .notes(flight.getNotes())
            .sellingPerAdult(priced.sellingPerAdult())
            .pricingNote(priced.warnings().isEmpty()
                ? "Markup from " + priced.markup().source()
                : String.join(" ", priced.warnings()))
            .createdAt(flight.getCreatedAt())
            .updatedAt(flight.getUpdatedAt())
            .build();
    }
}
