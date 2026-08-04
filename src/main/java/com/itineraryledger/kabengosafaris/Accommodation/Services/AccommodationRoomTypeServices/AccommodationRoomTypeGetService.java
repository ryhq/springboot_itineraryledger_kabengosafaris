package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRoomTypeServices;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRoomTypeDTOs.AccommodationRoomTypeDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRoomTypeRepository;
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
 * AccommodationRoomTypeGetService - Service for retrieving accommodation room types
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class AccommodationRoomTypeGetService {

    private final AccommodationRoomTypeRepository roomTypeRepository;

    // dashboard counters for the CURRENT filter set (see CLAUDE.md)

    @org.springframework.beans.factory.annotation.Autowired

    private com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "name", "maxOccupancy", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public AccommodationRoomTypeGetService(
        AccommodationRoomTypeRepository roomTypeRepository,
        IdObfuscator idObfuscator
    ) {
        this.roomTypeRepository = roomTypeRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get a single accommodation room type by ID
     *
     * @param idObfuscated The obfuscated room type ID
     * @return ResponseEntity with ApiResponse containing the room type
     */
    public ResponseEntity<ApiResponse<?>> getAccommodationRoomTypeById(String idObfuscated, String scopeParentId) {
        log.info("Fetching accommodation room type by ID: {}", idObfuscated);

        try {
            // Decode room type ID
            Long roomTypeId;
            try {
                roomTypeId = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode room type ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid room type ID",
                        "INVALID_ROOM_TYPE_ID"
                    )
                );
            }

            // Find room type
            AccommodationRoomType roomType = roomTypeRepository.findById(roomTypeId).orElse(null);
            if (roomType == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Room type not found",
                        "ROOM_TYPE_NOT_FOUND"
                    )
                );
            }

            // Convert to DTO
            AccommodationRoomTypeDTO roomTypeDTO = convertToDTO(roomType);

            // Decode optional scope parent ID for scoped navigation
            Long decodedParentId = null;
            if (scopeParentId != null && !scopeParentId.isEmpty()) {
                try {
                    decodedParentId = idObfuscator.decodeId(scopeParentId);
                } catch (Exception ex) {
                    log.warn("Invalid scopeParentId: {}, falling back to global navigation", scopeParentId);
                }
            }

            // Circular navigation (scoped if parent provided, global otherwise)
            Long nextId, previousId;
            if (decodedParentId != null) {
                nextId = roomTypeRepository.findNextIdByParent(roomTypeId, decodedParentId).orElse(null);
                previousId = roomTypeRepository.findPreviousIdByParent(roomTypeId, decodedParentId).orElse(null);
                if (nextId == null) nextId = roomTypeRepository.findFirstIdByParent(decodedParentId).orElse(null);
                if (previousId == null) previousId = roomTypeRepository.findLastIdByParent(decodedParentId).orElse(null);
            } else {
                nextId = roomTypeRepository.findNextId(roomTypeId).orElse(null);
                previousId = roomTypeRepository.findPreviousId(roomTypeId).orElse(null);
                if (nextId == null) nextId = roomTypeRepository.findFirstId().orElse(null);
                if (previousId == null) previousId = roomTypeRepository.findLastId().orElse(null);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("roomType", roomTypeDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("scopeParentId", scopeParentId);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Room type retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching accommodation room type by ID", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch room type",
                    "ROOM_TYPE_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all accommodation room types with optional filters
     * Accommodation ID is optional
     *
     * @param accommodationId Optional accommodation ID filter
     * @param name Filter by name
     * @param minOccupancy Filter by minimum occupancy
     * @param maxOccupancy Filter by maximum occupancy
     * @param isActive Filter by active status
     * @param keyword Search keyword
     * @param pageable Pagination parameters
     * @return ResponseEntity with ApiResponse containing paginated room types
     */
    public ResponseEntity<ApiResponse<?>> getAllAccommodationRoomTypes(
        String accommodationId,
        String name,
        Integer minOccupancy,
        Integer maxOccupancy,
        Boolean isActive,
        String keyword,
        String sortBy,
        String sortDirection,
        Pageable pageable
    ) {
        log.info("Fetching all accommodation room types with filters");

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
            Specification<AccommodationRoomType> spec = Specification.unrestricted();

            // Filter by accommodation ID if provided
            if (accommodationId != null && !accommodationId.isEmpty()) {
                try {
                    Long accId = idObfuscator.decodeId(accommodationId);
                    spec = spec.and(AccommodationRoomTypeSpecification.hasAccommodationId(accId));
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
            }

            // Apply other filters
            if (name != null && !name.isEmpty()) {
                spec = spec.and(AccommodationRoomTypeSpecification.hasName(name));
            }
            if (minOccupancy != null) {
                spec = spec.and(AccommodationRoomTypeSpecification.hasMinOccupancy(minOccupancy));
            }
            if (maxOccupancy != null) {
                spec = spec.and(AccommodationRoomTypeSpecification.hasMaxOccupancy(maxOccupancy));
            }
            if (isActive != null) {
                spec = spec.and(AccommodationRoomTypeSpecification.isActive(isActive));
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(AccommodationRoomTypeSpecification.searchKeyword(keyword));
            }

            // Fetch paginated results
            Page<AccommodationRoomType> roomTypesPage = roomTypeRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<AccommodationRoomTypeDTO> dtos = roomTypesPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Build response map
            Map<String, Object> response = new HashMap<>();
            response.put("roomTypes", dtos);
            response.put("currentPage", roomTypesPage.getNumber());
            response.put("totalItems", roomTypesPage.getTotalElements());
            response.put("totalPages", roomTypesPage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
            // counters share the rows' Specification, so cards and table agree
            response.put("stats", computeStats(spec));

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Room types retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching all accommodation room types", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch room types",
                    "ROOM_TYPES_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all room types for a specific accommodation
     * Accommodation ID is required
     *
     * @param accommodationId Required accommodation ID
     * @param name Filter by name
     * @param minOccupancy Filter by minimum occupancy
     * @param maxOccupancy Filter by maximum occupancy
     * @param isActive Filter by active status
     * @param keyword Search keyword
     * @param pageable Pagination parameters
     * @return ResponseEntity with ApiResponse containing paginated room types
     */
    public ResponseEntity<ApiResponse<?>> getAllAccommodationsRoomTypes(
        @NotBlank(message = "Accommodation ID is required") String accommodationId,
        String name,
        Integer minOccupancy,
        Integer maxOccupancy,
        Boolean isActive,
        String keyword,
        String sortBy,
        String sortDirection,
        Pageable pageable
    ) {
        log.info("Fetching room types for accommodation: {}", accommodationId);

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
            Specification<AccommodationRoomType> spec = AccommodationRoomTypeSpecification.hasAccommodationId(accId);

            // Apply other filters
            if (name != null && !name.isEmpty()) {
                spec = spec.and(AccommodationRoomTypeSpecification.hasName(name));
            }
            if (minOccupancy != null) {
                spec = spec.and(AccommodationRoomTypeSpecification.hasMinOccupancy(minOccupancy));
            }
            if (maxOccupancy != null) {
                spec = spec.and(AccommodationRoomTypeSpecification.hasMaxOccupancy(maxOccupancy));
            }
            if (isActive != null) {
                spec = spec.and(AccommodationRoomTypeSpecification.isActive(isActive));
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(AccommodationRoomTypeSpecification.searchKeyword(keyword));
            }

            // Fetch paginated results
            Page<AccommodationRoomType> roomTypesPage = roomTypeRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<AccommodationRoomTypeDTO> dtos = roomTypesPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Build response map
            Map<String, Object> response = new HashMap<>();
            response.put("roomTypes", dtos);
            response.put("currentPage", roomTypesPage.getNumber());
            response.put("totalItems", roomTypesPage.getTotalElements());
            response.put("totalPages", roomTypesPage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Room types retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching accommodation room types", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch room types",
                    "ROOM_TYPES_FETCH_FAILED"
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
     * Convert AccommodationRoomType entity to DTO
     */
    private AccommodationRoomTypeDTO convertToDTO(AccommodationRoomType roomType) {
        return AccommodationRoomTypeDTO.builder()
            .id(idObfuscator.encodeId(roomType.getId()))
            .accommodationId(idObfuscator.encodeId(roomType.getAccommodation().getId()))
            .accommodationName(roomType.getAccommodation().getName())
            .name(roomType.getName())
            .bedConfiguration(roomType.getBedConfiguration())
            .maxOccupancy(roomType.getMaxOccupancy())
            .minOccupancy(roomType.getMinOccupancy())
            .description(roomType.getDescription())
            .isActive(roomType.getIsActive())
            .createdAt(roomType.getCreatedAt())
            .updatedAt(roomType.getUpdatedAt())
            .build();
    }

    /**
     * Get unique room types based on name
     * Returns one room type per unique name, sorted alphabetically
     * This is useful for dropdowns where users select existing room type names
     *
     * @return ResponseEntity with ApiResponse containing list of unique room types
     */
    public ResponseEntity<ApiResponse<?>> getUniqueRoomTypes() {
        log.info("Fetching unique room types by name");

        try {
            // Fetch unique room types from repository
            List<AccommodationRoomType> uniqueRoomTypes = roomTypeRepository.findUniqueRoomTypesByName();

            // Convert to DTOs
            List<AccommodationRoomTypeDTO> dtos = uniqueRoomTypes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Unique room types retrieved successfully",
                    dtos
                )
            );

        } catch (Exception e) {
            log.error("Error fetching unique room types", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch unique room types",
                    "UNIQUE_ROOM_TYPES_FETCH_FAILED"
                )
            );
        }
    }

    /** Dashboard counters built from the SAME Specification as the rows. */
    private java.util.Map<String, Object> computeStats(
        org.springframework.data.jpa.domain.Specification<AccommodationRoomType> base
    ) {
        return listStats.of(AccommodationRoomType.class, base)
            .total()
            .count("active", AccommodationRoomTypeSpecification.isActive(true))
            .complement("inactive", "active")
            .recency(AccommodationRoomTypeSpecification::createdAfter)
            .build();
    }
}
