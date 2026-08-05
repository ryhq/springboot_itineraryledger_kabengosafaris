package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRateServices;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRateDTOs.AccommodationRateDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRateDTOs.UpdateAccommodationRateDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRate;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRateRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AccommodationRateGetService - Service for reading and updating accommodation rates
 *
 * Handles:
 * - Get rate by ID
 * - Get all rates with filtering and pagination
 * - Update existing rates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccommodationRateGetService {

    private final AccommodationRateRepository rateRepository;

    // filter-aware prev/next + the N of M readout

    @org.springframework.beans.factory.annotation.Autowired

    private com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    // dashboard counters for the CURRENT filter set (see CLAUDE.md)

    @org.springframework.beans.factory.annotation.Autowired

    private com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "rackRate", "stoRate", "currency", "isActive", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    /**
     * Get rate by ID
     */
    public ResponseEntity<ApiResponse<?>> getRateById(String idObfuscated, String scopeParentId) {
        log.info("Fetching accommodation rate by ID: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            if (id == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid rate ID", "INVALID_ID")
                );
            }

            Optional<AccommodationRate> rateOpt = rateRepository.findById(id);
            if (rateOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Rate not found", "RATE_NOT_FOUND")
                );
            }

            AccommodationRateDTO rateDTO = convertToDTO(rateOpt.get());

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
            org.springframework.data.jpa.domain.Specification<AccommodationRate> navSpec =
                decodedParentId != null
                    ? AccommodationRateSpecification.byAccommodationId(decodedParentId)
                    : org.springframework.data.jpa.domain.Specification.unrestricted();
            java.util.Map<String, Object> nav = recordNavigation.navigate(
                AccommodationRate.class, navSpec, "createdAt", false, id
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("rate", rateDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));
            response.put("scopeParentId", scopeParentId);

            return ResponseEntity.ok(ApiResponse.success(200, "Rate retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error fetching accommodation rate", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch rate: " + e.getMessage(), "FETCH_FAILED")
            );
        }
    }

    /**
     * Get all rates with filtering and pagination
     *
     * @param isPerPerson Filter by rate charging model: true = Per Person Sharing (PPS), false = Per Room
     */
    public ResponseEntity<ApiResponse<?>> getAllRates(
        String accommodationIdObfuscated,
        String seasonIdObfuscated,
        String roomTypeIdObfuscated,
        String roomStandardIdObfuscated,
        String boardTypeIdObfuscated,
        Boolean isActive,
        Boolean isPerPerson,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching accommodation rates with filters");

        try {
            // Build specification
            Specification<AccommodationRate> spec = Specification.unrestricted();

            if (accommodationIdObfuscated != null) {
                try {
                    Long accommodationId = idObfuscator.decodeId(accommodationIdObfuscated);
                    if (accommodationId != null) {
                        spec = spec.and(AccommodationRateSpecification.byAccommodationId(accommodationId));
                    }
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid accommodation ID format", "INVALID_ACCOMMODATION_ID")
                    );
                }
            }

            if (seasonIdObfuscated != null) {
                try {
                    Long seasonId = idObfuscator.decodeId(seasonIdObfuscated);
                    if (seasonId != null) {
                        spec = spec.and(AccommodationRateSpecification.bySeasonId(seasonId));
                    }
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid season ID format", "INVALID_SEASON_ID")
                    );
                }
            }

            if (roomTypeIdObfuscated != null) {
                try {
                    Long roomTypeId = idObfuscator.decodeId(roomTypeIdObfuscated);
                    if (roomTypeId != null) {
                        spec = spec.and(AccommodationRateSpecification.byRoomTypeId(roomTypeId));
                    }
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid room type ID format", "INVALID_ROOM_TYPE_ID")
                    );
                }
            }

            if (roomStandardIdObfuscated != null) {
                try {
                    Long roomStandardId = idObfuscator.decodeId(roomStandardIdObfuscated);
                    if (roomStandardId != null) {
                        spec = spec.and(AccommodationRateSpecification.byRoomStandardId(roomStandardId));
                    }
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid room standard ID format", "INVALID_ROOM_STANDARD_ID")
                    );
                }
            }

            if (boardTypeIdObfuscated != null) {
                try {
                    Long boardTypeId = idObfuscator.decodeId(boardTypeIdObfuscated);
                    if (boardTypeId != null) {
                        spec = spec.and(AccommodationRateSpecification.byBoardTypeId(boardTypeId));
                    }
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid board type ID format", "INVALID_BOARD_TYPE_ID")
                    );
                }
            }

            if (isActive != null) {
                spec = spec.and(AccommodationRateSpecification.isActive(isActive));
            }

            if (isPerPerson != null) {
                spec = spec.and(AccommodationRateSpecification.isPerPerson(isPerPerson));
            }

            // Sorting with validation
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 10,
                Sort.by(direction, validatedSortBy)
            );

            // Execute query
            Page<AccommodationRate> ratePage = rateRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<AccommodationRateDTO> rateDTOs = ratePage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("rates", rateDTOs);
            response.put("currentPage", ratePage.getNumber());
            response.put("totalItems", ratePage.getTotalElements());
            response.put("totalPages", ratePage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
            // counters share the rows' Specification, so cards and table agree
            response.put("stats", computeStats(spec));

            return ResponseEntity.ok(ApiResponse.success(200, "Rates retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error fetching accommodation rates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch rates: " + e.getMessage(), "FETCH_FAILED")
            );
        }
    }

    /**
     * Update an existing rate
     */
    @Transactional
    @AuditLogAnnotation(action = "UPDATE_ACCOMMODATION_RATE", description = "Updating accommodation rate", entityType = "AccommodationRate")
    public ResponseEntity<ApiResponse<?>> updateRate(String idObfuscated, UpdateAccommodationRateDTO dto) {
        log.info("Updating accommodation rate: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            if (id == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid rate ID", "INVALID_ID")
                );
            }

            Optional<AccommodationRate> rateOpt = rateRepository.findById(id);
            if (rateOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Rate not found", "RATE_NOT_FOUND")
                );
            }

            AccommodationRate rate = rateOpt.get();

            // Update fields
            if (dto.getRackRate() != null) {
                rate.setRackRate(dto.getRackRate());
            }
            if (dto.getClearStoRate() != null && dto.getClearStoRate()) {
                rate.setStoRate(null);
            } else if (dto.getStoRate() != null) {
                rate.setStoRate(dto.getStoRate());
            }
            if (dto.getCurrency() != null) {
                rate.setCurrency(dto.getCurrency().toUpperCase());
            }
            if (dto.getNotes() != null) {
                rate.setNotes(dto.getNotes());
            }
            if (dto.getIsActive() != null) {
                rate.setIsActive(dto.getIsActive());
            }
            if (dto.getIsPerPerson() != null) {
                rate.setIsPerPerson(dto.getIsPerPerson());
            }

            // Validate rack rate >= sto rate
            if (rate.getStoRate() != null && rate.getRackRate().compareTo(rate.getStoRate()) < 0) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Rack rate cannot be less than STO rate", "INVALID_RATE")
                );
            }

            rate = rateRepository.save(rate);
            log.info("Updated accommodation rate: {}", rate.getId());

            return ResponseEntity.ok(ApiResponse.success(200, "Rate updated successfully", convertToDTO(rate)));

        } catch (Exception e) {
            log.error("Error updating accommodation rate", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update rate: " + e.getMessage(), "UPDATE_FAILED")
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
     * Convert entity to DTO
     */
    private AccommodationRateDTO convertToDTO(AccommodationRate rate) {
        return AccommodationRateDTO.builder()
            .id(idObfuscator.encodeId(rate.getId()))
            .accommodationId(idObfuscator.encodeId(rate.getAccommodation().getId()))
            .accommodationName(rate.getAccommodation().getName())
            .seasonId(idObfuscator.encodeId(rate.getSeason().getId()))
            .seasonName(rate.getSeason().getName())
            .seasonType(rate.getSeason().getSeasonType() != null
                ? rate.getSeason().getSeasonType().getDisplayName() : null)
            .roomTypeId(idObfuscator.encodeId(rate.getRoomType().getId()))
            .roomTypeName(rate.getRoomType().getName())
            .bedConfiguration(rate.getRoomType().getBedConfiguration())
            .roomStandardId(idObfuscator.encodeId(rate.getRoomStandard().getId()))
            .roomStandardName(rate.getRoomStandard().getName())
            .boardTypeId(idObfuscator.encodeId(rate.getBoardType().getId()))
            .boardTypeName(rate.getBoardType().getName())
            .rackRate(rate.getRackRate())
            .stoRate(rate.getStoRate())
            .currency(rate.getCurrency())
            .profitAmount(rate.getProfitAmount())
            .profitPercentage(rate.getProfitPercentage())
            .isPerPerson(rate.getIsPerPerson())
            .notes(rate.getNotes())
            .isActive(rate.getIsActive())
            .createdAt(rate.getCreatedAt())
            .updatedAt(rate.getUpdatedAt())
            .build();
    }

    /** Dashboard counters built from the SAME Specification as the rows. */
    private java.util.Map<String, Object> computeStats(
        org.springframework.data.jpa.domain.Specification<AccommodationRate> base
    ) {
        return listStats.of(AccommodationRate.class, base)
            .total()
            .count("active", AccommodationRateSpecification.isActive(true))
            .complement("inactive", "active")
            .recency(AccommodationRateSpecification::createdAfter)
            .build();
    }
}
