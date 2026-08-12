package com.itineraryledger.kabengosafaris.Park.Services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.Park.ParkSpecification;
import com.itineraryledger.kabengosafaris.Park.ParkType;
import com.itineraryledger.kabengosafaris.Park.DTOs.ParkDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ParkGetService - Service for retrieving parks with filtering, pagination, and sorting
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class ParkGetService {

    private final ParkRepository parkRepository;
    private final IdObfuscator idObfuscator;
    private final com.itineraryledger.kabengosafaris.Response.ListStats listStats;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "name", "slug", "parkType", "region", "district", "isActive", "isWebActive", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    /** Ceiling on the id walk backing prev/next, so a huge filter can't OOM. */
    private static final int NAV_ID_LIMIT = 20_000;
    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    public ParkGetService(
        ParkRepository parkRepository,
        IdObfuscator idObfuscator,
        com.itineraryledger.kabengosafaris.Response.ListStats listStats
    ) {
        this.parkRepository = parkRepository;
        this.idObfuscator = idObfuscator;
        this.listStats = listStats;
    }

    /**
     * Get a single park by obfuscated ID
     *
     * @param idObfuscated The obfuscated park ID
     * @return ResponseEntity with ApiResponse containing the park
     */
    public ResponseEntity<ApiResponse<?>> getParkById(String idObfuscated) {
        return getParkById(idObfuscated, null, null, null, null, null, null, null);
    }

    /**
     * Get a single park, with prev/next walking the SAME filtered + sorted set the
     * list page was showing. Paging from a "Missing images" list therefore stays
     * inside that set, and `position`/`total` make the wraparound visible.
     */
    public ResponseEntity<ApiResponse<?>> getParkById(
        String idObfuscated,
        String keyword,
        java.util.List<ParkType> parkTypes,
        java.util.List<String> statuses,
        java.util.List<String> visibilities,
        java.util.List<String> qualities,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching park with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode park ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid park ID",
                        "INVALID_PARK_ID"
                    )
                );
            }

            // Find park
            Park park = parkRepository.findById(id).orElse(null);
            if (park == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Park not found",
                        "PARK_NOT_FOUND"
                    )
                );
            }

            // Convert to DTO
            ParkDTO parkDTO = convertToDTO(park);

            // Circular navigation across the caller's filtered + sorted set
            Specification<Park> navSpec = buildSpec(
                null, null, null, null, null, null, null, null, null, keyword,
                parkTypes, statuses, visibilities, qualities, null, null
            );
            List<Long> orderedIds = navigationIds(navSpec, sortBy, sortDirection);
            int index = orderedIds.indexOf(id);

            Long nextId = null;
            Long previousId = null;
            if (index >= 0 && orderedIds.size() > 1) {
                nextId = orderedIds.get((index + 1) % orderedIds.size());
                previousId = orderedIds.get((index - 1 + orderedIds.size()) % orderedIds.size());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("park", parkDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            // lets the UI show "3 of 6" so wrapping past the last record is visible
            response.put("position", index >= 0 ? index + 1 : null);
            response.put("total", orderedIds.size());

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Park retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching park", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch park",
                    "PARK_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get a single park by slug
     *
     * @param slug The park slug
     * @return ResponseEntity with ApiResponse containing the park
     */
    public ResponseEntity<ApiResponse<?>> getParkBySlug(String slug) {
        log.info("Fetching park with slug: {}", slug);

        try {
            // Find park
            Park park = parkRepository.findBySlug(slug).orElse(null);
            if (park == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Park not found",
                        "PARK_NOT_FOUND"
                    )
                );
            }

            // Convert to DTO
            ParkDTO parkDTO = convertToDTO(park);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Park retrieved successfully",
                    parkDTO
                )
            );

        } catch (Exception e) {
            log.error("Error fetching park", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch park",
                    "PARK_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all parks with pagination, sorting, and filtering
     *
     * @param name Filter by name (partial match)
     * @param slug Filter by slug (partial match)
     * @param parkType Filter by park type
     * @param region Filter by region (partial match)
     * @param district Filter by district (partial match)
     * @param location Filter by location (partial match)
     * @param parkSize Filter by park size (partial match)
     * @param isActive Filter by active status
     * @param keyword Search keyword across multiple fields
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated parks
     */
    public ResponseEntity<ApiResponse<?>> getAllParks(
        String name,
        String slug,
        ParkType parkType,
        String region,
        String district,
        String location,
        String parkSize,
        Boolean isActive,
        Boolean isWebActive,
        String keyword,
        java.util.List<ParkType> parkTypes,
        java.util.List<String> statuses,
        java.util.List<String> visibilities,
        java.util.List<String> qualities,
        java.time.LocalDateTime createdAfter,
        java.time.LocalDateTime createdBefore,
        Boolean includeStats,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching all parks with filters");

        try {
            // ONE filter chain, shared with the stat counters below
            Specification<Park> spec = buildSpec(
                name, slug, parkType, region, district, location, parkSize,
                isActive, isWebActive, keyword,
                parkTypes, statuses, visibilities, qualities,
                createdAfter, createdBefore
            );

            // Set default pagination values
            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? Math.min(size, MAX_PAGE_SIZE) : 10;

            // Sorting with validation
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            Sort.Direction direction = Sort.Direction.DESC;
            if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            }

            // Create pageable
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, validatedSortBy));

            // Fetch parks
            Page<Park> parkPage = parkRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<ParkDTO> parkDTOs = parkPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Create response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("parks", parkDTOs);
            response.put("currentPage", parkPage.getNumber());
            response.put("totalItems", parkPage.getTotalElements());
            response.put("totalPages", parkPage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
            // stats ride in the SAME response, computed from the SAME spec, so the
            // dashboard cards and the table can never disagree
            if (!Boolean.FALSE.equals(includeStats)) {
                response.put("stats", computeStats(spec));
            }

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Parks retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching parks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch parks",
                    "PARKS_FETCH_FAILED"
                )
            );
        }
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    /**
     * Convert Park entity to ParkDTO
     */
    public ParkDTO convertToDTO(Park park) {
        ParkDTO dto = new ParkDTO();
        dto.setId(idObfuscator.encodeId(park.getId()));
        dto.setName(park.getName());
        dto.setSlug(park.getSlug());
        dto.setParkType(park.getParkType());
        dto.setRegion(park.getRegion());
        dto.setDistrict(park.getDistrict());
        dto.setLocation(park.getLocation());
        dto.setLatitude(park.getLatitude());
        dto.setLongitude(park.getLongitude());
        dto.setElevation(park.getElevation());
        dto.setSize(park.getSize());
        dto.setShortDescription(park.getShortDescription());
        dto.setFullDescription(park.getFullDescription());
        dto.setHistory(park.getHistory());
        dto.setEcosystem(park.getEcosystem());
        dto.setWildlife(park.getWildlife());
        dto.setVegetation(park.getVegetation());
        dto.setPrimaryImage(park.getPrimaryImage());
        dto.setBestTimeToVisit(park.getBestTimeToVisit());
        dto.setOpeningHours(park.getOpeningHours());
        dto.setAccessInformation(park.getAccessInformation());
        dto.setTags(park.getTags());
        dto.setIsActive(park.getIsActive());
        if (park.getVendor() != null) {
            dto.setVendorId(idObfuscator.encodeId(park.getVendor().getId()));
            dto.setVendorName(park.getVendor().getName());
        }
        dto.setIsWebActive(park.getIsWebActive());
        dto.setCreatedAt(park.getCreatedAt());
        dto.setUpdatedAt(park.getUpdatedAt());
        return dto;
    }

    /**
     * Shared filter chain — the rows, the stat counters and the prev/next walk
     * must always agree, so all three build their Specification here.
     */
    private Specification<Park> buildSpec(
        String name,
        String slug,
        ParkType parkType,
        String region,
        String district,
        String location,
        String parkSize,
        Boolean isActive,
        Boolean isWebActive,
        String keyword,
        java.util.List<ParkType> parkTypes,
        java.util.List<String> statuses,
        java.util.List<String> visibilities,
        java.util.List<String> qualities,
        java.time.LocalDateTime createdAfter,
        java.time.LocalDateTime createdBefore
    ) {
        Specification<Park> spec = Specification.unrestricted();

        // multi-value facets: OR inside each dimension, AND between dimensions
        if (parkTypes != null && !parkTypes.isEmpty()) {
            spec = spec.and(ParkSpecification.parkTypeIn(parkTypes));
        }
        if (statuses != null && !statuses.isEmpty()) {
            java.util.List<Boolean> states = new java.util.ArrayList<>();
            if (statuses.contains("active")) states.add(true);
            if (statuses.contains("inactive")) states.add(false);
            // both selected = contradictory pair, cancels to no constraint
            spec = spec.and(ParkSpecification.activeIn(states));
        }
        if (visibilities != null && !visibilities.isEmpty()) {
            java.util.List<Boolean> states = new java.util.ArrayList<>();
            if (visibilities.contains("on-web")) states.add(true);
            if (visibilities.contains("off-web")) states.add(false);
            spec = spec.and(ParkSpecification.webActiveIn(states));
        }
        if (qualities != null && !qualities.isEmpty()) {
            spec = spec.and(ParkSpecification.anyQualityIssue(
                qualities.contains("no-description"),
                qualities.contains("no-image"),
                qualities.contains("no-coordinates"),
                qualities.contains("no-tariff")
            ));
        }

        if (name != null && !name.isEmpty()) spec = spec.and(ParkSpecification.nameLike(name));
        if (slug != null && !slug.isEmpty()) spec = spec.and(ParkSpecification.slugLike(slug));
        if (parkType != null) spec = spec.and(ParkSpecification.hasParkType(parkType));
        if (region != null && !region.isEmpty()) spec = spec.and(ParkSpecification.regionLike(region));
        if (district != null && !district.isEmpty()) spec = spec.and(ParkSpecification.districtLike(district));
        if (location != null && !location.isEmpty()) spec = spec.and(ParkSpecification.locationLike(location));
        if (parkSize != null && !parkSize.isEmpty()) spec = spec.and(ParkSpecification.sizeLike(parkSize));
        if (isActive != null) spec = spec.and(ParkSpecification.isActive(isActive));
        if (isWebActive != null) spec = spec.and(ParkSpecification.isWebActive(isWebActive));
        if (keyword != null && !keyword.isEmpty()) spec = spec.and(ParkSpecification.searchKeyword(keyword));
        if (createdAfter != null) spec = spec.and(ParkSpecification.createdAfter(createdAfter));
        if (createdBefore != null) spec = spec.and(ParkSpecification.createdBefore(createdBefore));
        return spec;
    }

    /**
     * Dashboard counters for the CURRENT filter set. Every figure here is also
     * reachable as a filter — no stat exists that the user can't then drill into.
     */
    private Map<String, Object> computeStats(Specification<Park> base) {
        return listStats.of(Park.class, base)
            .total()
            .count("active", ParkSpecification.isActive(true))
            .complement("inactive", "active")
            .count("onWebsite", ParkSpecification.isWebActive(true))
            .complement("offWebsite", "onWebsite")
            .breakdown("byType", ParkType.values(), ParkSpecification::hasParkType)
            .recency(ParkSpecification::createdAfter)
            // actionable data-quality gaps — each one is also a filter
            .count("missingDescription", ParkSpecification.anyQualityIssue(true, false, false, false))
            .count("missingImage", ParkSpecification.anyQualityIssue(false, true, false, false))
            .count("missingCoordinates", ParkSpecification.anyQualityIssue(false, false, true, false))
            .count("missingTariff", ParkSpecification.anyQualityIssue(false, false, false, true))
            .build();
    }

    /**
     * The ordered id set behind a filtered list, used for record paging.
     * Capped at NAV_ID_LIMIT.
     */
    private List<Long> navigationIds(Specification<Park> spec, String sortBy, String sortDirection) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var root = query.from(Park.class);
        query.select(root.get("id"));

        var predicate = spec.toPredicate(root, query, cb);
        if (predicate != null) query.where(predicate);

        String field = validateSortField(sortBy);
        if (field == null) field = DEFAULT_SORT_FIELD;
        boolean asc = sortDirection != null && sortDirection.equalsIgnoreCase("asc");
        // id is the tiebreaker so the order is stable and matches the list page
        query.orderBy(
            asc ? cb.asc(root.get(field)) : cb.desc(root.get(field)),
            asc ? cb.asc(root.get("id")) : cb.desc(root.get("id"))
        );

        return entityManager.createQuery(query).setMaxResults(NAV_ID_LIMIT).getResultList();
    }

}
