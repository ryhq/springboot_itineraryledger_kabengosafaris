package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CostItemType;
import com.itineraryledger.kabengosafaris.ParkTariffRate.ParkTariffRate;
import com.itineraryledger.kabengosafaris.ParkTariffRate.Repositories.ParkTariffRateRepository;
import com.itineraryledger.kabengosafaris.Season.Season;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for looking up park tariff rates with active filtering.
 *
 * - Only returns ACTIVE rates (isActive = true)
 * - Supports per-person rates (with age category) and group rates (without age category)
 * - Logs rate issues when not found or inactive
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParkTariffRateLookupService {

    private final ParkTariffRateRepository parkTariffRateRepository;
    private final RateIssueLoggerService rateIssueLoggerService;
    private final IdObfuscator idObfuscator;

    /**
     * Lookup result containing the rate and metadata.
     */
    public record LookupResult(
        ParkTariffRate rate,
        boolean found,
        String seasonName
    ) {
        public static LookupResult notFound(String seasonName) {
            return new LookupResult(null, false, seasonName);
        }

        public static LookupResult found(ParkTariffRate rate, String seasonName) {
            return new LookupResult(rate, true, seasonName);
        }
    }

    /**
     * Find an active rate for per-person tariffs.
     *
     * @param parkId Park ID
     * @param tariffId Tariff ID
     * @param seasonId Season ID
     * @param nationCategoryId Nation category ID
     * @param ageCategoryId Age category ID
     * @return Optional containing the active rate if found
     */
    public Optional<ParkTariffRate> findActiveRateForPerson(
            Long parkId,
            Long tariffId,
            Long seasonId,
            Long nationCategoryId,
            Long ageCategoryId
    ) {
        return parkTariffRateRepository.findActiveRateForPerson(
            parkId, tariffId, seasonId, nationCategoryId, ageCategoryId
        );
    }

    /**
     * Find an active rate for group/vehicle/flat tariffs.
     *
     * @param parkId Park ID
     * @param tariffId Tariff ID
     * @param seasonId Season ID
     * @param nationCategoryId Nation category ID
     * @return Optional containing the active rate if found
     */
    public Optional<ParkTariffRate> findActiveRateForGroup(
            Long parkId,
            Long tariffId,
            Long seasonId,
            Long nationCategoryId
    ) {
        return parkTariffRateRepository.findActiveRateForGroup(
            parkId, tariffId, seasonId, nationCategoryId
        );
    }

    /**
     * Lookup per-person rate with issue logging.
     *
     * @param parkId Park ID
     * @param parkName Park name (for logging)
     * @param tariffId Tariff ID
     * @param tariffName Tariff name (for logging)
     * @param season Season
     * @param nationCategoryId Nation category ID
     * @param ageCategoryId Age category ID
     * @param paxCategory Pax category description (for logging)
     * @param dayNumber Day number (for logging)
     * @return LookupResult containing the rate or indicating not found
     */
    public LookupResult lookupPersonRateWithIssueLogging(
            Long parkId,
            String parkName,
            Long tariffId,
            String tariffName,
            Season season,
            Long nationCategoryId,
            Long ageCategoryId,
            String paxCategory,
            Integer dayNumber
    ) {
        String seasonName = season != null ? season.getName() : "Unknown Season";
        String itemName = tariffName + " - " + parkName;

        if (season == null || nationCategoryId == null || ageCategoryId == null) {
            rateIssueLoggerService.logMissingRate(
                CostItemType.PARK_FEE,
                itemName,
                idObfuscator.encodeId(tariffId),
                dayNumber,
                paxCategory,
                seasonName
            );
            return LookupResult.notFound(seasonName);
        }

        Long seasonId = season.getId();

        Optional<ParkTariffRate> rateOpt = findActiveRateForPerson(
            parkId, tariffId, seasonId, nationCategoryId, ageCategoryId
        );

        if (rateOpt.isEmpty()) {
            rateIssueLoggerService.logMissingRate(
                CostItemType.PARK_FEE,
                itemName,
                idObfuscator.encodeId(tariffId),
                dayNumber,
                paxCategory,
                seasonName
            );
            return LookupResult.notFound(seasonName);
        }

        ParkTariffRate rate = rateOpt.get();

        // Check if STO rate is missing
        if (rate.getStoRate() == null) {
            rateIssueLoggerService.logNoStoRate(
                CostItemType.PARK_FEE,
                itemName,
                idObfuscator.encodeId(tariffId),
                dayNumber,
                paxCategory
            );
        }

        return LookupResult.found(rate, seasonName);
    }

    /**
     * Lookup group rate with issue logging.
     *
     * @param parkId Park ID
     * @param parkName Park name (for logging)
     * @param tariffId Tariff ID
     * @param tariffName Tariff name (for logging)
     * @param season Season
     * @param nationCategoryId Nation category ID
     * @param chargingBasisDescription Description of charging basis (for logging)
     * @param dayNumber Day number (for logging)
     * @return LookupResult containing the rate or indicating not found
     */
    public LookupResult lookupGroupRateWithIssueLogging(
            Long parkId,
            String parkName,
            Long tariffId,
            String tariffName,
            Season season,
            Long nationCategoryId,
            String chargingBasisDescription,
            Integer dayNumber
    ) {
        String seasonName = season != null ? season.getName() : "Unknown Season";
        String itemName = tariffName + " - " + parkName;

        if (season == null || nationCategoryId == null) {
            rateIssueLoggerService.logMissingRate(
                CostItemType.PARK_FEE,
                itemName,
                idObfuscator.encodeId(tariffId),
                dayNumber,
                chargingBasisDescription,
                seasonName
            );
            return LookupResult.notFound(seasonName);
        }

        Long seasonId = season.getId();

        Optional<ParkTariffRate> rateOpt = findActiveRateForGroup(
            parkId, tariffId, seasonId, nationCategoryId
        );

        if (rateOpt.isEmpty()) {
            rateIssueLoggerService.logMissingRate(
                CostItemType.PARK_FEE,
                itemName,
                idObfuscator.encodeId(tariffId),
                dayNumber,
                chargingBasisDescription,
                seasonName
            );
            return LookupResult.notFound(seasonName);
        }

        ParkTariffRate rate = rateOpt.get();

        // Check if STO rate is missing
        if (rate.getStoRate() == null) {
            rateIssueLoggerService.logNoStoRate(
                CostItemType.PARK_FEE,
                itemName,
                idObfuscator.encodeId(tariffId),
                dayNumber,
                chargingBasisDescription
            );
        }

        return LookupResult.found(rate, seasonName);
    }
}
