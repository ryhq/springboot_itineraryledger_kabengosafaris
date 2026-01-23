package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core;

import com.itineraryledger.kabengosafaris.Season.Season;
import com.itineraryledger.kabengosafaris.Season.SeasonPeriod;
import com.itineraryledger.kabengosafaris.Season.Repositories.SeasonPeriodRepository;
import com.itineraryledger.kabengosafaris.Season.Repositories.SeasonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Service for resolving the applicable season for a given date.
 *
 * Supports:
 * - Global seasons (used by parks)
 * - Accommodation-specific seasons (NO fallback to global for accommodations)
 * - Park-specific seasons (with fallback to global)
 *
 * ACCOMMODATION SEASON RESOLUTION LOGIC:
 * 1. If accommodation has no seasons at all → return null (caller should skip cost calculation)
 * 2. If accommodation has seasons but none match the date → fallback to highest priority season type:
 *    HIGH_SEASON > PEAK_SEASON > SHOULDER_SEASON > FESTIVE_SEASON > SPECIAL_EVENT > LOW_SEASON > STANDARD > CUSTOM
 * 3. Never fallback to global seasons for accommodations (rates are stored per accommodation's season)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeasonResolverService {

    private final SeasonPeriodRepository seasonPeriodRepository;
    private final SeasonRepository seasonRepository;

    /**
     * Find the applicable global season for a date.
     *
     * @param date The date to find season for
     * @return The matching global season, or null if none found
     */
    public Season resolveGlobalSeason(LocalDate date) {
        List<SeasonPeriod> allPeriods = seasonPeriodRepository.findAll();

        // Find active global season period that contains the date
        for (SeasonPeriod period : allPeriods) {
            Season season = period.getSeason();
            if (season != null &&
                season.getIsGlobal() != null && season.getIsGlobal() &&
                season.getIsActive() != null && season.getIsActive() &&
                period.containsDate(date)) {
                return season;
            }
        }

        // Fallback: return first active global season
        return allPeriods.stream()
            .filter(p -> p.getSeason() != null)
            .filter(p -> p.getSeason().getIsGlobal() != null && p.getSeason().getIsGlobal())
            .filter(p -> p.getSeason().getIsActive() != null && p.getSeason().getIsActive())
            .map(SeasonPeriod::getSeason)
            .findFirst()
            .orElse(null);
    }

    /**
     * Result of accommodation season resolution.
     */
    public record AccommodationSeasonResult(
        Season season,
        boolean hasSeasons,
        boolean usedFallback
    ) {
        public static AccommodationSeasonResult noSeasons() {
            return new AccommodationSeasonResult(null, false, false);
        }

        public static AccommodationSeasonResult found(Season season) {
            return new AccommodationSeasonResult(season, true, false);
        }

        public static AccommodationSeasonResult fallback(Season season) {
            return new AccommodationSeasonResult(season, true, true);
        }
    }

    /**
     * Find the applicable season for an accommodation on a given date.
     *
     * RESOLUTION LOGIC:
     * 1. If accommodation has NO seasons at all → return null (skip cost calculation)
     * 2. If accommodation has seasons and one matches the date → return that season
     * 3. If accommodation has seasons but none match the date → fallback to highest priority season type:
     *    HIGH_SEASON > PEAK_SEASON > SHOULDER_SEASON > FESTIVE_SEASON > SPECIAL_EVENT > LOW_SEASON > STANDARD > CUSTOM
     * 4. NEVER fallback to global seasons (rates are stored per accommodation's season)
     *
     * @param accommodationId The accommodation ID
     * @param date The date to find season for
     * @return The matching season, or null if accommodation has no seasons
     */
    public Season resolveAccommodationSeason(Long accommodationId, LocalDate date) {
        return resolveAccommodationSeasonWithDetails(accommodationId, date).season();
    }

    /**
     * Find the applicable season for an accommodation with detailed result.
     *
     * @param accommodationId The accommodation ID
     * @param date The date to find season for
     * @return AccommodationSeasonResult with season and metadata
     */
    public AccommodationSeasonResult resolveAccommodationSeasonWithDetails(Long accommodationId, LocalDate date) {
        if (accommodationId == null) {
            return AccommodationSeasonResult.noSeasons();
        }

        // Check if accommodation has any seasons at all
        boolean hasSeasons = seasonRepository.existsByAccommodationId(accommodationId);
        if (!hasSeasons) {
            log.debug("Accommodation {} has no seasons configured", accommodationId);
            return AccommodationSeasonResult.noSeasons();
        }

        List<SeasonPeriod> allPeriods = seasonPeriodRepository.findAll();

        // Try to find accommodation-specific season that contains the date
        for (SeasonPeriod period : allPeriods) {
            Season season = period.getSeason();
            if (season != null &&
                season.getAccommodation() != null &&
                season.getAccommodation().getId().equals(accommodationId) &&
                season.getIsActive() != null && season.getIsActive() &&
                period.containsDate(date)) {
                return AccommodationSeasonResult.found(season);
            }
        }

        // Date not in any season period - fallback to highest priority season type
        List<Season> prioritizedSeasons = seasonRepository
            .findActiveByAccommodationIdOrderedByTypePriority(accommodationId);

        if (!prioritizedSeasons.isEmpty()) {
            Season fallbackSeason = prioritizedSeasons.get(0);
            log.debug("Accommodation {} has no season for date {}, falling back to {} ({})",
                accommodationId, date, fallbackSeason.getName(), fallbackSeason.getSeasonType());
            return AccommodationSeasonResult.fallback(fallbackSeason);
        }

        // Accommodation has seasons but none are active
        log.debug("Accommodation {} has seasons but none are active", accommodationId);
        return AccommodationSeasonResult.noSeasons();
    }

    /**
     * Find the applicable season for a park on a given date.
     *
     * Parks typically use global seasons, but this method allows for
     * future park-specific season implementations.
     *
     * @param parkId The park ID (currently unused but allows future extension)
     * @param date The date to find season for
     * @return The matching season (currently always global)
     */
    public Season resolveParkSeason(Long parkId, LocalDate date) {
        // Parks currently use global seasons
        // This method allows for future park-specific seasons if needed
        return resolveGlobalSeason(date);
    }

    /**
     * Get the season name for a date, with fallback to "Unknown Season".
     *
     * @param date The date to get season name for
     * @return The season name or "Unknown Season"
     */
    public String getGlobalSeasonName(LocalDate date) {
        Season season = resolveGlobalSeason(date);
        return season != null ? season.getName() : "Unknown Season";
    }

    /**
     * Get the season name for an accommodation on a date.
     *
     * @param accommodationId The accommodation ID
     * @param date The date to get season name for
     * @return The season name or "Unknown Season"
     */
    public String getAccommodationSeasonName(Long accommodationId, LocalDate date) {
        Season season = resolveAccommodationSeason(accommodationId, date);
        return season != null ? season.getName() : "Unknown Season";
    }
}
