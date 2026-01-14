package com.itineraryledger.kabengosafaris.ActivityTariffRate.Services;

import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.ActivityTariffRate;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.DTOs.ActivityRateMatrixResponseDTO;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.DTOs.ActivityRateMatrixResponseDTO.*;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.DTOs.ActivityTariffRateDTO;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.Repositories.ActivityTariffRateRepository;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.Specifications.ActivityTariffRateSpecification;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivity;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivityRepository;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.Repositories.PaxAgeCategoryRepository;
import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Repositories.PaxNationCategoryRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Season.Season;
import com.itineraryledger.kabengosafaris.Season.Repositories.SeasonRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
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
 * ActivityTariffRateMatrixService - Service for fetching rate matrix data for UI
 *
 * Provides an endpoint that returns:
 * - List of active global seasons (with exclusion support)
 * - List of active nation categories (with exclusion support)
 * - List of active age categories (only for PER_PERSON activities, with exclusion support)
 * - Existing rates filtered by the same exclusions
 *
 * This data is used to render a rate input grid/matrix in the frontend.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityTariffRateMatrixService {

    private final ActivityRepository activityRepository;
    private final ParkRepository parkRepository;
    private final ParkActivityRepository parkActivityRepository;
    private final SeasonRepository seasonRepository;
    private final PaxNationCategoryRepository nationCategoryRepository;
    private final PaxAgeCategoryRepository ageCategoryRepository;
    private final ActivityTariffRateRepository rateRepository;
    private final IdObfuscator idObfuscator;

    /**
     * Get rate matrix data for an activity
     *
     * @param activityIdObfuscated The obfuscated activity ID (required)
     * @param parkIdObfuscated     The obfuscated park ID (null for global rates)
     * @param excludeSeasonIds     List of obfuscated season IDs to exclude
     * @param excludeNationCategoryIds List of obfuscated nation category IDs to exclude
     * @param excludeAgeCategoryIds List of obfuscated age category IDs to exclude (only for PER_PERSON)
     * @return ResponseEntity with ActivityRateMatrixResponseDTO
     */
    public ResponseEntity<ApiResponse<?>> getRateMatrix(
        String activityIdObfuscated,
        String parkIdObfuscated,
        List<String> excludeSeasonIds,
        List<String> excludeNationCategoryIds,
        List<String> excludeAgeCategoryIds
    ) {
        log.info("Fetching rate matrix for activity: {}, park: {}", activityIdObfuscated, parkIdObfuscated);

        // Validate activity ID
        if (activityIdObfuscated == null || activityIdObfuscated.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Activity ID is required", "ACTIVITY_ID_REQUIRED")
            );
        }

        // Decode activity ID
        Long activityId;
        try {
            activityId = idObfuscator.decodeId(activityIdObfuscated);
            if (activityId == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid activity ID format", "INVALID_ACTIVITY_ID")
                );
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid activity ID format", "INVALID_ACTIVITY_ID")
            );
        }

        // Fetch activity
        Optional<Activity> activityOpt = activityRepository.findById(activityId);
        if (activityOpt.isEmpty()) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(404, "Activity not found", "ACTIVITY_NOT_FOUND")
            );
        }

        Activity activity = activityOpt.get();

        // Validate activity can have rates
        if (!Boolean.TRUE.equals(activity.getHasTariff())) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400,
                    "Activity '" + activity.getName() + "' cannot have rates because hasTariff is false",
                    "ACTIVITY_NO_TARIFF")
            );
        }

        if (activity.getChargingBasis() == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400,
                    "Activity '" + activity.getName() + "' cannot have rates because chargingBasis is not set",
                    "ACTIVITY_NO_CHARGING_BASIS")
            );
        }

        // Fetch all parks that offer this activity (for dropdown)
        List<Park> availableParks = fetchParksOfferingActivity(activityId);

        // Determine park selection
        // Global rates = activity has no parks associated with it
        // Park-specific rates = activity has parks, select specific one
        Long parkId = null;
        Park park = null;

        if (availableParks.isEmpty()) {
            // No parks offer this activity - this is a global activity
            log.info("Activity {} has no associated parks - using global rates", activity.getName());
        } else if (parkIdObfuscated != null && !parkIdObfuscated.trim().isEmpty()) {
            // User specified a park - validate it exists and is associated with this activity
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

                    // Verify the park is associated with this activity
                    boolean parkAssociated = availableParks.stream()
                        .anyMatch(p -> p.getId().equals(foundPark.getId()));
                    if (!parkAssociated) {
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(400,
                                "Park '" + foundPark.getName() + "' is not associated with activity '" + activity.getName() + "'",
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
            park = availableParks.get(0);
            parkId = park.getId();
            log.info("Auto-selected first available park: {} ({})", park.getName(), park.getId());
        }

        // Decode exclusion IDs
        Set<Long> excludedSeasonIdsDecoded = decodeIds(excludeSeasonIds);
        Set<Long> excludedNationCategoryIdsDecoded = decodeIds(excludeNationCategoryIds);
        Set<Long> excludedAgeCategoryIdsDecoded = decodeIds(excludeAgeCategoryIds);

        // Determine if age categories are applicable
        boolean includesAgeCategories = activity.getChargingBasis() == ChargingBasis.PER_PERSON;

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
        List<ActivityTariffRate> existingRates = fetchExistingRates(
            activityId, parkId,
            excludedSeasonIdsDecoded, excludedNationCategoryIdsDecoded, excludedAgeCategoryIdsDecoded,
            includesAgeCategories
        );

        // Build response
        ActivityRateMatrixResponseDTO response = buildResponse(
            activity, park, availableParks, seasons, nationCategories, ageCategories,
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
        // Using specification or custom query would be cleaner, but for simplicity:
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
     * Fetch all parks that offer the specified activity
     */
    private List<Park> fetchParksOfferingActivity(Long activityId) {
        List<ParkActivity> parkActivities = parkActivityRepository.findByActivityIdWithPark(activityId);
        return parkActivities.stream()
            .map(ParkActivity::getPark)
            .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
            .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
            .collect(Collectors.toList());
    }

    /**
     * Fetch existing rates for the activity with exclusions applied
     */
    private List<ActivityTariffRate> fetchExistingRates(
        Long activityId, Long parkId,
        Set<Long> excludedSeasonIds, Set<Long> excludedNationCategoryIds,
        Set<Long> excludedAgeCategoryIds, boolean includesAgeCategories
    ) {
        // Build base specification
        Specification<ActivityTariffRate> spec = ActivityTariffRateSpecification.byActivityId(activityId);

        // Filter by park (or global)
        if (parkId == null) {
            spec = spec.and(ActivityTariffRateSpecification.globalRatesOnly());
        } else {
            spec = spec.and(ActivityTariffRateSpecification.byParkId(parkId));
        }

        // Fetch all matching rates
        List<ActivityTariffRate> rates = rateRepository.findAll(spec);

        // Apply exclusions in memory (could be optimized with custom queries)
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
    private ActivityRateMatrixResponseDTO buildResponse(
        Activity activity,
        Park park,
        List<Park> availableParks,
        List<Season> seasons,
        List<PaxNationCategory> nationCategories,
        List<PaxAgeCategory> ageCategories,
        List<ActivityTariffRate> existingRates,
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
        List<ActivityTariffRateDTO> rateDTOs = existingRates.stream()
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

        // Build activity info
        ActivityInfo activityInfo = ActivityInfo.builder()
            .id(idObfuscator.encodeId(activity.getId()))
            .name(activity.getName())
            .chargingBasis(activity.getChargingBasis().name())
            .chargingBasisDisplayName(activity.getChargingBasis().getDisplayName())
            .hasTariff(activity.getHasTariff())
            .build();

        // Build current park info (the park being viewed)
        ParkInfo parkInfo = null;
        if (park != null) {
            parkInfo = ParkInfo.builder()
                .id(idObfuscator.encodeId(park.getId()))
                .name(park.getName())
                .build();
        }

        // Convert available parks to DTOs (for dropdown selection)
        List<ParkInfo> availableParkInfos = availableParks.stream()
            .map(p -> ParkInfo.builder()
                .id(idObfuscator.encodeId(p.getId()))
                .name(p.getName())
                .build())
            .collect(Collectors.toList());

        return ActivityRateMatrixResponseDTO.builder()
            .activity(activityInfo)
            .park(parkInfo)
            .availableParks(availableParkInfos)
            .isGlobalRateMatrix(park == null)
            .seasons(seasonInfos)
            .nationCategories(nationCategoryInfos)
            .ageCategories(ageCategoryInfos)
            .includesAgeCategories(includesAgeCategories)
            .existingRates(rateDTOs)
            .summary(summary)
            .build();
    }

    /**
     * Convert ActivityTariffRate entity to DTO
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
