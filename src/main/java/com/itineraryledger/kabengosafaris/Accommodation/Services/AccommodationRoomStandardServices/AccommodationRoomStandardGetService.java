package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRoomStandardServices;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRoomStandardDTOs.AccommodationRoomStandardDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomStandard;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRoomStandardRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AccommodationRoomStandardGetService - Service for retrieving accommodation room standards
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class AccommodationRoomStandardGetService {

    private final AccommodationRoomStandardRepository roomStandardRepository;

    // filter-aware prev/next + the N of M readout

    @org.springframework.beans.factory.annotation.Autowired

    private com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    // dashboard counters for the CURRENT filter set (see CLAUDE.md)

    @org.springframework.beans.factory.annotation.Autowired

    private com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "name", "maxOccupancy", "viewType", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public AccommodationRoomStandardGetService(
        AccommodationRoomStandardRepository roomStandardRepository,
        IdObfuscator idObfuscator
    ) {
        this.roomStandardRepository = roomStandardRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get a single accommodation room standard by ID
     *
     * @param idObfuscated The obfuscated room standard ID
     * @return ResponseEntity with ApiResponse containing the room standard
     */
    public ResponseEntity<ApiResponse<?>> getAccommodationRoomStandardById(String idObfuscated, String scopeParentId) {
        return getAccommodationRoomStandardById(idObfuscated, scopeParentId, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * One record, plus where it sits in the set the caller was looking at.
     *
     * The list's filters and sort arrive here because paging out of a filtered
     * list must stay inside that filter, and the N of M readout must count the
     * same set. Arrows that traverse a different set are worse than no arrows.
     */
    public ResponseEntity<ApiResponse<?>> getAccommodationRoomStandardById(
        String idObfuscated,
        String scopeParentId,
        /*
         * The global list filters by accommodation through a facet, not a scope; both
         * forms have to reach the walk or paging escapes the filter on screen.
         */
        String accommodationId,
        String name,
        String viewType,
        String floorLevel,
        Integer minOccupancy,
        Integer maxOccupancy,
        Boolean isActive,
        String keyword,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching accommodation room standard by ID: {}", idObfuscated);

        try {
            // Decode room standard ID
            Long roomStandardId;
            try {
                roomStandardId = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode room standard ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid room standard ID",
                        "INVALID_ROOM_STANDARD_ID"
                    )
                );
            }

            // Find room standard
            AccommodationRoomStandard roomStandard = roomStandardRepository.findById(roomStandardId).orElse(null);
            if (roomStandard == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Room standard not found",
                        "ROOM_STANDARD_NOT_FOUND"
                    )
                );
            }

            // Convert to DTO
            AccommodationRoomStandardDTO roomStandardDTO = convertToDTO(roomStandard);

            // Decode optional scope parent ID for scoped navigation
            Long decodedParentId = null;
            if (scopeParentId != null && !scopeParentId.isEmpty()) {
                try {
                    decodedParentId = idObfuscator.decodeId(scopeParentId);
                } catch (Exception ex) {
                    log.warn("Invalid scopeParentId: {}, falling back to global navigation", scopeParentId);
                }
            }

            /*
             * Prev/next walks the SAME set the caller was looking at — this parent's
             * children when scoped, everything otherwise — and returns the position so
             * the record page can show 'N of M' with the wraparound visible.
             */
            Specification<AccommodationRoomStandard> navSpec = buildSpec(decodedParentId != null ? decodedParentId : decodeOrNull(accommodationId), name, viewType, floorLevel, minOccupancy, maxOccupancy, isActive, keyword);
            String navSortBy = validateSortField(sortBy);
            if (navSortBy == null) navSortBy = DEFAULT_SORT_FIELD;
            boolean navAscending = sortDirection != null && sortDirection.equalsIgnoreCase("asc");
            java.util.Map<String, Object> nav = recordNavigation.navigate(
                AccommodationRoomStandard.class, navSpec, navSortBy, navAscending, roomStandardId
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("roomStandard", roomStandardDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));
            response.put("scopeParentId", scopeParentId);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Room standard retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching accommodation room standard by ID", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch room standard",
                    "ROOM_STANDARD_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all accommodation room standards with optional filters
     * Accommodation ID is optional
     *
     * @param accommodationId Optional accommodation ID filter
     * @param name Filter by name
     * @param viewType Filter by view type
     * @param floorLevel Filter by floor level
     * @param minOccupancy Filter by minimum occupancy
     * @param maxOccupancy Filter by maximum occupancy
     * @param isActive Filter by active status
     * @param keyword Search keyword
     * @param pageable Pagination parameters
     * @return ResponseEntity with ApiResponse containing paginated room standards
     */
    public ResponseEntity<ApiResponse<?>> getAllAccommodationRoomStandards(
        String accommodationId,
        String name,
        String viewType,
        String floorLevel,
        Integer minOccupancy,
        Integer maxOccupancy,
        Boolean isActive,
        String keyword,
        String sortBy,
        String sortDirection,
        Pageable pageable
    ) {
        log.info("Fetching all accommodation room standards with filters");

        try {
            // Validate sort field
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            // Build specification
            Long decodedAccommodationId = null;
            if (accommodationId != null && !accommodationId.isEmpty()) {
                try {
                    decodedAccommodationId = idObfuscator.decodeId(accommodationId);
                } catch (Exception e) {
                    log.warn("Failed to decode accommodation ID: {}", accommodationId, e);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid accommodation ID", "INVALID_ACCOMMODATION_ID")
                    );
                }
            }
            Specification<AccommodationRoomStandard> spec = buildSpec(decodedAccommodationId, name, viewType, floorLevel, minOccupancy, maxOccupancy, isActive, keyword);

            // Fetch paginated results
            Page<AccommodationRoomStandard> roomStandardsPage = roomStandardRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<AccommodationRoomStandardDTO> dtos = roomStandardsPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Build response map
            Map<String, Object> response = new HashMap<>();
            response.put("roomStandards", dtos);
            response.put("currentPage", roomStandardsPage.getNumber());
            response.put("totalItems", roomStandardsPage.getTotalElements());
            response.put("totalPages", roomStandardsPage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
            // counters share the rows' Specification, so cards and table agree
            response.put("stats", computeStats(spec));

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Room standards retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching all accommodation room standards", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch room standards",
                    "ROOM_STANDARDS_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all room standards for a specific accommodation
     * Accommodation ID is required
     *
     * @param accommodationId Required accommodation ID
     * @param name Filter by name
     * @param viewType Filter by view type
     * @param floorLevel Filter by floor level
     * @param minOccupancy Filter by minimum occupancy
     * @param maxOccupancy Filter by maximum occupancy
     * @param isActive Filter by active status
     * @param keyword Search keyword
     * @param pageable Pagination parameters
     * @return ResponseEntity with ApiResponse containing paginated room standards
     */
    public ResponseEntity<ApiResponse<?>> getAllAccommodationsRoomStandards(
        @NotBlank(message = "Accommodation ID is required") String accommodationId,
        String name,
        String viewType,
        String floorLevel,
        Integer minOccupancy,
        Integer maxOccupancy,
        Boolean isActive,
        String keyword,
        String sortBy,
        String sortDirection,
        Pageable pageable
    ) {
        log.info("Fetching room standards for accommodation: {}", accommodationId);

        try {
            // Validate sort field
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            // Decode accommodation ID
            Long accId;
            try {
                accId = idObfuscator.decodeId(accommodationId);
            } catch (Exception e) {
                log.warn("Failed to decode accommodation ID: {}", accommodationId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid accommodation ID",
                        "INVALID_ACCOMMODATION_ID"
                    )
                );
            }

            // Build specification starting with accommodation ID
            Specification<AccommodationRoomStandard> spec = AccommodationRoomStandardSpecification.hasAccommodationId(accId);

            // Apply other filters
            if (name != null && !name.isEmpty()) {
                spec = spec.and(AccommodationRoomStandardSpecification.hasName(name));
            }
            if (viewType != null && !viewType.isEmpty()) {
                spec = spec.and(AccommodationRoomStandardSpecification.hasViewType(viewType));
            }
            if (floorLevel != null && !floorLevel.isEmpty()) {
                spec = spec.and(AccommodationRoomStandardSpecification.hasFloorLevel(floorLevel));
            }
            if (minOccupancy != null) {
                spec = spec.and(AccommodationRoomStandardSpecification.hasMinOccupancy(minOccupancy));
            }
            if (maxOccupancy != null) {
                spec = spec.and(AccommodationRoomStandardSpecification.hasMaxOccupancy(maxOccupancy));
            }
            if (isActive != null) {
                spec = spec.and(AccommodationRoomStandardSpecification.isActive(isActive));
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(AccommodationRoomStandardSpecification.searchKeyword(keyword));
            }

            // Fetch paginated results
            Page<AccommodationRoomStandard> roomStandardsPage = roomStandardRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<AccommodationRoomStandardDTO> dtos = roomStandardsPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Build response map
            Map<String, Object> response = new HashMap<>();
            response.put("roomStandards", dtos);
            response.put("currentPage", roomStandardsPage.getNumber());
            response.put("totalItems", roomStandardsPage.getTotalElements());
            response.put("totalPages", roomStandardsPage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Room standards retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching accommodation room standards", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch room standards",
                    "ROOM_STANDARDS_FETCH_FAILED"
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
     * Convert AccommodationRoomStandard entity to DTO
     */
    private AccommodationRoomStandardDTO convertToDTO(AccommodationRoomStandard roomStandard) {
        return AccommodationRoomStandardDTO.builder()
            .id(idObfuscator.encodeId(roomStandard.getId()))
            .accommodationId(idObfuscator.encodeId(roomStandard.getAccommodation().getId()))
            .accommodationName(roomStandard.getAccommodation().getName())
            .name(roomStandard.getName())
            .description(roomStandard.getDescription())
            .maxOccupancy(roomStandard.getMaxOccupancy())
            .amenities(roomStandard.getAmenities())
            .viewType(roomStandard.getViewType())
            .floorLevel(roomStandard.getFloorLevel())
            .isActive(roomStandard.getIsActive())
            .createdAt(roomStandard.getCreatedAt())
            .updatedAt(roomStandard.getUpdatedAt())
            .build();
    }

    /**
     * Get unique room standards based on name
     * Returns one room standard per unique name, sorted alphabetically
     * This is useful for dropdowns where users select existing room standard names
     *
     * @return ResponseEntity with ApiResponse containing list of unique room standards
     */
    public ResponseEntity<ApiResponse<?>> getUniqueRoomStandards() {
        log.info("Fetching unique room standards by name");

        try {
            // Fetch unique room standards from repository
            List<AccommodationRoomStandard> uniqueRoomStandards = roomStandardRepository.findUniqueRoomStandardsByName();

            // Convert to DTOs
            List<AccommodationRoomStandardDTO> dtos = uniqueRoomStandards.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Unique room standards retrieved successfully",
                    dtos
                )
            );

        } catch (Exception e) {
            log.error("Error fetching unique room standards", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch unique room standards",
                    "UNIQUE_ROOM_STANDARDS_FETCH_FAILED"
                )
            );
        }
    }

    /** Dashboard counters built from the SAME Specification as the rows. */
    private java.util.Map<String, Object> computeStats(
        org.springframework.data.jpa.domain.Specification<AccommodationRoomStandard> base
    ) {
        return listStats.of(AccommodationRoomStandard.class, base)
            .total()
            .count("active", AccommodationRoomStandardSpecification.isActive(true))
            .complement("inactive", "active")
            .recency(AccommodationRoomStandardSpecification::createdAfter)
            .build();
    }

    /**
     * The ONE place a AccommodationRoomStandard filter is expressed.
     *
     * The rows, the stat counters and prev/next paging all build from this, so a
     * card can never disagree with the table and the arrows can never walk a
     * different set from the one on screen.
     */
    private Specification<AccommodationRoomStandard> buildSpec(
        Long accommodationId,
        String name,
        String viewType,
        String floorLevel,
        Integer minOccupancy,
        Integer maxOccupancy,
        Boolean isActive,
        String keyword
    ) {
        Specification<AccommodationRoomStandard> spec = Specification.unrestricted();
        if (accommodationId != null) {
            spec = spec.and(AccommodationRoomStandardSpecification.hasAccommodationId(accommodationId));
        }
    if (name != null && !name.isEmpty()) {
    spec = spec.and(AccommodationRoomStandardSpecification.hasName(name));
    }
    if (viewType != null && !viewType.isEmpty()) {
    spec = spec.and(AccommodationRoomStandardSpecification.hasViewType(viewType));
    }
    if (floorLevel != null && !floorLevel.isEmpty()) {
    spec = spec.and(AccommodationRoomStandardSpecification.hasFloorLevel(floorLevel));
    }
    if (minOccupancy != null) {
    spec = spec.and(AccommodationRoomStandardSpecification.hasMinOccupancy(minOccupancy));
    }
    if (maxOccupancy != null) {
    spec = spec.and(AccommodationRoomStandardSpecification.hasMaxOccupancy(maxOccupancy));
    }
    if (isActive != null) {
    spec = spec.and(AccommodationRoomStandardSpecification.isActive(isActive));
    }
    if (keyword != null && !keyword.isEmpty()) {
    spec = spec.and(AccommodationRoomStandardSpecification.searchKeyword(keyword));
    }
        return spec;
    }

    /** Decodes an obfuscated id, or null when absent or unreadable. */
    private Long decodeOrNull(String obfuscated) {
        if (obfuscated == null || obfuscated.isBlank()) return null;
        try {
            return idObfuscator.decodeId(obfuscated);
        } catch (Exception e) {
            log.warn("Unreadable id in filter: {}", obfuscated);
            return null;
        }
    }
}
