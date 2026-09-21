package com.itineraryledger.kabengosafaris.Flight.Services;

import java.text.Normalizer;
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

import com.itineraryledger.kabengosafaris.Flight.DTOs.AirstripDTO;
import com.itineraryledger.kabengosafaris.Flight.DTOs.CreateAirstripDTO;
import com.itineraryledger.kabengosafaris.Flight.DTOs.UpdateAirstripDTO;
import com.itineraryledger.kabengosafaris.Flight.Entity.Airstrip;
import com.itineraryledger.kabengosafaris.Flight.Repository.AirstripRepository;
import com.itineraryledger.kabengosafaris.Flight.Repository.FlightRouteRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Airstrips — where aircraft land.
 *
 * <p>A shared lookup rather than something an airline owns: two airlines flying into Seronera are
 * flying into the same strip, and that is what makes "who serves Seronera" answerable.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AirstripService {

    private final AirstripRepository repository;
    private final FlightRouteRepository routes;
    private final IdObfuscator idObfuscator;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    private static final List<String> SORTABLE = List.of("code", "name", "region", "country", "createdAt");

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getAll(String keyword, String region, Boolean isActive,
                                                 Integer page, Integer size, String sortBy, String sortDirection) {
        Specification<Airstrip> spec = buildSpec(keyword, region, isActive);

        /*
         * The null check is NOT redundant. SORTABLE is a List.of, which forbids nulls and
         * throws NullPointerException from contains(null) rather than answering false — and
         * sortBy is null on every request that does not ask for a sort, which is most of them.
         * Without it the whole listing answers 500.
         */
        String sortField = sortBy != null && SORTABLE.contains(sortBy) ? sortBy : "name";
        Sort sort = Sort.by("desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC, sortField);
        Pageable pageable = PageRequest.of(page == null ? 0 : page, clamp(size), sort);

        Page<Airstrip> found = repository.findAll(spec, pageable);

        Map<String, Object> payload = new HashMap<>();
        payload.put("airstrips", found.getContent().stream().map(this::toDTO).toList());
        payload.put("currentPage", found.getNumber());
        payload.put("totalItems", found.getTotalElements());
        payload.put("totalPages", found.getTotalPages());
        payload.put("pageSize", found.getSize());
        payload.put("validSortFields", SORTABLE);
        payload.put("currentSortBy", sortField);
        payload.put("currentSortDirection", sortDirection);
        return ResponseEntity.ok(ApiResponse.success(200, "Airstrips retrieved successfully", payload));
    }

    @Transactional(readOnly = true)
    /**
     * One record, plus where it sits in the list you came from.
     *
     * <p>The filters are taken again rather than ignored: the arrows have to walk the SAME
     * set that was on screen. Paging from a filtered list into records that were never in it
     * is worse than having no arrows, which is the house rule for every other module.
     */
    public ResponseEntity<ApiResponse<?>> getById(String id, String keyword, String region, Boolean isActive,
                                                  String sortBy, String sortDirection) {
        /*
         * Written as a branch rather than map/orElseGet on purpose: the two arms infer
         * ApiResponse<Object> and ApiResponse<String> respectively, and neither widens to the
         * ApiResponse<?> this method returns.
         */
        Long decoded = idObfuscator.decodeId(id);
        Airstrip strip = decoded == null ? null : repository.findById(decoded).orElse(null);
        if (strip == null) {
            return ResponseEntity.status(404)
                .body(ApiResponse.error(404, "No airstrip with that id", "AIRSTRIP_NOT_FOUND"));
        }
        String navSortBy = sortBy != null && SORTABLE.contains(sortBy) ? sortBy : "code";
        Map<String, Object> nav = recordNavigation.navigate(Airstrip.class,
            buildSpec(keyword, region, isActive), navSortBy, !"desc".equalsIgnoreCase(sortDirection), decoded);
        Long nextRaw = (Long) nav.get("nextRawId");
        Long prevRaw = (Long) nav.get("previousRawId");

        Map<String, Object> payload = new HashMap<>();
        payload.put("airstrip", toDTO(strip));
        payload.put("nextId", nextRaw == null ? null : idObfuscator.encodeId(nextRaw));
        payload.put("previousId", prevRaw == null ? null : idObfuscator.encodeId(prevRaw));
        payload.put("position", nav.get("position"));
        payload.put("total", nav.get("total"));
        return ResponseEntity.ok(ApiResponse.success(200, "Airstrip retrieved successfully", payload));
    }

    public ResponseEntity<ApiResponse<?>> create(CreateAirstripDTO dto) {
        String code = dto.getCode().trim().toUpperCase(Locale.ROOT);
        if (repository.existsByCodeIgnoreCase(code)) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400,
                "An airstrip with code " + code + " already exists", "AIRSTRIP_CODE_TAKEN"));
        }
        String slug = uniqueSlug(dto.getName());

        Airstrip strip = Airstrip.builder()
            .code(code)
            .name(dto.getName().trim())
            .slug(slug)
            .region(dto.getRegion())
            .district(dto.getDistrict())
            .country(dto.getCountry() == null || dto.getCountry().isBlank() ? "Tanzania" : dto.getCountry())
            .latitude(dto.getLatitude())
            .longitude(dto.getLongitude())
            .notes(dto.getNotes())
            .isActive(dto.getIsActive() == null || dto.getIsActive())
            .build();

        Airstrip saved = repository.save(strip);
        Map<String, Object> payload = new HashMap<>();
        payload.put("airstrip", toDTO(saved));
        return ResponseEntity.status(201).body(ApiResponse.success(201, "Airstrip created successfully", payload));
    }

    public ResponseEntity<ApiResponse<?>> update(String id, UpdateAirstripDTO dto) {
        Long decoded = idObfuscator.decodeId(id);
        Airstrip strip = repository.findById(decoded).orElse(null);
        if (strip == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "No airstrip with that id", "AIRSTRIP_NOT_FOUND"));
        }
        /* Null means "leave alone" on every field: the house patch semantics. */
        if (dto.getCode() != null) {
            String code = dto.getCode().trim().toUpperCase(Locale.ROOT);
            if (!code.equalsIgnoreCase(strip.getCode()) && repository.existsByCodeIgnoreCase(code)) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400,
                    "An airstrip with code " + code + " already exists", "AIRSTRIP_CODE_TAKEN"));
            }
            strip.setCode(code);
        }
        if (dto.getName() != null) strip.setName(dto.getName().trim());
        if (dto.getRegion() != null) strip.setRegion(dto.getRegion());
        if (dto.getDistrict() != null) strip.setDistrict(dto.getDistrict());
        if (dto.getCountry() != null) strip.setCountry(dto.getCountry());
        if (dto.getLatitude() != null) strip.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null) strip.setLongitude(dto.getLongitude());
        if (dto.getNotes() != null) strip.setNotes(dto.getNotes());
        if (dto.getIsActive() != null) strip.setIsActive(dto.getIsActive());

        Map<String, Object> payload = new HashMap<>();
        payload.put("airstrip", toDTO(repository.save(strip)));
        return ResponseEntity.ok(ApiResponse.success(200, "Airstrip updated successfully", payload));
    }

    /**
     * Reference-checked, like every other delete here: a strip still on a route is refused by name
     * rather than by a foreign key error somewhere the user cannot see.
     */
    public ResponseEntity<ApiResponse<?>> delete(List<String> ids) {
        List<Map<String, Object>> skipped = new java.util.ArrayList<>();
        List<String> deleted = new java.util.ArrayList<>();

        for (String id : ids) {
            Long decoded = idObfuscator.decodeId(id);
            Airstrip strip = decoded == null ? null : repository.findById(decoded).orElse(null);
            if (strip == null) {
                skipped.add(Map.of("id", id, "reason", "No airstrip with that id"));
                continue;
            }
            long inUse = routes.countByOriginAirstripIdOrDestinationAirstripId(strip.getId(), strip.getId());
            if (inUse > 0) {
                skipped.add(Map.of("id", id, "code", strip.getCode(),
                    "reason", inUse + " route(s) fly through here. Deactivate it instead."));
                continue;
            }
            repository.delete(strip);
            deleted.add(id);
        }

        Map<String, Object> report = new HashMap<>();
        report.put("deletedCount", deleted.size());
        report.put("deletedIds", deleted);
        report.put("skipped", skipped);
        return ResponseEntity.ok(ApiResponse.success(200,
            deleted.size() + " airstrip(s) deleted", report));
    }

    // ---- helpers --------------------------------------------------------------------------------

    private Specification<Airstrip> buildSpec(String keyword, String region, Boolean isActive) {
        return (root, query, cb) -> {
            List<Predicate> and = new java.util.ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                and.add(cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("code")), like),
                    cb.like(cb.lower(root.get("region")), like)));
            }
            if (region != null && !region.isBlank()) and.add(cb.equal(root.get("region"), region));
            if (isActive != null) and.add(cb.equal(root.get("isActive"), isActive));
            return and.isEmpty() ? cb.conjunction() : cb.and(and.toArray(new Predicate[0]));
        };
    }

    private int clamp(Integer size) {
        if (size == null || size < 1) return 50;
        return Math.min(size, 100);
    }

    private String uniqueSlug(String name) {
        String base = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "airstrip";
        String slug = base;
        int n = 2;
        while (repository.existsBySlug(slug)) slug = base + "-" + n++;
        return slug;
    }

    private AirstripDTO toDTO(Airstrip strip) {
        return AirstripDTO.builder()
            .id(idObfuscator.encodeId(strip.getId()))
            .code(strip.getCode())
            .name(strip.getName())
            .slug(strip.getSlug())
            .displayName(strip.getDisplayName())
            .region(strip.getRegion())
            .district(strip.getDistrict())
            .country(strip.getCountry())
            .latitude(strip.getLatitude())
            .longitude(strip.getLongitude())
            .notes(strip.getNotes())
            .isActive(strip.getIsActive())
            .routeCount(routes.countByOriginAirstripIdOrDestinationAirstripId(strip.getId(), strip.getId()))
            .createdAt(strip.getCreatedAt())
            .updatedAt(strip.getUpdatedAt())
            .build();
    }
}
