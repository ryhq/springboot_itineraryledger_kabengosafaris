package com.itineraryledger.kabengosafaris.Flight.Services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Flight.CostEstimation.FlightFarePricer;
import com.itineraryledger.kabengosafaris.Flight.DTOs.BulkUpsertFlightFareDTO;
import com.itineraryledger.kabengosafaris.Flight.DTOs.BulkUpsertFlightFareResponseDTO;
import com.itineraryledger.kabengosafaris.Flight.DTOs.FlightFareDTO;
import com.itineraryledger.kabengosafaris.Flight.DTOs.UpdateFlightFareDTO;
import com.itineraryledger.kabengosafaris.Flight.Entity.FlightFare;
import com.itineraryledger.kabengosafaris.Flight.Entity.FlightRoute;
import com.itineraryledger.kabengosafaris.Flight.Enums.MarkupType;
import com.itineraryledger.kabengosafaris.Flight.Repository.FlightFareRepository;
import com.itineraryledger.kabengosafaris.Flight.Repository.FlightRouteRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayFlight.Repository.ItineraryDayFlightRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Fares — one published departure of a sector, in one contract window.
 *
 * <p>Written through a bulk upsert rather than a create endpoint, because a price list arrives all
 * at once and is re-imported when the airline reissues it. A row already present for the same
 * sector, departure time and operating window is updated; it is never duplicated.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class FlightFareService {

    private final FlightFareRepository repository;
    private final FlightRouteRepository routes;
    private final ItineraryDayFlightRepository dayFlights;
    private final IdObfuscator idObfuscator;

    private static final List<String> SORTABLE = List.of("etd", "netFare", "validFrom", "id", "createdAt");
    private static final String[] MONTH_NAMES = {
        "", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December" };

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getAll(String keyword, String flightRouteId, String airlineId,
                                                 Boolean isActive, Boolean includeRetired, Boolean missingNetFare,
                                                 Integer page, Integer size, String sortBy, String sortDirection) {
        Specification<FlightFare> spec = buildSpec(keyword, flightRouteId, airlineId, isActive,
            includeRetired, missingNetFare);

        /*
         * The null check is NOT redundant. SORTABLE is a List.of, which forbids nulls and
         * throws NullPointerException from contains(null) rather than answering false — and
         * sortBy is null on every request that does not ask for a sort, which is most of them.
         * Without it the whole listing answers 500.
         */
        String sortField = sortBy != null && SORTABLE.contains(sortBy) ? sortBy : "id";
        Sort sort = Sort.by("desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC, sortField);
        Pageable pageable = PageRequest.of(page == null ? 0 : page, clamp(size), sort);

        Page<FlightFare> found = repository.findAll(spec, pageable);
        Map<String, Object> payload = new HashMap<>();
        payload.put("flightFares", found.getContent().stream().map(this::toDTO).toList());
        payload.put("currentPage", found.getNumber());
        payload.put("totalItems", found.getTotalElements());
        payload.put("totalPages", found.getTotalPages());
        payload.put("pageSize", found.getSize());
        payload.put("validSortFields", SORTABLE);
        payload.put("currentSortBy", sortField);
        payload.put("currentSortDirection", sortDirection);
        return ResponseEntity.ok(ApiResponse.success(200, "Flight fares retrieved successfully", payload));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getById(String id) {
        Long decoded = idObfuscator.decodeId(id);
        FlightFare fare = decoded == null ? null : repository.findById(decoded).orElse(null);
        if (fare == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "No flight fare with that id", "FLIGHT_FARE_NOT_FOUND"));
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("flightFare", toDTO(fare));
        return ResponseEntity.ok(ApiResponse.success(200, "Flight fare retrieved successfully", payload));
    }

    /**
     * Load a price list.
     *
     * <p>Row by row with a per-row reason, deliberately: a batch that fails as a whole tells nobody
     * which of 350 lines was wrong, and re-keying a price list to find out is a day's work.
     */
    public ResponseEntity<ApiResponse<?>> bulkUpsert(List<BulkUpsertFlightFareDTO> rows) {
        BulkUpsertFlightFareResponseDTO report = BulkUpsertFlightFareResponseDTO.builder().build();
        report.setTotalProcessed(rows == null ? 0 : rows.size());
        if (rows == null || rows.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(200, "Nothing to do", report));
        }

        int index = 0;
        for (BulkUpsertFlightFareDTO row : rows) {
            index++;
            try {
                Long routeId = idObfuscator.decodeId(row.getFlightRouteId());
                FlightRoute route = routeId == null ? null : routes.findById(routeId).orElse(null);
                if (route == null) {
                    throw new IllegalArgumentException("no flight route with id " + row.getFlightRouteId());
                }
                if (row.getValidTo().isBefore(row.getValidFrom())) {
                    throw new IllegalArgumentException("validity ends (" + row.getValidTo()
                        + ") before it starts (" + row.getValidFrom() + ")");
                }
                if (row.getNetFare() == null && row.getGrossFare() == null) {
                    throw new IllegalArgumentException("neither a net nor a gross fare, so it can say nothing");
                }
                MarkupType markupType = parseMarkup(row.getMarkupType(), row.getMarkupValue());

                /*
                 * The table's own key. Matching on it is what makes a re-import an update rather
                 * than a second copy of the whole price list.
                 */
                FlightFare existing = repository.findAll(
                    (root, query, cb) -> cb.and(
                        cb.equal(root.get("flightRoute").get("id"), route.getId()),
                        cb.equal(root.get("validFrom"), row.getValidFrom()),
                        row.getEtd() == null ? cb.isNull(root.get("etd")) : cb.equal(root.get("etd"), row.getEtd()),
                        row.getOperatingMonths() == null
                            ? cb.isNull(root.get("operatingMonths"))
                            : cb.equal(root.get("operatingMonths"), row.getOperatingMonths())))
                    .stream().findFirst().orElse(null);

                boolean isNew = existing == null;
                FlightFare fare = isNew ? new FlightFare() : existing;
                fare.setFlightRoute(route);
                fare.setEtd(row.getEtd());
                fare.setEta(row.getEta());
                fare.setDepartureLabel(row.getDepartureLabel());
                fare.setValidFrom(row.getValidFrom());
                fare.setValidTo(row.getValidTo());
                fare.setOperatingMonths(row.getOperatingMonths());
                fare.setNetFare(row.getNetFare());
                fare.setGrossFare(row.getGrossFare());
                fare.setTaxesAndFees(row.getTaxesAndFees() == null ? BigDecimal.ZERO : row.getTaxesAndFees());
                fare.setCurrency(row.getCurrency() == null || row.getCurrency().isBlank() ? "USD" : row.getCurrency());
                if (row.getChildPercent() != null) fare.setChildPercent(row.getChildPercent());
                fare.setMinimumSeats(row.getMinimumSeats());
                fare.setMarkupType(markupType);
                fare.setMarkupValue(markupType == null ? null : row.getMarkupValue());
                fare.setRemarks(row.getRemarks());
                if (isNew) fare.setIsActive(true);

                FlightFare saved = repository.saveAndFlush(fare);
                if (saved.getCode() == null) {
                    saved.setCode(saved.generateCode());
                    repository.save(saved);
                }
                if (isNew) report.setCreated(report.getCreated() + 1);
                else report.setUpdated(report.getUpdated() + 1);

            } catch (Exception e) {
                report.setFailed(report.getFailed() + 1);
                report.getErrors().add("row " + index + ": " + e.getMessage());
            }
        }

        return ResponseEntity.ok(ApiResponse.success(200,
            report.getCreated() + " created, " + report.getUpdated() + " updated, "
                + report.getFailed() + " failed", report));
    }

    public ResponseEntity<ApiResponse<?>> update(String id, UpdateFlightFareDTO dto) {
        Long decoded = idObfuscator.decodeId(id);
        FlightFare fare = decoded == null ? null : repository.findById(decoded).orElse(null);
        if (fare == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "No flight fare with that id", "FLIGHT_FARE_NOT_FOUND"));
        }
        if (dto.getEtd() != null) fare.setEtd(dto.getEtd());
        if (dto.getEta() != null) fare.setEta(dto.getEta());
        if (dto.getDepartureLabel() != null) fare.setDepartureLabel(dto.getDepartureLabel());
        if (dto.getValidFrom() != null) fare.setValidFrom(dto.getValidFrom());
        if (dto.getValidTo() != null) fare.setValidTo(dto.getValidTo());
        if (dto.getOperatingMonths() != null) fare.setOperatingMonths(dto.getOperatingMonths());
        if (dto.getNetFare() != null) fare.setNetFare(dto.getNetFare());
        if (dto.getGrossFare() != null) fare.setGrossFare(dto.getGrossFare());
        if (dto.getTaxesAndFees() != null) fare.setTaxesAndFees(dto.getTaxesAndFees());
        if (dto.getCurrency() != null) fare.setCurrency(dto.getCurrency());
        if (dto.getChildPercent() != null) fare.setChildPercent(dto.getChildPercent());
        if (dto.getMinimumSeats() != null) fare.setMinimumSeats(dto.getMinimumSeats());
        if (dto.getRemarks() != null) fare.setRemarks(dto.getRemarks());
        if (dto.getIsActive() != null) fare.setIsActive(dto.getIsActive());

        /* Retiring is reversible and is NOT a delete: a sent quote still points here. */
        if (dto.getIsRetired() != null) {
            fare.setRetiredAt(dto.getIsRetired() ? LocalDateTime.now() : null);
        }

        if (dto.getMarkupType() != null) {
            if (dto.getMarkupType().isBlank()) {
                fare.setMarkupType(null);
                fare.setMarkupValue(null);
            } else {
                try {
                    BigDecimal value = dto.getMarkupValue() != null ? dto.getMarkupValue() : fare.getMarkupValue();
                    fare.setMarkupType(parseMarkup(dto.getMarkupType(), value));
                    fare.setMarkupValue(value);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage(), "MARKUP_INVALID"));
                }
            }
        } else if (dto.getMarkupValue() != null && fare.getMarkupType() != null) {
            fare.setMarkupValue(dto.getMarkupValue());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("flightFare", toDTO(repository.save(fare)));
        return ResponseEntity.ok(ApiResponse.success(200, "Flight fare updated successfully", payload));
    }

    /**
     * A fare a trip has chosen is refused, with the count, rather than deleted.
     *
     * <p>Retiring is almost always what was wanted: it keeps the row for documents already sent and
     * takes it out of every future choice.
     */
    public ResponseEntity<ApiResponse<?>> delete(List<String> ids) {
        List<Map<String, Object>> skipped = new ArrayList<>();
        List<String> deleted = new ArrayList<>();
        for (String id : ids) {
            Long decoded = idObfuscator.decodeId(id);
            FlightFare fare = decoded == null ? null : repository.findById(decoded).orElse(null);
            if (fare == null) {
                skipped.add(Map.of("id", id, "reason", "No flight fare with that id"));
                continue;
            }
            long chosen = dayFlights.countByFlightFareId(fare.getId());
            if (chosen > 0) {
                skipped.add(Map.of("id", id, "code", String.valueOf(fare.getCode()),
                    "reason", chosen + " trip day(s) have chosen this departure. Retire it instead, "
                        + "which keeps the price on documents already sent."));
                continue;
            }
            repository.delete(fare);
            deleted.add(id);
        }
        Map<String, Object> report = new HashMap<>();
        report.put("deletedCount", deleted.size());
        report.put("deletedIds", deleted);
        report.put("skipped", skipped);
        return ResponseEntity.ok(ApiResponse.success(200, deleted.size() + " flight fare(s) deleted", report));
    }

    // ---- helpers --------------------------------------------------------------------------------

    private MarkupType parseMarkup(String type, BigDecimal value) {
        if (type == null || type.isBlank()) return null;
        MarkupType parsed;
        try {
            parsed = MarkupType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("markup type must be PERCENT or AMOUNT, not \"" + type + "\"");
        }
        if (value == null) throw new IllegalArgumentException("a markup of " + parsed + " needs a value");
        return parsed;
    }

    private Specification<FlightFare> buildSpec(String keyword, String flightRouteId, String airlineId,
                                                Boolean isActive, Boolean includeRetired, Boolean missingNetFare) {
        Long route = idObfuscator.decodeId(flightRouteId);
        Long airline = idObfuscator.decodeId(airlineId);
        return (root, query, cb) -> {
            List<Predicate> and = new ArrayList<>();
            /*
             * Search what the listing SHOWS, which is the sector and the airline — neither of which
             * lives on this table. A fare has no name of its own, so searching only its own columns
             * would answer nothing for every term a person would actually type.
             */
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                var sector = root.join("flightRoute");
                var origin = sector.join("originAirstrip");
                var destination = sector.join("destinationAirstrip");
                var carrier = sector.join("airline");
                and.add(cb.or(
                    cb.like(cb.lower(origin.get("code")), like),
                    cb.like(cb.lower(origin.get("name")), like),
                    cb.like(cb.lower(destination.get("code")), like),
                    cb.like(cb.lower(destination.get("name")), like),
                    cb.like(cb.lower(carrier.get("name")), like),
                    cb.like(cb.lower(root.get("code")), like),
                    cb.like(cb.lower(root.get("departureLabel")), like)));
                /* A fare matching at both ends would otherwise come back twice. */
                if (query != null) query.distinct(true);
            }
            if (route != null) and.add(cb.equal(root.get("flightRoute").get("id"), route));
            if (airline != null) and.add(cb.equal(root.get("flightRoute").get("airline").get("id"), airline));
            if (isActive != null) and.add(cb.equal(root.get("isActive"), isActive));
            /* Retired rows are out unless asked for: they exist for old documents, not new choices. */
            if (!Boolean.TRUE.equals(includeRetired)) and.add(cb.isNull(root.get("retiredAt")));
            /* The actionable data-quality counter: which fares cannot be quoted yet. */
            if (Boolean.TRUE.equals(missingNetFare)) and.add(cb.isNull(root.get("netFare")));
            else if (Boolean.FALSE.equals(missingNetFare)) and.add(cb.isNotNull(root.get("netFare")));
            return and.isEmpty() ? cb.conjunction() : cb.and(and.toArray(new Predicate[0]));
        };
    }

    private int clamp(Integer size) { return size == null || size < 1 ? 50 : Math.min(size, 100); }

    /** "June to November", or "January, February, March, June to December" for a split window. */
    private String describeMonths(String months) {
        if (months == null || months.isBlank()) return "No window stated";
        List<Integer> list = new ArrayList<>();
        for (String part : months.split(",")) {
            try { list.add(Integer.parseInt(part.trim())); } catch (NumberFormatException ignored) { }
        }
        if (list.isEmpty()) return "No window stated";
        if (list.size() == 12) return "Year round";
        List<String> runs = new ArrayList<>();
        int start = list.get(0), previous = start;
        for (int i = 1; i <= list.size(); i++) {
            Integer current = i < list.size() ? list.get(i) : null;
            if (current == null || current != previous + 1) {
                runs.add(start == previous ? MONTH_NAMES[start] : MONTH_NAMES[start] + " to " + MONTH_NAMES[previous]);
                if (current != null) start = current;
            }
            if (current != null) previous = current;
        }
        return String.join(", ", runs);
    }

    private FlightFareDTO toDTO(FlightFare f) {
        FlightRoute route = f.getFlightRoute();
        var priced = FlightFarePricer.price(f, route == null ? null : route.getAirline(),
            1, 0, null, null, null);
        return FlightFareDTO.builder()
            .id(idObfuscator.encodeId(f.getId()))
            .code(f.getCode())
            .flightRouteId(route == null ? null : idObfuscator.encodeId(route.getId()))
            .sectorLabel(route == null ? null : route.getSectorLabel())
            .airlineName(route == null || route.getAirline() == null ? null : route.getAirline().getName())
            .etd(f.getEtd())
            .eta(f.getEta())
            .departureLabel(f.getDepartureLabel())
            .validFrom(f.getValidFrom())
            .validTo(f.getValidTo())
            .operatingMonths(f.getOperatingMonths())
            .operatingWindow(describeMonths(f.getOperatingMonths()))
            .netFare(f.getNetFare())
            .grossFare(f.getGrossFare())
            .taxesAndFees(f.getTaxesAndFees())
            .currency(f.getCurrency())
            .childPercent(f.getChildPercent())
            .minimumSeats(f.getMinimumSeats())
            .markupType(f.getMarkupType())
            .markupValue(f.getMarkupValue())
            .remarks(f.getRemarks())
            .retiredAt(f.getRetiredAt())
            .isRetired(f.isRetired())
            .isActive(f.getIsActive())
            .sellingPerAdult(priced.sellingPerAdult())
            .pricingNote(priced.warnings().isEmpty()
                ? "Markup from " + priced.markup().source()
                : String.join(" ", priced.warnings()))
            .createdAt(f.getCreatedAt())
            .updatedAt(f.getUpdatedAt())
            .build();
    }
}
