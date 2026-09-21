package com.itineraryledger.kabengosafaris.Flight.Services;

import java.text.Normalizer;
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

import com.itineraryledger.kabengosafaris.Flight.DTOs.AirlineDTO;
import com.itineraryledger.kabengosafaris.Flight.DTOs.CreateAirlineDTO;
import com.itineraryledger.kabengosafaris.Flight.DTOs.UpdateAirlineDTO;
import com.itineraryledger.kabengosafaris.Flight.Entity.Airline;
import com.itineraryledger.kabengosafaris.Flight.Enums.MarkupType;
import com.itineraryledger.kabengosafaris.Flight.Repository.AirlineRepository;
import com.itineraryledger.kabengosafaris.Flight.Repository.FlightRouteRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Airlines, and the markup we add to their fares.
 *
 * <p>The markup is the reason this record matters beyond a name: it is set once here and inherited
 * by every sector the airline flies, so a change of policy is one edit rather than 243.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AirlineService {

    private final AirlineRepository repository;
    private final FlightRouteRepository routes;
    private final IdObfuscator idObfuscator;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    private static final List<String> SORTABLE = List.of("name", "code", "createdAt");

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getAll(String keyword, Boolean isActive,
                                                 Integer page, Integer size, String sortBy, String sortDirection) {
        Specification<Airline> spec = buildSpec(keyword, isActive);
        /*
         * The null check is NOT redundant. SORTABLE is a List.of, which forbids nulls and
         * throws NullPointerException from contains(null) rather than answering false — and
         * sortBy is null on every request that does not ask for a sort, which is most of them.
         * Without it the whole listing answers 500.
         */
        String sortField = sortBy != null && SORTABLE.contains(sortBy) ? sortBy : "name";
        Sort sort = Sort.by("desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC, sortField);
        Pageable pageable = PageRequest.of(page == null ? 0 : page, clamp(size), sort);

        Page<Airline> found = repository.findAll(spec, pageable);
        Map<String, Object> payload = new HashMap<>();
        payload.put("airlines", found.getContent().stream().map(this::toDTO).toList());
        payload.put("currentPage", found.getNumber());
        payload.put("totalItems", found.getTotalElements());
        payload.put("totalPages", found.getTotalPages());
        payload.put("pageSize", found.getSize());
        payload.put("validSortFields", SORTABLE);
        payload.put("currentSortBy", sortField);
        payload.put("currentSortDirection", sortDirection);
        return ResponseEntity.ok(ApiResponse.success(200, "Airlines retrieved successfully", payload));
    }

    @Transactional(readOnly = true)
    /**
     * One record, plus where it sits in the list you came from.
     *
     * <p>The filters are taken again rather than ignored: the arrows have to walk the SAME
     * set that was on screen. Paging from a filtered list into records that were never in it
     * is worse than having no arrows, which is the house rule for every other module.
     */
    public ResponseEntity<ApiResponse<?>> getById(String id, String keyword, Boolean isActive,
                                                  String sortBy, String sortDirection) {
        Long decoded = idObfuscator.decodeId(id);
        Airline airline = decoded == null ? null : repository.findById(decoded).orElse(null);
        if (airline == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "No airline with that id", "AIRLINE_NOT_FOUND"));
        }
        String navSortBy = sortBy != null && SORTABLE.contains(sortBy) ? sortBy : "name";
        Map<String, Object> nav = recordNavigation.navigate(Airline.class,
            buildSpec(keyword, isActive), navSortBy, !"desc".equalsIgnoreCase(sortDirection), decoded);
        Long nextRaw = (Long) nav.get("nextRawId");
        Long prevRaw = (Long) nav.get("previousRawId");

        Map<String, Object> payload = new HashMap<>();
        payload.put("airline", toDTO(airline));
        payload.put("nextId", nextRaw == null ? null : idObfuscator.encodeId(nextRaw));
        payload.put("previousId", prevRaw == null ? null : idObfuscator.encodeId(prevRaw));
        payload.put("position", nav.get("position"));
        payload.put("total", nav.get("total"));
        return ResponseEntity.ok(ApiResponse.success(200, "Airline retrieved successfully", payload));
    }

    public ResponseEntity<ApiResponse<?>> create(CreateAirlineDTO dto) {
        if (repository.existsByNameIgnoreCase(dto.getName().trim())) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400,
                "An airline called " + dto.getName().trim() + " already exists", "AIRLINE_NAME_TAKEN"));
        }
        MarkupType markupType;
        try {
            markupType = parseMarkup(dto.getMarkupType(), dto.getMarkupValue());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage(), "MARKUP_INVALID"));
        }

        Airline airline = Airline.builder()
            .name(dto.getName().trim())
            .slug(uniqueSlug(dto.getName()))
            .code(blankToNull(dto.getCode()))
            .website(dto.getWebsite())
            .baggageKg(dto.getBaggageKg())
            .baggageNotes(dto.getBaggageNotes())
            .markupType(markupType)
            .markupValue(markupType == null ? null : dto.getMarkupValue())
            .bookingTerms(dto.getBookingTerms())
            .cancellationPolicy(dto.getCancellationPolicy())
            .internalNotes(dto.getInternalNotes())
            .isActive(dto.getIsActive() == null || dto.getIsActive())
            .build();

        Map<String, Object> payload = new HashMap<>();
        payload.put("airline", toDTO(repository.save(airline)));
        return ResponseEntity.status(201).body(ApiResponse.success(201, "Airline created successfully", payload));
    }

    public ResponseEntity<ApiResponse<?>> update(String id, UpdateAirlineDTO dto) {
        Long decoded = idObfuscator.decodeId(id);
        Airline airline = decoded == null ? null : repository.findById(decoded).orElse(null);
        if (airline == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "No airline with that id", "AIRLINE_NOT_FOUND"));
        }
        if (dto.getName() != null) {
            String name = dto.getName().trim();
            if (!name.equalsIgnoreCase(airline.getName()) && repository.existsByNameIgnoreCase(name)) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400,
                    "An airline called " + name + " already exists", "AIRLINE_NAME_TAKEN"));
            }
            airline.setName(name);
        }
        if (dto.getCode() != null) airline.setCode(blankToNull(dto.getCode()));
        if (dto.getWebsite() != null) airline.setWebsite(dto.getWebsite());
        if (dto.getBaggageKg() != null) airline.setBaggageKg(dto.getBaggageKg());
        if (dto.getBaggageNotes() != null) airline.setBaggageNotes(dto.getBaggageNotes());
        if (dto.getBookingTerms() != null) airline.setBookingTerms(dto.getBookingTerms());
        if (dto.getCancellationPolicy() != null) airline.setCancellationPolicy(dto.getCancellationPolicy());
        if (dto.getInternalNotes() != null) airline.setInternalNotes(dto.getInternalNotes());
        if (dto.getIsActive() != null) airline.setIsActive(dto.getIsActive());

        /*
         * An EMPTY markupType clears the markup; null leaves it alone. Without the distinction there
         * is no way to say "this airline no longer has a default", which is the same clearing-a-set-
         * field problem the patch helpers elsewhere already had to solve.
         */
        if (dto.getMarkupType() != null) {
            if (dto.getMarkupType().isBlank()) {
                airline.setMarkupType(null);
                airline.setMarkupValue(null);
            } else {
                try {
                    BigDecimalHolder holder = new BigDecimalHolder(
                        dto.getMarkupValue() != null ? dto.getMarkupValue() : airline.getMarkupValue());
                    airline.setMarkupType(parseMarkup(dto.getMarkupType(), holder.value));
                    airline.setMarkupValue(holder.value);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage(), "MARKUP_INVALID"));
                }
            }
        } else if (dto.getMarkupValue() != null && airline.getMarkupType() != null) {
            airline.setMarkupValue(dto.getMarkupValue());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("airline", toDTO(repository.save(airline)));
        return ResponseEntity.ok(ApiResponse.success(200, "Airline updated successfully", payload));
    }

    public ResponseEntity<ApiResponse<?>> delete(List<String> ids) {
        List<Map<String, Object>> skipped = new ArrayList<>();
        List<String> deleted = new ArrayList<>();
        for (String id : ids) {
            Long decoded = idObfuscator.decodeId(id);
            Airline airline = decoded == null ? null : repository.findById(decoded).orElse(null);
            if (airline == null) {
                skipped.add(Map.of("id", id, "reason", "No airline with that id"));
                continue;
            }
            long inUse = routes.countByAirlineId(airline.getId());
            if (inUse > 0) {
                skipped.add(Map.of("id", id, "code", String.valueOf(airline.getName()),
                    "reason", inUse + " route(s) belong to this airline. Deactivate it instead."));
                continue;
            }
            repository.delete(airline);
            deleted.add(id);
        }
        Map<String, Object> report = new HashMap<>();
        report.put("deletedCount", deleted.size());
        report.put("deletedIds", deleted);
        report.put("skipped", skipped);
        return ResponseEntity.ok(ApiResponse.success(200, deleted.size() + " airline(s) deleted", report));
    }

    // ---- helpers --------------------------------------------------------------------------------

    /** Lets the update path carry a possibly-inherited value through without a second variable. */
    private static final class BigDecimalHolder {
        final java.math.BigDecimal value;
        BigDecimalHolder(java.math.BigDecimal value) { this.value = value; }
    }

    private MarkupType parseMarkup(String type, java.math.BigDecimal value) {
        if (type == null || type.isBlank()) return null;
        MarkupType parsed;
        try {
            parsed = MarkupType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Markup type must be PERCENT or AMOUNT, not \"" + type + "\"");
        }
        /* A type with no value would silently mark nothing up while looking configured. */
        if (value == null) {
            throw new IllegalArgumentException("A markup of " + parsed + " needs a value");
        }
        return parsed;
    }

    private Specification<Airline> buildSpec(String keyword, Boolean isActive) {
        return (root, query, cb) -> {
            List<Predicate> and = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                and.add(cb.or(cb.like(cb.lower(root.get("name")), like),
                              cb.like(cb.lower(root.get("code")), like)));
            }
            if (isActive != null) and.add(cb.equal(root.get("isActive"), isActive));
            return and.isEmpty() ? cb.conjunction() : cb.and(and.toArray(new Predicate[0]));
        };
    }

    private int clamp(Integer size) { return size == null || size < 1 ? 50 : Math.min(size, 100); }

    private String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private String uniqueSlug(String name) {
        String base = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "airline";
        String slug = base;
        int n = 2;
        while (repository.existsBySlug(slug)) slug = base + "-" + n++;
        return slug;
    }

    private AirlineDTO toDTO(Airline a) {
        return AirlineDTO.builder()
            .id(idObfuscator.encodeId(a.getId()))
            .code(a.getCode())
            .name(a.getName())
            .slug(a.getSlug())
            .website(a.getWebsite())
            .baggageKg(a.getBaggageKg())
            .baggageNotes(a.getBaggageNotes())
            .markupType(a.getMarkupType())
            .markupValue(a.getMarkupValue())
            .markupSummary(describeMarkup(a))
            .bookingTerms(a.getBookingTerms())
            .cancellationPolicy(a.getCancellationPolicy())
            .internalNotes(a.getInternalNotes())
            .isActive(a.getIsActive())
            .routeCount(routes.countByAirlineId(a.getId()))
            .createdAt(a.getCreatedAt())
            .updatedAt(a.getUpdatedAt())
            .build();
    }

    private String describeMarkup(Airline a) {
        if (!a.hasMarkup()) return "No markup — fares pass through at cost";
        return a.getMarkupType() == MarkupType.PERCENT
            ? a.getMarkupValue().stripTrailingZeros().toPlainString() + "% per person, on the fare before taxes"
            : a.getMarkupValue().stripTrailingZeros().toPlainString() + " per person, flat";
    }
}
