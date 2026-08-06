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

    // filter-aware prev/next + the N of M readout, and declarative counters
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.ListStats listStats;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "rackRate", "stoRate", "currency", "isActive", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    /**
     * Get rate by ID
     */
    public ResponseEntity<ApiResponse<?>> getRateById(String idObfuscated) {
        return getRateById(idObfuscated, null, null, null, null, null, null, null, null);
    }

    /**
     * One rate, plus where it sits in the set the caller was looking at.
     *
     * The list's filters and sort arrive here because paging out of a filtered list
     * must stay inside that filter, and N of M must count the same set.
     */
    public ResponseEntity<ApiResponse<?>> getRateById(
        String idObfuscated,
        String activityIdObfuscated,
        String parkIdObfuscated,
        String seasonIdObfuscated,
        String nationCategoryIdObfuscated,
        String ageCategoryIdObfuscated,
        Boolean isActive,
        Boolean globalOnly,
        String sortBy
    ) {
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
            /*
             * Prev/next walks the SAME filtered, sorted set the list showed; the old
             * pair walked raw id order over every rate regardless of the filter.
             */
            Specification<ActivityTariffRate> navSpec = buildSpec(decodeOrNull(activityIdObfuscated), decodeOrNull(parkIdObfuscated), decodeOrNull(seasonIdObfuscated), decodeOrNull(nationCategoryIdObfuscated), decodeOrNull(ageCategoryIdObfuscated), isActive, globalOnly);
            String navSortBy = validateSortField(sortBy);
            if (navSortBy == null) navSortBy = DEFAULT_SORT_FIELD;
            java.util.Map<String, Object> nav = recordNavigation.navigate(
                ActivityTariffRate.class, navSpec, navSortBy, false, id
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("rate", rateDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

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
        Boolean includeStats,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching activity rates with filters");

        try {
            Long decoded_activityId = decodeOrNull(activityIdObfuscated);
            Long decoded_parkId = decodeOrNull(parkIdObfuscated);
            Long decoded_seasonId = decodeOrNull(seasonIdObfuscated);
            Long decoded_nationCategoryId = decodeOrNull(nationCategoryIdObfuscated);
            Long decoded_ageCategoryId = decodeOrNull(ageCategoryIdObfuscated);
            Specification<ActivityTariffRate> spec = buildSpec(decoded_activityId, decoded_parkId, decoded_seasonId, decoded_nationCategoryId, decoded_ageCategoryId, isActive, globalOnly);

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
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
            // counters share the rows' Specification, so cards and table agree
            if (includeStats == null || includeStats) {
                response.put("stats", computeStats(spec));
            }

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

    /**
     * The ONE place a rate filter is expressed — rows, counters and prev/next all
     * build from it, so a card can never disagree with the table and the arrows can
     * never walk a different set from the one on screen.
     */
    private Specification<ActivityTariffRate> buildSpec(
        Long activityId,
        Long parkId,
        Long seasonId,
        Long nationCategoryId,
        Long ageCategoryId,
        Boolean isActive,
        Boolean globalOnly
    ) {
        Specification<ActivityTariffRate> spec = Specification.unrestricted();
        if (activityId != null) spec = spec.and(ActivityTariffRateSpecification.byActivityId(activityId));
        if (parkId != null) spec = spec.and(ActivityTariffRateSpecification.byParkId(parkId));
        if (seasonId != null) spec = spec.and(ActivityTariffRateSpecification.bySeasonId(seasonId));
        if (nationCategoryId != null) spec = spec.and(ActivityTariffRateSpecification.byNationCategoryId(nationCategoryId));
        if (ageCategoryId != null) spec = spec.and(ActivityTariffRateSpecification.byAgeCategoryId(ageCategoryId));
        if (isActive != null) spec = spec.and(ActivityTariffRateSpecification.isActive(isActive));
        if (globalOnly != null && globalOnly) spec = spec.and(ActivityTariffRateSpecification.globalRatesOnly());
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

    /** Counters built from the SAME Specification as the rows. */
    private java.util.Map<String, Object> computeStats(Specification<ActivityTariffRate> base) {
        return listStats.of(ActivityTariffRate.class, base)
            .total()
            .count("active", ActivityTariffRateSpecification.isActive(true))
            .complement("inactive", "active")
            .count("hasSto", ActivityTariffRateSpecification.hasStoRate())
            .complement("missingSto", "hasSto")
            .count("global", ActivityTariffRateSpecification.globalRatesOnly())
            .complement("parkSpecific", "global")
            .count("withAgeBand", ActivityTariffRateSpecification.hasAgeCategory())
            .recency(ActivityTariffRateSpecification::createdAfter)
            .build();
    }
}
