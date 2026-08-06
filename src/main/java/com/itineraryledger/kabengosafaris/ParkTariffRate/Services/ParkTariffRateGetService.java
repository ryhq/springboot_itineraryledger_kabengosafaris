package com.itineraryledger.kabengosafaris.ParkTariffRate.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.ParkTariffRate.DTOs.ParkTariffRateDTO;
import com.itineraryledger.kabengosafaris.ParkTariffRate.DTOs.UpdateParkTariffRateDTO;
import com.itineraryledger.kabengosafaris.ParkTariffRate.ParkTariffRate;
import com.itineraryledger.kabengosafaris.ParkTariffRate.Repositories.ParkTariffRateRepository;
import com.itineraryledger.kabengosafaris.ParkTariffRate.Specifications.ParkTariffRateSpecification;
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
 * ParkTariffRateGetService - Service for reading and updating park tariff rates
 *
 * Handles:
 * - Get rate by ID
 * - Get all rates with filtering and pagination
 * - Update existing rates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParkTariffRateGetService {

    private final ParkTariffRateRepository rateRepository;
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
        String parkIdObfuscated,
        String tariffIdObfuscated,
        String seasonIdObfuscated,
        String nationCategoryIdObfuscated,
        String ageCategoryIdObfuscated,
        Boolean isActive,
        String keyword,
        String sortBy
    ) {
        log.info("Fetching park tariff rate by ID: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            if (id == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid rate ID", "INVALID_ID")
                );
            }

            Optional<ParkTariffRate> rateOpt = rateRepository.findById(id);
            if (rateOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Rate not found", "RATE_NOT_FOUND")
                );
            }

            ParkTariffRateDTO rateDTO = convertToDTO(rateOpt.get());

            // Circular navigation
            /*
             * Prev/next walks the SAME filtered, sorted set the list showed; the old
             * pair walked raw id order over every rate regardless of the filter.
             */
            Specification<ParkTariffRate> navSpec = buildSpec(decodeOrNull(parkIdObfuscated), decodeOrNull(tariffIdObfuscated), decodeOrNull(seasonIdObfuscated), decodeOrNull(nationCategoryIdObfuscated), decodeOrNull(ageCategoryIdObfuscated), isActive, keyword);
            String navSortBy = validateSortField(sortBy);
            if (navSortBy == null) navSortBy = DEFAULT_SORT_FIELD;
            java.util.Map<String, Object> nav = recordNavigation.navigate(
                ParkTariffRate.class, navSpec, navSortBy, false, id
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
            log.error("Error fetching park tariff rate", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch rate: " + e.getMessage(), "FETCH_FAILED")
            );
        }
    }

    /**
     * Get all rates with filtering and pagination
     */
    public ResponseEntity<ApiResponse<?>> getAllRates(
        String parkIdObfuscated,
        String tariffIdObfuscated,
        String seasonIdObfuscated,
        String nationCategoryIdObfuscated,
        String ageCategoryIdObfuscated,
        Boolean isActive,
        String keyword,
        Boolean includeStats,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching park tariff rates with filters");

        try {
            Long decoded_parkId = decodeOrNull(parkIdObfuscated);
            Long decoded_tariffId = decodeOrNull(tariffIdObfuscated);
            Long decoded_seasonId = decodeOrNull(seasonIdObfuscated);
            Long decoded_nationCategoryId = decodeOrNull(nationCategoryIdObfuscated);
            Long decoded_ageCategoryId = decodeOrNull(ageCategoryIdObfuscated);
            Specification<ParkTariffRate> spec = buildSpec(decoded_parkId, decoded_tariffId, decoded_seasonId, decoded_nationCategoryId, decoded_ageCategoryId, isActive, keyword);

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
            Page<ParkTariffRate> ratePage = rateRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<ParkTariffRateDTO> rateDTOs = ratePage.getContent().stream()
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
            log.error("Error fetching park tariff rates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch rates: " + e.getMessage(), "FETCH_FAILED")
            );
        }
    }

    /**
     * Update an existing rate
     */
    @Transactional
    @AuditLogAnnotation(action = "UPDATE_PARK_TARIFF_RATE", description = "Updating park tariff rate", entityType = "ParkTariffRate")
    public ResponseEntity<ApiResponse<?>> updateRate(String idObfuscated, UpdateParkTariffRateDTO dto) {
        log.info("Updating park tariff rate: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            if (id == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid rate ID", "INVALID_ID")
                );
            }

            Optional<ParkTariffRate> rateOpt = rateRepository.findById(id);
            if (rateOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Rate not found", "RATE_NOT_FOUND")
                );
            }

            ParkTariffRate rate = rateOpt.get();

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
            log.info("Updated park tariff rate: {}", rate.getId());

            return ResponseEntity.ok(ApiResponse.success(200, "Rate updated successfully", convertToDTO(rate)));

        } catch (Exception e) {
            log.error("Error updating park tariff rate", e);
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
    private ParkTariffRateDTO convertToDTO(ParkTariffRate rate) {
        return ParkTariffRateDTO.builder()
            .id(idObfuscator.encodeId(rate.getId()))
            .parkId(idObfuscator.encodeId(rate.getParkTariff().getPark().getId()))
            .parkName(rate.getParkTariff().getPark().getName())
            .tariffId(idObfuscator.encodeId(rate.getParkTariff().getTariff().getId()))
            .tariffName(rate.getParkTariff().getTariff().getName())
            .tariffChargingBasis(rate.getParkTariff().getTariff().getChargingBasis() != null
                ? rate.getParkTariff().getTariff().getChargingBasis().getDisplayName() : null)
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
    private Specification<ParkTariffRate> buildSpec(
        Long parkId,
        Long tariffId,
        Long seasonId,
        Long nationCategoryId,
        Long ageCategoryId,
        Boolean isActive,
        String keyword
    ) {
        Specification<ParkTariffRate> spec = Specification.unrestricted();
        if (parkId != null) spec = spec.and(ParkTariffRateSpecification.byParkId(parkId));
        if (tariffId != null) spec = spec.and(ParkTariffRateSpecification.byTariffId(tariffId));
        if (seasonId != null) spec = spec.and(ParkTariffRateSpecification.bySeasonId(seasonId));
        if (nationCategoryId != null) spec = spec.and(ParkTariffRateSpecification.byNationCategoryId(nationCategoryId));
        if (ageCategoryId != null) spec = spec.and(ParkTariffRateSpecification.byAgeCategoryId(ageCategoryId));
        if (isActive != null) spec = spec.and(ParkTariffRateSpecification.isActive(isActive));
        if (keyword != null && !keyword.isBlank()) spec = spec.and(ParkTariffRateSpecification.searchKeyword(keyword));

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
    private java.util.Map<String, Object> computeStats(Specification<ParkTariffRate> base) {
        return listStats.of(ParkTariffRate.class, base)
            .total()
            .count("active", ParkTariffRateSpecification.isActive(true))
            .complement("inactive", "active")
            .count("hasSto", ParkTariffRateSpecification.hasStoRate())
            .complement("missingSto", "hasSto")
            .count("globalSeason", ParkTariffRateSpecification.byGlobalSeason())
            .count("withAgeBand", ParkTariffRateSpecification.hasAgeCategory())
            .recency(ParkTariffRateSpecification::createdAfter)
            .build();
    }
}
