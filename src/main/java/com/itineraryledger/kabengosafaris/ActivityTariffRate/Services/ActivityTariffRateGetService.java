package com.itineraryledger.kabengosafaris.ActivityTariffRate.Services;

import com.itineraryledger.kabengosafaris.ActivityTariffRate.ActivityTariffRate;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.DTOs.ActivityTariffRateDTO;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.DTOs.UpdateActivityTariffRateDTO;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.Repositories.ActivityTariffRateRepository;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.Specifications.ActivityTariffRateSpecification;
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
 * ActivityTariffRateGetService - Service for reading and updating activity tariff rates
 *
 * Handles:
 * - Get rate by ID
 * - Get all rates with filtering and pagination
 * - Update existing rates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityTariffRateGetService {

    private final ActivityTariffRateRepository rateRepository;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "rackRate", "stoRate", "currency", "isActive", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    /**
     * Get rate by ID
     */
    public ResponseEntity<ApiResponse<?>> getRateById(String idObfuscated) {
        log.info("Fetching activity rate by ID: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            if (id == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid rate ID", "INVALID_ID")
                );
            }

            Optional<ActivityTariffRate> rateOpt = rateRepository.findById(id);
            if (rateOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Rate not found", "RATE_NOT_FOUND")
                );
            }

            ActivityTariffRateDTO rateDTO = convertToDTO(rateOpt.get());

            // Circular navigation
            Long nextId = rateRepository.findNextId(id).orElse(null);
            Long previousId = rateRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = rateRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = rateRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("rate", rateDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok(ApiResponse.success(200, "Rate retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error fetching activity rate", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch rate: " + e.getMessage(), "FETCH_FAILED")
            );
        }
    }

    /**
     * Get all rates with filtering and pagination
     */
    public ResponseEntity<ApiResponse<?>> getAllRates(
        String activityIdObfuscated,
        String parkIdObfuscated,
        Boolean globalOnly,
        String seasonIdObfuscated,
        String nationCategoryIdObfuscated,
        String ageCategoryIdObfuscated,
        Boolean isActive,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching activity rates with filters");

        try {
            // Build specification
            Specification<ActivityTariffRate> spec = Specification.unrestricted();

            if (activityIdObfuscated != null) {
                try {
                    Long activityId = idObfuscator.decodeId(activityIdObfuscated);
                    if (activityId != null) {
                        spec = spec.and(ActivityTariffRateSpecification.byActivityId(activityId));
                    }
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid activity ID format", "INVALID_ACTIVITY_ID")
                    );
                }
            }
            if (globalOnly != null && globalOnly) {
                spec = spec.and(ActivityTariffRateSpecification.globalRatesOnly());
            } else if (parkIdObfuscated != null) {
                try {
                    Long parkId = idObfuscator.decodeId(parkIdObfuscated);
                    if (parkId != null) {
                        spec = spec.and(ActivityTariffRateSpecification.byParkId(parkId));
                    }
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid park ID format", "INVALID_PARK_ID")
                    );
                }
            }
            if (seasonIdObfuscated != null) {
                try {
                    Long seasonId = idObfuscator.decodeId(seasonIdObfuscated);
                    if (seasonId != null) {
                        spec = spec.and(ActivityTariffRateSpecification.bySeasonId(seasonId));
                    }
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid season ID format", "INVALID_SEASON_ID")
                    );
                }
            }
            if (nationCategoryIdObfuscated != null) {
                try {
                    Long nationCategoryId = idObfuscator.decodeId(nationCategoryIdObfuscated);
                    if (nationCategoryId != null) {
                        spec = spec.and(ActivityTariffRateSpecification.byNationCategoryId(nationCategoryId));
                    }
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid nation category ID format", "INVALID_NATION_CATEGORY_ID")
                    );
                }
            }
            if (ageCategoryIdObfuscated != null) {
                try {
                    Long ageCategoryId = idObfuscator.decodeId(ageCategoryIdObfuscated);
                    if (ageCategoryId != null) {
                        spec = spec.and(ActivityTariffRateSpecification.byAgeCategoryId(ageCategoryId));
                    }
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid age category ID format", "INVALID_AGE_CATEGORY_ID")
                    );
                }
            }
            if (isActive != null) {
                spec = spec.and(ActivityTariffRateSpecification.isActive(isActive));
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
            Page<ActivityTariffRate> ratePage = rateRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<ActivityTariffRateDTO> rateDTOs = ratePage.getContent().stream()
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
            response.put("currentSortDir", sortDirection != null ? sortDirection : "desc");

            return ResponseEntity.ok(ApiResponse.success(200, "Rates retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error fetching activity rates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch rates: " + e.getMessage(), "FETCH_FAILED")
            );
        }
    }

    /**
     * Update an existing rate
     */
    @Transactional
    @AuditLogAnnotation(action = "UPDATE_ACTIVITY_TARIFF_RATE", description = "Updating activity tariff rate", entityType = "ActivityTariffRate")
    public ResponseEntity<ApiResponse<?>> updateRate(String idObfuscated, UpdateActivityTariffRateDTO dto) {
        log.info("Updating activity rate: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            if (id == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid rate ID", "INVALID_ID")
                );
            }

            Optional<ActivityTariffRate> rateOpt = rateRepository.findById(id);
            if (rateOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Rate not found", "RATE_NOT_FOUND")
                );
            }

            ActivityTariffRate rate = rateOpt.get();

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

            rate = rateRepository.save(rate);
            log.info("Updated activity rate: {}", rate.getId());

            return ResponseEntity.ok(ApiResponse.success(200, "Rate updated successfully", convertToDTO(rate)));

        } catch (Exception e) {
            log.error("Error updating activity rate", e);
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
    private ActivityTariffRateDTO convertToDTO(ActivityTariffRate rate) {
        return ActivityTariffRateDTO.builder()
            .id(idObfuscator.encodeId(rate.getId()))
            .activityId(idObfuscator.encodeId(rate.getActivity().getId()))
            .activityName(rate.getActivity().getName())
            .activityChargingBasis(rate.getActivity().getChargingBasis() != null
                ? rate.getActivity().getChargingBasis().getDisplayName() : null)
            .parkId(rate.getPark() != null ? idObfuscator.encodeId(rate.getPark().getId()) : null)
            .parkName(rate.getPark() != null ? rate.getPark().getName() : null)
            .isGlobalRate(rate.isGlobalRate())
            .seasonId(idObfuscator.encodeId(rate.getSeason().getId()))
            .seasonName(rate.getSeason().getName())
            .seasonType(rate.getSeason().getSeasonType() != null
                ? rate.getSeason().getSeasonType().getDisplayName() : null)
            .nationCategoryId(idObfuscator.encodeId(rate.getNationCategory().getId()))
            .nationCategoryName(rate.getNationCategory().getName())
            .ageCategoryId(rate.getAgeCategory() != null
                ? idObfuscator.encodeId(rate.getAgeCategory().getId()) : null)
            .ageCategoryName(rate.getAgeCategory() != null
                ? rate.getAgeCategory().getName() : null)
            .ageCategoryAgeRange(rate.getAgeCategory() != null
                ? rate.getAgeCategory().getAgeRangeDisplay() : null)
            .rackRate(rate.getRackRate())
            .stoRate(rate.getStoRate())
            .currency(rate.getCurrency())
            .profitAmount(rate.getProfitAmount())
            .profitPercentage(rate.getProfitPercentage())
            .notes(rate.getNotes())
            .isActive(rate.getIsActive())
            .createdAt(rate.getCreatedAt())
            .updatedAt(rate.getUpdatedAt())
            .build();
    }
}
