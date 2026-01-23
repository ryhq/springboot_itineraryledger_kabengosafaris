package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core;

import com.itineraryledger.kabengosafaris.ActivityTariffRate.ActivityTariffRate;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.Repositories.ActivityTariffRateRepository;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CostItemType;
import com.itineraryledger.kabengosafaris.Season.Season;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for looking up activity tariff rates with active filtering.
 *
 * - Only returns ACTIVE rates (isActive = true)
 * - Supports park-specific rates with fallback to global rates
 * - Supports per-person rates and group rates
 * - Logs rate issues when not found or inactive
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityRateLookupService {

    private final ActivityTariffRateRepository activityTariffRateRepository;
    private final RateIssueLoggerService rateIssueLoggerService;
    private final IdObfuscator idObfuscator;

    /**
     * Lookup result containing the rate and metadata.
     */
    public record LookupResult(
        ActivityTariffRate rate,
        boolean found,
        String seasonName,
        boolean isParkSpecific
    ) {
        public static LookupResult notFound(String seasonName) {
            return new LookupResult(null, false, seasonName, false);
        }

        public static LookupResult foundParkRate(ActivityTariffRate rate, String seasonName) {
            return new LookupResult(rate, true, seasonName, true);
        }

        public static LookupResult foundGlobalRate(ActivityTariffRate rate, String seasonName) {
            return new LookupResult(rate, true, seasonName, false);
        }
    }

    /**
     * Find an active rate for per-person activities.
     * Tries park-specific first, then falls back to global.
     *
     * @param activityId Activity ID
     * @param parkId Park ID (null for global lookup)
     * @param seasonId Season ID
     * @param nationCategoryId Nation category ID
     * @param ageCategoryId Age category ID
     * @return LookupResult containing the rate and whether it's park-specific
     */
    public LookupResult findActiveRateForPerson(
            Long activityId,
            Long parkId,
            Long seasonId,
            Long nationCategoryId,
            Long ageCategoryId,
            String seasonName
    ) {
        // Try park-specific rate first
        if (parkId != null) {
            Optional<ActivityTariffRate> parkRate = activityTariffRateRepository.findActiveParkRateForPerson(
                activityId, parkId, seasonId, nationCategoryId, ageCategoryId
            );
            if (parkRate.isPresent()) {
                return LookupResult.foundParkRate(parkRate.get(), seasonName);
            }
        }

        // Fallback to global rate
        Optional<ActivityTariffRate> globalRate = activityTariffRateRepository.findActiveGlobalRateForPerson(
            activityId, seasonId, nationCategoryId, ageCategoryId
        );

        if (globalRate.isPresent()) {
            return LookupResult.foundGlobalRate(globalRate.get(), seasonName);
        }

        return LookupResult.notFound(seasonName);
    }

    /**
     * Find an active rate for group/vehicle activities.
     * Tries park-specific first, then falls back to global.
     *
     * @param activityId Activity ID
     * @param parkId Park ID (null for global lookup)
     * @param seasonId Season ID
     * @param nationCategoryId Nation category ID
     * @return LookupResult containing the rate and whether it's park-specific
     */
    public LookupResult findActiveRateForGroup(
            Long activityId,
            Long parkId,
            Long seasonId,
            Long nationCategoryId,
            String seasonName
    ) {
        // Try park-specific rate first
        if (parkId != null) {
            Optional<ActivityTariffRate> parkRate = activityTariffRateRepository.findActiveParkRateForGroup(
                activityId, parkId, seasonId, nationCategoryId
            );
            if (parkRate.isPresent()) {
                return LookupResult.foundParkRate(parkRate.get(), seasonName);
            }
        }

        // Fallback to global rate
        Optional<ActivityTariffRate> globalRate = activityTariffRateRepository.findActiveGlobalRateForGroup(
            activityId, seasonId, nationCategoryId
        );

        if (globalRate.isPresent()) {
            return LookupResult.foundGlobalRate(globalRate.get(), seasonName);
        }

        return LookupResult.notFound(seasonName);
    }

    /**
     * Lookup per-person activity rate with issue logging.
     *
     * @param activityId Activity ID
     * @param activityName Activity name (for logging)
     * @param parkId Park ID (null for standalone activities)
     * @param parkName Park name (for logging)
     * @param season Season
     * @param nationCategoryId Nation category ID
     * @param ageCategoryId Age category ID
     * @param paxCategory Pax category description (for logging)
     * @param dayNumber Day number (for logging)
     * @return LookupResult containing the rate or indicating not found
     */
    public LookupResult lookupPersonRateWithIssueLogging(
            Long activityId,
            String activityName,
            Long parkId,
            String parkName,
            Season season,
            Long nationCategoryId,
            Long ageCategoryId,
            String paxCategory,
            Integer dayNumber
    ) {
        String seasonName = season != null ? season.getName() : "Unknown Season";
        String itemName = activityName + (parkName != null ? " @ " + parkName : "");

        if (season == null || nationCategoryId == null || ageCategoryId == null) {
            rateIssueLoggerService.logMissingRate(
                CostItemType.ACTIVITY,
                itemName,
                idObfuscator.encodeId(activityId),
                dayNumber,
                paxCategory,
                seasonName
            );
            return LookupResult.notFound(seasonName);
        }

        Long seasonId = season.getId();

        LookupResult result = findActiveRateForPerson(
            activityId, parkId, seasonId, nationCategoryId, ageCategoryId, seasonName
        );

        if (!result.found()) {
            rateIssueLoggerService.logMissingRate(
                CostItemType.ACTIVITY,
                itemName,
                idObfuscator.encodeId(activityId),
                dayNumber,
                paxCategory,
                seasonName
            );
            return result;
        }

        // Check if STO rate is missing
        if (result.rate().getStoRate() == null) {
            rateIssueLoggerService.logNoStoRate(
                CostItemType.ACTIVITY,
                itemName,
                idObfuscator.encodeId(activityId),
                dayNumber,
                paxCategory
            );
        }

        return result;
    }

    /**
     * Lookup group activity rate with issue logging.
     *
     * @param activityId Activity ID
     * @param activityName Activity name (for logging)
     * @param parkId Park ID (null for standalone activities)
     * @param parkName Park name (for logging)
     * @param season Season
     * @param nationCategoryId Nation category ID
     * @param chargingBasisDescription Description of charging basis (for logging)
     * @param dayNumber Day number (for logging)
     * @return LookupResult containing the rate or indicating not found
     */
    public LookupResult lookupGroupRateWithIssueLogging(
            Long activityId,
            String activityName,
            Long parkId,
            String parkName,
            Season season,
            Long nationCategoryId,
            String chargingBasisDescription,
            Integer dayNumber
    ) {
        String seasonName = season != null ? season.getName() : "Unknown Season";
        String itemName = activityName + (parkName != null ? " @ " + parkName : "");

        if (season == null || nationCategoryId == null) {
            rateIssueLoggerService.logMissingRate(
                CostItemType.ACTIVITY,
                itemName,
                idObfuscator.encodeId(activityId),
                dayNumber,
                chargingBasisDescription,
                seasonName
            );
            return LookupResult.notFound(seasonName);
        }

        Long seasonId = season.getId();

        LookupResult result = findActiveRateForGroup(
            activityId, parkId, seasonId, nationCategoryId, seasonName
        );

        if (!result.found()) {
            rateIssueLoggerService.logMissingRate(
                CostItemType.ACTIVITY,
                itemName,
                idObfuscator.encodeId(activityId),
                dayNumber,
                chargingBasisDescription,
                seasonName
            );
            return result;
        }

        // Check if STO rate is missing
        if (result.rate().getStoRate() == null) {
            rateIssueLoggerService.logNoStoRate(
                CostItemType.ACTIVITY,
                itemName,
                idObfuscator.encodeId(activityId),
                dayNumber,
                chargingBasisDescription
            );
        }

        return result;
    }
}
