package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRate;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRateRepository;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CostItemType;
import com.itineraryledger.kabengosafaris.Season.Season;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Service for looking up accommodation rates with active filtering.
 *
 * - Only returns ACTIVE rates (isActive = true)
 * - Automatically resolves season (accommodation-specific → global)
 * - Logs rate issues when not found or inactive
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccommodationRateLookupService {

    private final AccommodationRateRepository accommodationRateRepository;
    private final SeasonResolverService seasonResolverService;
    private final RateIssueLoggerService rateIssueLoggerService;
    private final IdObfuscator idObfuscator;

    /**
     * Lookup result containing the rate and metadata.
     */
    public record LookupResult(
        AccommodationRate rate,
        boolean found,
        String seasonName
    ) {
        public static LookupResult notFound(String seasonName) {
            return new LookupResult(null, false, seasonName);
        }

        public static LookupResult found(AccommodationRate rate, String seasonName) {
            return new LookupResult(rate, true, seasonName);
        }
    }

    /**
     * Find an active accommodation rate.
     *
     * @param accommodationId Accommodation ID
     * @param seasonId Season ID
     * @param roomTypeId Room type ID
     * @param roomStandardId Room standard ID
     * @param boardTypeId Board type ID
     * @return Optional containing the active rate if found
     */
    public Optional<AccommodationRate> findActiveRate(
            Long accommodationId,
            Long seasonId,
            Long roomTypeId,
            Long roomStandardId,
            Long boardTypeId
    ) {
        if (accommodationId == null || seasonId == null || roomTypeId == null
            || roomStandardId == null || boardTypeId == null) {
            return Optional.empty();
        }

        return accommodationRateRepository.findActiveRate(
            accommodationId, seasonId, roomTypeId, roomStandardId, boardTypeId
        );
    }

    /**
     * Lookup rate with automatic season resolution and issue logging.
     *
     * Season resolution follows this logic:
     * 1. If accommodation has NO seasons → log NO_SEASON, return not found (skip cost calculation)
     * 2. If accommodation has seasons and one matches the date → use that season
     * 3. If accommodation has seasons but none match the date → fallback to highest priority season type
     *    Priority: HIGH_SEASON > PEAK_SEASON > SHOULDER_SEASON > FESTIVE_SEASON > SPECIAL_EVENT > LOW_SEASON > STANDARD > CUSTOM
     * 4. NEVER fallback to global seasons (accommodation rates are stored per accommodation's season)
     *
     * @param accommodationId Accommodation ID
     * @param accommodationName Accommodation name (for logging)
     * @param roomTypeId Room type ID
     * @param roomStandardId Room standard ID
     * @param boardTypeId Board type ID
     * @param date Date to determine season
     * @param dayNumber Day number (for logging)
     * @return LookupResult containing the rate or indicating not found
     */
    public LookupResult lookupRateWithIssueLogging(
            Long accommodationId,
            String accommodationName,
            Long roomTypeId,
            Long roomStandardId,
            Long boardTypeId,
            LocalDate date,
            Integer dayNumber
    ) {
        String obfuscatedId = idObfuscator.encodeId(accommodationId);

        // Resolve season with detailed result
        SeasonResolverService.AccommodationSeasonResult seasonResult = seasonResolverService
            .resolveAccommodationSeasonWithDetails(accommodationId, date);

        // Case 1: Accommodation has no seasons configured at all
        if (!seasonResult.hasSeasons()) {
            rateIssueLoggerService.logNoSeason(
                accommodationName,
                obfuscatedId,
                dayNumber
            );
            return LookupResult.notFound("No Season");
        }

        Season season = seasonResult.season();
        String seasonName = season != null ? season.getName() : "Unknown Season";

        // Case 2: Season was resolved (either direct match or fallback)
        if (season == null) {
            // This shouldn't happen if hasSeasons is true, but handle defensively
            rateIssueLoggerService.logMissingRate(
                CostItemType.ACCOMMODATION,
                accommodationName,
                obfuscatedId,
                dayNumber,
                null,
                "No applicable season"
            );
            return LookupResult.notFound(seasonName);
        }

        // Log if fallback season was used (informational)
        if (seasonResult.usedFallback()) {
            rateIssueLoggerService.logSeasonNotFound(
                accommodationName,
                obfuscatedId,
                dayNumber,
                date.toString(),
                season.getName(),
                season.getSeasonType() != null ? season.getSeasonType().getDisplayName() : "Unknown"
            );
        }

        Long seasonId = season.getId();

        // Check for missing parameters
        if (roomTypeId == null || roomStandardId == null || boardTypeId == null) {
            rateIssueLoggerService.logMissingRate(
                CostItemType.ACCOMMODATION,
                accommodationName,
                obfuscatedId,
                dayNumber,
                null,
                seasonName
            );
            return LookupResult.notFound(seasonName);
        }

        // Try to find active rate
        Optional<AccommodationRate> rateOpt = accommodationRateRepository.findActiveRate(
            accommodationId, seasonId, roomTypeId, roomStandardId, boardTypeId
        );

        if (rateOpt.isEmpty()) {
            // Check if rate exists but is inactive
            Optional<AccommodationRate> anyRate = accommodationRateRepository
                .findByAccommodationIdAndSeasonIdAndRoomTypeIdAndRoomStandardIdAndBoardTypeId(
                    accommodationId, seasonId, roomTypeId, roomStandardId, boardTypeId
                );

            if (anyRate.isPresent() && Boolean.FALSE.equals(anyRate.get().getIsActive())) {
                rateIssueLoggerService.logInactiveRate(
                    CostItemType.ACCOMMODATION,
                    accommodationName,
                    obfuscatedId,
                    dayNumber,
                    null,
                    seasonName
                );
            } else {
                rateIssueLoggerService.logMissingRate(
                    CostItemType.ACCOMMODATION,
                    accommodationName,
                    obfuscatedId,
                    dayNumber,
                    null,
                    seasonName
                );
            }

            return LookupResult.notFound(seasonName);
        }

        AccommodationRate rate = rateOpt.get();

        // Check if STO rate is missing (optional warning)
        if (rate.getStoRate() == null) {
            rateIssueLoggerService.logNoStoRate(
                CostItemType.ACCOMMODATION,
                accommodationName,
                obfuscatedId,
                dayNumber,
                null
            );
        }

        return LookupResult.found(rate, seasonName);
    }
}
