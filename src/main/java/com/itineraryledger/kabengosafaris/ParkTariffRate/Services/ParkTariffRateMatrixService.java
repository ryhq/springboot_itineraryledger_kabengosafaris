package com.itineraryledger.kabengosafaris.ParkTariffRate.Services;

import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariff;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariffRepository;
import com.itineraryledger.kabengosafaris.ParkTariffRate.DTOs.ParkRateMatrixResponseDTO;
import com.itineraryledger.kabengosafaris.ParkTariffRate.DTOs.ParkRateMatrixResponseDTO.*;
import com.itineraryledger.kabengosafaris.ParkTariffRate.DTOs.ParkTariffRateDTO;
import com.itineraryledger.kabengosafaris.ParkTariffRate.ParkTariffRate;
import com.itineraryledger.kabengosafaris.ParkTariffRate.Repositories.ParkTariffRateRepository;
import com.itineraryledger.kabengosafaris.ParkTariffRate.Specifications.ParkTariffRateSpecification;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.Repositories.PaxAgeCategoryRepository;
import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Repositories.PaxNationCategoryRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Season.Repositories.SeasonRepository;
import com.itineraryledger.kabengosafaris.Season.Season;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Tariff.Tariff;
import com.itineraryledger.kabengosafaris.Tariff.Repositories.TariffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ParkTariffRateMatrixService - Service for fetching rate matrix data for UI
 *
 * Provides an endpoint that returns:
 * - Park and Tariff information
 * - List of active global seasons (with exclusion support)
 * - List of active nation categories (with exclusion support)
 * - List of active age categories (only for PER_PERSON tariffs, with exclusion support)
 * - Existing rates filtered by the same exclusions
 *
 * This data is used to render a rate input grid/matrix in the frontend.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParkTariffRateMatrixService {

    private final ParkRepository parkRepository;
    private final TariffRepository tariffRepository;
    private final ParkTariffRepository parkTariffRepository;
    private final SeasonRepository seasonRepository;
    private final PaxNationCategoryRepository nationCategoryRepository;
    private final PaxAgeCategoryRepository ageCategoryRepository;
    private final ParkTariffRateRepository rateRepository;
    private final IdObfuscator idObfuscator;

    /**
     * Get rate matrix data for a park-tariff combination
     *
     * @param tariffIdObfuscated The obfuscated tariff ID (required)
     * @param parkIdObfuscated The obfuscated park ID (null to auto-select first available park)
     * @param excludeSeasonIds List of obfuscated season IDs to exclude
     * @param excludeNationCategoryIds List of obfuscated nation category IDs to exclude
     * @param excludeAgeCategoryIds List of obfuscated age category IDs to exclude (only for PER_PERSON)
     * @return ResponseEntity with ParkRateMatrixResponseDTO
     */
    public ResponseEntity<ApiResponse<?>> getRateMatrix(
        String tariffIdObfuscated,
        String parkIdObfuscated,
        List<String> excludeSeasonIds,
        List<String> excludeNationCategoryIds,
        List<String> excludeAgeCategoryIds
    ) {
        log.info("Fetching rate matrix for tariff: {}, park: {}", tariffIdObfuscated, parkIdObfuscated);

        // Validate tariff ID
        if (tariffIdObfuscated == null || tariffIdObfuscated.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Tariff ID is required", "TARIFF_ID_REQUIRED")
            );
        }

        // Decode tariff ID
        Long tariffId;
        try {
            tariffId = idObfuscator.decodeId(tariffIdObfuscated);
            if (tariffId == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid tariff ID format", "INVALID_TARIFF_ID")
                );
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid tariff ID format", "INVALID_TARIFF_ID")
            );
        }

        // Fetch tariff
        Optional<Tariff> tariffOpt = tariffRepository.findById(tariffId);
        if (tariffOpt.isEmpty()) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(404, "Tariff not found", "TARIFF_NOT_FOUND")
            );
        }
        Tariff tariff = tariffOpt.get();

        // Validate tariff has charging basis
        if (tariff.getChargingBasis() == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400,
                    "Tariff '" + tariff.getName() + "' cannot have rates because chargingBasis is not set",
                    "TARIFF_NO_CHARGING_BASIS")
            );
        }

        // Fetch all parks that have this tariff (for dropdown)
        List<Park> parksWithTariff = fetchParksWithTariff(tariffId);

        // Determine park selection
        Long parkId = null;
        Park park = null;

        if (parksWithTariff.isEmpty()) {
            // No parks have this tariff - cannot create rates
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400,
                    "Tariff '" + tariff.getName() + "' is not associated with any parks. Please assign it to a park first.",
                    "TARIFF_NO_PARKS")
            );
        } else if (parkIdObfuscated != null && !parkIdObfuscated.trim().isEmpty()) {
            // User specified a park - validate it exists and is associated with this tariff
            try {
                parkId = idObfuscator.decodeId(parkIdObfuscated);
                if (parkId != null) {
                    Optional<Park> parkOpt = parkRepository.findById(parkId);
                    if (parkOpt.isEmpty()) {
                        return ResponseEntity.status(404).body(
                            ApiResponse.error(404, "Park not found", "PARK_NOT_FOUND")
                        );
                    }
                    Park foundPark = parkOpt.get();

                    // Verify the park is associated with this tariff
                    boolean parkAssociated = parksWithTariff.stream()
                        .anyMatch(p -> p.getId().equals(foundPark.getId()));
                    if (!parkAssociated) {
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(400,
                                "Park '" + foundPark.getName() + "' is not associated with tariff '" + tariff.getName() + "'",
                                "PARK_NOT_ASSOCIATED")
                        );
                    }
                    park = foundPark;
                }
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid park ID format", "INVALID_PARK_ID")
                );
            }
        } else {
            // No parkId provided - auto-select first available park
            park = parksWithTariff.get(0);
            parkId = park.getId();
            log.info("Auto-selected first available park: {} ({})", park.getName(), park.getId());
        }

        // Decode exclusion IDs
        Set<Long> excludedSeasonIdsDecoded = decodeIds(excludeSeasonIds);
        Set<Long> excludedNationCategoryIdsDecoded = decodeIds(excludeNationCategoryIds);
        Set<Long> excludedAgeCategoryIdsDecoded = decodeIds(excludeAgeCategoryIds);

        // Determine if age categories are applicable
        boolean includesAgeCategories = tariff.getChargingBasis() == ChargingBasis.PER_PERSON;

        // Fetch active global seasons (excluding specified ones)
        List<Season> seasons = fetchActiveGlobalSeasons(excludedSeasonIdsDecoded);

        // Fetch active nation categories (excluding specified ones)
        List<PaxNationCategory> nationCategories = fetchActiveNationCategories(excludedNationCategoryIdsDecoded);

        // Fetch active age categories if applicable (excluding specified ones)
        List<PaxAgeCategory> ageCategories = new ArrayList<>();
        if (includesAgeCategories) {
            ageCategories = fetchActiveAgeCategories(excludedAgeCategoryIdsDecoded);
        }

        // Fetch existing rates with same exclusions applied
        List<ParkTariffRate> existingRates = fetchExistingRates(
            parkId, tariffId,
            excludedSeasonIdsDecoded, excludedNationCategoryIdsDecoded, excludedAgeCategoryIdsDecoded,
            includesAgeCategories
        );

        // Build response
        ParkRateMatrixResponseDTO response = buildResponse(
            park, tariff, parksWithTariff, seasons, nationCategories, ageCategories,
            existingRates, includesAgeCategories
        );

        return ResponseEntity.ok(ApiResponse.success(200, "Rate matrix retrieved successfully", response));
    }

    /**
     * Decode a list of obfuscated IDs, ignoring invalid ones
     */
    private Set<Long> decodeIds(List<String> obfuscatedIds) {
        if (obfuscatedIds == null || obfuscatedIds.isEmpty()) {
            return Set.of();
        }

        return obfuscatedIds.stream()
            .filter(id -> id != null && !id.trim().isEmpty())
            .map(id -> {
                try {
                    return idObfuscator.decodeId(id);
                } catch (Exception e) {
                    log.warn("Failed to decode ID: {}", id);
                    return null;
                }
            })
            .filter(id -> id != null)
            .collect(Collectors.toSet());
    }

    /**
     * Fetch active global seasons, excluding specified IDs
     */
    private List<Season> fetchActiveGlobalSeasons(Set<Long> excludeIds) {
        return seasonRepository.findAll().stream()
            .filter(s -> Boolean.TRUE.equals(s.getIsGlobal()))
            .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
            .filter(s -> !excludeIds.contains(s.getId()))
            .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
            .collect(Collectors.toList());
    }

    /**
     * Fetch active nation categories, excluding specified IDs
     */
    private List<PaxNationCategory> fetchActiveNationCategories(Set<Long> excludeIds) {
        return nationCategoryRepository.findAll().stream()
            .filter(nc -> Boolean.TRUE.equals(nc.getIsActive()))
            .filter(nc -> !excludeIds.contains(nc.getId()))
            .sorted((a, b) -> Integer.compare(a.getPriorityFactor(), b.getPriorityFactor()))
            .collect(Collectors.toList());
    }

    /**
     * Fetch active age categories, excluding specified IDs
     */
    private List<PaxAgeCategory> fetchActiveAgeCategories(Set<Long> excludeIds) {
        return ageCategoryRepository.findAll().stream()
            .filter(ac -> Boolean.TRUE.equals(ac.getIsActive()))
            .filter(ac -> !excludeIds.contains(ac.getId()))
            .sorted((a, b) -> Integer.compare(a.getMinAge(), b.getMinAge()))
            .collect(Collectors.toList());
    }

    /**
     * Fetch all parks where the tariff is used
     */
    private List<Park> fetchParksWithTariff(Long tariffId) {
        return parkTariffRepository.findByTariffId(tariffId).stream()
            .map(ParkTariff::getPark)
            .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
            .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
            .collect(Collectors.toList());
    }

    /**
     * Fetch existing rates for the park-tariff with exclusions applied
     */
    private List<ParkTariffRate> fetchExistingRates(
        Long parkId, Long tariffId,
        Set<Long> excludedSeasonIds, Set<Long> excludedNationCategoryIds,
        Set<Long> excludedAgeCategoryIds, boolean includesAgeCategories
    ) {
        // Build base specification
        Specification<ParkTariffRate> spec = ParkTariffRateSpecification.byParkAndTariff(parkId, tariffId);

        // Fetch all matching rates
        List<ParkTariffRate> rates = rateRepository.findAll(spec);

        // Apply exclusions in memory
        return rates.stream()
            .filter(r -> !excludedSeasonIds.contains(r.getSeason().getId()))
            .filter(r -> !excludedNationCategoryIds.contains(r.getNationCategory().getId()))
            .filter(r -> {
                if (!includesAgeCategories) {
                    return true; // No age category filtering for non-PER_PERSON
                }
                if (r.getAgeCategory() == null) {
                    return true;
                }
                return !excludedAgeCategoryIds.contains(r.getAgeCategory().getId());
            })
            .collect(Collectors.toList());
    }

    /**
     * Build the response DTO
     */
    private ParkRateMatrixResponseDTO buildResponse(
        Park park,
        Tariff tariff,
        List<Park> parksWithTariff,
        List<Season> seasons,
        List<PaxNationCategory> nationCategories,
        List<PaxAgeCategory> ageCategories,
        List<ParkTariffRate> existingRates,
        boolean includesAgeCategories
    ) {
        // Convert seasons to DTOs
        List<SeasonInfo> seasonInfos = seasons.stream()
            .map(s -> SeasonInfo.builder()
                .id(idObfuscator.encodeId(s.getId()))
                .name(s.getName())
                .seasonType(s.getSeasonType() != null ? s.getSeasonType().name() : null)
                .seasonTypeDisplayName(s.getSeasonType() != null ? s.getSeasonType().getDisplayName() : null)
                .build())
            .collect(Collectors.toList());

        // Convert nation categories to DTOs
        List<NationCategoryInfo> nationCategoryInfos = nationCategories.stream()
            .map(nc -> NationCategoryInfo.builder()
                .id(idObfuscator.encodeId(nc.getId()))
                .name(nc.getName())
                .categoryType(nc.getCategoryType() != null ? nc.getCategoryType().name() : null)
                .priorityFactor(nc.getPriorityFactor())
                .build())
            .collect(Collectors.toList());

        // Convert age categories to DTOs
        List<AgeCategoryInfo> ageCategoryInfos = null;
        if (includesAgeCategories) {
            ageCategoryInfos = ageCategories.stream()
                .map(ac -> AgeCategoryInfo.builder()
                    .id(idObfuscator.encodeId(ac.getId()))
                    .name(ac.getName())
                    .categoryType(ac.getCategoryType() != null ? ac.getCategoryType().name() : null)
                    .ageRange(ac.getAgeRangeDisplay())
                    .minAge(ac.getMinAge())
                    .maxAge(ac.getMaxAge())
                    .build())
                .collect(Collectors.toList());
        }

        // Convert existing rates to DTOs
        List<ParkTariffRateDTO> rateDTOs = existingRates.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());

        // Calculate summary
        int totalPossibleRates;
        if (includesAgeCategories) {
            totalPossibleRates = seasons.size() * nationCategories.size() * ageCategories.size();
        } else {
            totalPossibleRates = seasons.size() * nationCategories.size();
        }

        MatrixSummary summary = MatrixSummary.builder()
            .totalPossibleRates(totalPossibleRates)
            .existingRatesCount(existingRates.size())
            .missingRatesCount(Math.max(0, totalPossibleRates - existingRates.size()))
            .seasonsCount(seasons.size())
            .nationCategoriesCount(nationCategories.size())
            .ageCategoriesCount(includesAgeCategories ? ageCategories.size() : 0)
            .build();

        // Build park info
        ParkInfo parkInfo = ParkInfo.builder()
            .id(idObfuscator.encodeId(park.getId()))
            .name(park.getName())
            .build();

        // Build tariff info
        TariffInfo tariffInfo = TariffInfo.builder()
            .id(idObfuscator.encodeId(tariff.getId()))
            .name(tariff.getName())
            .chargingBasis(tariff.getChargingBasis().name())
            .chargingBasisDisplayName(tariff.getChargingBasis().getDisplayName())
            .build();

        // Build list of parks where tariff is used
        List<ParkInfo> parksWithTariffInfos = parksWithTariff.stream()
            .map(p -> ParkInfo.builder()
                .id(idObfuscator.encodeId(p.getId()))
                .name(p.getName())
                .build())
            .collect(Collectors.toList());

        return ParkRateMatrixResponseDTO.builder()
            .park(parkInfo)
            .tariff(tariffInfo)
            .parksWithTariff(parksWithTariffInfos)
            .seasons(seasonInfos)
            .nationCategories(nationCategoryInfos)
            .ageCategories(ageCategoryInfos)
            .includesAgeCategories(includesAgeCategories)
            .existingRates(rateDTOs)
            .summary(summary)
            .build();
    }

    /**
     * Convert ParkTariffRate entity to DTO
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
}
