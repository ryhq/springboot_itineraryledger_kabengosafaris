package com.itineraryledger.kabengosafaris.Initializers;

import com.itineraryledger.kabengosafaris.Season.Repositories.SeasonPeriodRepository;
import com.itineraryledger.kabengosafaris.Season.Repositories.SeasonRepository;
import com.itineraryledger.kabengosafaris.Season.Season;
import com.itineraryledger.kabengosafaris.Season.SeasonPeriod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;

/**
 * GlobalSeasonInitializer - Initializes global tourism seasons for Tanzania at application startup
 *
 * IMPORTANT: This initializer creates global seasons used by TANAPA (Tanzania National Parks Authority),
 * NCAA (Ngorongoro Conservation Area Authority), and beach destinations for pricing and planning.
 *
 * Runs AFTER ParkActivityInitializer (Order = HIGHEST_PRECEDENCE + 9)
 *
 * Data Sources:
 * - TANAPA official tourism data
 * - Tanzania safari industry standards
 * - Serengeti and Ngorongoro seasonal patterns
 * - Beach/Zanzibar tourism seasons
 * - Wildebeest migration patterns
 *
 * Season Structure (Based on Tanzania Tourism Industry Standards):
 * 1. Peak Season (June-October): Dry season, best wildlife viewing, highest prices
 * 2. Shoulder Season - Green/Short Rains (November-December): Lower crowds, good wildlife, lower prices
 * 3. Shoulder Season - Calving (January-February): Wildebeest calving, excellent photography, moderate prices
 * 4. Low Season - Long Rains (March-May): Rainy season, lowest prices, fewer tourists
 * 5. Festive Season (Christmas-New Year): Holiday period, premium pricing
 * 6. Migration Season (July-September): Wildebeest river crossings, peak wildlife activity
 *
 * Behavior:
 * - Only creates global seasons that don't already exist (checked by name)
 * - Creates associated season periods for recurring annual patterns
 * - Does not update or delete existing seasons
 * - All created seasons are marked as global (isGlobal = true, accommodation = null)
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 9) // Run after ParkActivityInitializer
public class GlobalSeasonInitializer implements ApplicationRunner {

    private final SeasonRepository seasonRepository;
    private final SeasonPeriodRepository seasonPeriodRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        printStartBanner();
        boolean success = true;

        try {
            initializeGlobalSeasons();
        } catch (Exception e) {
            log.error("Error during global season initialization: {}", e.getMessage(), e);
            success = false;
        } finally {
            printEndBanner(success);
        }
    }

    private void printStartBanner() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════════╗");
        log.info("║                                                                    ║");
        log.info("║          GLOBAL SEASON INITIALIZER - START                         ║");
        log.info("║          Tanzania Tourism Seasons (TANAPA/NCAA/Beaches)            ║");
        log.info("║                                                                    ║");
        log.info("╚════════════════════════════════════════════════════════════════════╝");
        log.info("");
    }

    private void printEndBanner(boolean success) {
        log.info("");
        if (success) {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║        ✓ GLOBAL SEASON INITIALIZER - COMPLETED                     ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        } else {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║        ✗ GLOBAL SEASON INITIALIZER - FAILED                        ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        }
        log.info("");
    }

    private void initializeGlobalSeasons() {
        log.info("Starting global season initialization for Tanzania tourism...");

        List<SeasonData> seasonsData = getSeasonDataList();

        int createdSeasons = 0;
        int createdPeriods = 0;
        int skippedSeasons = 0;

        for (SeasonData seasonData : seasonsData) {
            // Check if season already exists
            if (seasonRepository.existsByIsGlobalTrueAndName(seasonData.name)) {
                log.debug("⊘ Global season already exists: {}", seasonData.name);
                skippedSeasons++;
                continue;
            }

            // Create season
            Season season = Season.builder()
                    .name(seasonData.name)
                    .seasonType(seasonData.seasonType)
                    .description(seasonData.description)
                    .accommodation(null) // Global season - not tied to any accommodation
                    .isGlobal(true)
                    .isActive(seasonData.isActive)
                    .isSystem(seasonData.isSystem) // Use isSystem from seasonData
                    .build();

            season = seasonRepository.save(season);
            log.info("✓ Created global season: {} ({})", seasonData.name, seasonData.seasonType);
            createdSeasons++;

            // Create season periods
            for (PeriodData periodData : seasonData.periods) {
                SeasonPeriod period = SeasonPeriod.builder()
                        .season(season)
                        .startDate(periodData.startDate)
                        .endDate(periodData.endDate)
                        .year(periodData.year) // null for recurring annual periods
                        .notes(periodData.notes)
                        .isActive(true)
                        .isSystem(seasonData.isSystem) // Use same isSystem value as parent season
                        .build();

                seasonPeriodRepository.save(period);

                String yearInfo = periodData.year != null ? String.valueOf(periodData.year) : "recurring";
                log.info("  → Added period: {} to {} ({})",
                    periodData.startDate, periodData.endDate, yearInfo);
                createdPeriods++;
            }
        }

        log.info("");
        log.info("Global season initialization complete:");
        log.info("  - Created: {} global seasons", createdSeasons);
        log.info("  - Created: {} season periods", createdPeriods);
        log.info("  - Skipped: {} existing seasons", skippedSeasons);
        log.info("  - Total global seasons in system: {}", seasonRepository.countByIsGlobalTrue());
    }

    /**
     * Get Tanzania tourism season data based on industry standards
     *
     * THREE SYSTEM DEFAULT SEASONS (isSystem = true):
     * 1. High Season - ID 1
     * 2. Shoulder Season - ID 2
     * 3. Low Season - ID 3
     *
     * OTHER GLOBAL SEASONS (isSystem = false):
     * All other seasons are global but not system-protected
     *
     * Data Sources:
     * - Tanzania safari operators (African Scenic Safaris, Asilia Africa, etc.)
     * - TANAPA (Tanzania National Parks Authority)
     * - NCAA (Ngorongoro Conservation Area Authority)
     * - Safari Bookings industry data
     *
     * References:
     * - https://africanscenicsafaris.com/best-time-to-visit-tanzania
     * - https://www.safaribookings.com/tanzania/best-time
     * - https://www.asiliaafrica.com/destinations/tanzania/when-to-go/
     */
    private List<SeasonData> getSeasonDataList() {
        List<SeasonData> seasons = new ArrayList<>();

        // ========== SYSTEM DEFAULT SEASONS (isSystem = true) ==========
        // These are the three core tourism seasons for Tanzania

        // ===== 1. HIGH SEASON (ID = 1, isSystem = true) =====
        // June to October: Dry season, best time for safari, highest prices
        SeasonData highSeason = new SeasonData(
            "High Season",
            Season.SeasonType.HIGH_SEASON,
            "Tanzania's high tourism season during the dry months (June-October). Best time for wildlife viewing " +
            "as animals gather around water sources. Clear skies, minimal rain, and excellent game drives. " +
            "Highest prices and largest crowds. Prime time for Serengeti safaris and Ngorongoro Crater visits.",
            true , // isSystem = true
            true  // isActive = true
        );
        highSeason.addPeriod(
            MonthDay.of(6, 1),   // June 1
            MonthDay.of(10, 31), // October 31
            null,                // Recurring annually
            "Annual dry season - peak wildlife viewing period. Clear weather, large animal congregations at " +
            "water sources, best photography conditions."
        );
        seasons.add(highSeason);

        // ===== 2. SHOULDER SEASON (ID = 2, isSystem = true) =====
        // November to December & January to February: Moderate demand period
        SeasonData shoulderSeason = new SeasonData(
            "Shoulder Season",
            Season.SeasonType.SHOULDER_SEASON,
            "Moderate demand period between high and low seasons (November-December and January-February). " +
            "Good wildlife viewing with fewer crowds and moderate pricing. Ideal for value-conscious travelers " +
            "seeking quality safari experience without peak season crowds.",
            true , // isSystem = true
            true  // isActive = true
        );
        shoulderSeason.addPeriod(
            MonthDay.of(11, 1),  // November 1
            MonthDay.of(12, 31), // December 31
            null,                // Recurring annually
            "Shoulder season - short rains period. Lower crowds, good wildlife viewing, moderate pricing."
        );
        shoulderSeason.addPeriod(
            MonthDay.of(1, 1),   // January 1
            MonthDay.of(2, 28),  // February 28
            null,                // Recurring annually
            "Shoulder season - calving period in southern Serengeti. Excellent wildlife viewing with moderate crowds."
        );
        seasons.add(shoulderSeason);

        // ===== 3. LOW SEASON (ID = 3, isSystem = true) =====
        // March to May: Long rains, lowest prices, fewer tourists
        SeasonData lowSeason = new SeasonData(
            "Low Season",
            Season.SeasonType.LOW_SEASON,
            "Long rains season (March-May) offering the lowest rates and smallest crowds of the year. " +
            "Heaviest rainfall period with some lodges closing. However, lush green landscapes, excellent " +
            "bird watching, and dramatic storm photography. Budget-friendly with rates 30-50% lower than high season.",
            true , // isSystem = true
            true  // isActive = true
        );
        lowSeason.addPeriod(
            MonthDay.of(3, 1),   // March 1
            MonthDay.of(5, 31),  // May 31
            null,                // Recurring annually
            "Long rains - lowest tourism season. Substantial discounts on accommodation. Lush vegetation, " +
            "dramatic skies, excellent birding."
        );
        seasons.add(lowSeason);

        // ========== OTHER GLOBAL SEASONS (isSystem = false) ==========
        // These are more specific/detailed seasons for advanced use

        // ===== PEAK SEASON (Dry Season - Best Wildlife Viewing) =====
        SeasonData peakSeason = new SeasonData(
            "Peak Season - Dry Season",
            Season.SeasonType.PEAK_SEASON,
            "Tanzania's peak tourism season during the dry months (June-October). Best time for wildlife viewing " +
            "as animals gather around water sources. Clear skies, minimal rain, and excellent game drives. " +
            "Highest prices and largest crowds. Prime time for Serengeti safaris and Ngorongoro Crater visits. " +
            "TANAPA park entrance fees at premium rate (USD 70).",
            false,  // isSystem = false
            false  // isActive = false
        );
        peakSeason.addPeriod(
            MonthDay.of(6, 1),   // June 1
            MonthDay.of(10, 31), // October 31
            null,                // Recurring annually
            "Annual dry season - peak wildlife viewing period. Clear weather, large animal congregations at " +
            "water sources, best photography conditions. Serengeti and Ngorongoro at their finest."
        );
        seasons.add(peakSeason);

        // ===== MIGRATION SEASON (Wildebeest River Crossings) =====
        // July to September: Best time to see the Great Migration river crossings
        SeasonData migrationSeason = new SeasonData(
            "Great Migration Season",
            Season.SeasonType.SPECIAL_EVENT,
            "The spectacular Great Wildebeest Migration river crossings in the northern Serengeti (July-September). " +
            "Over 2 million wildebeest and zebra cross the Mara River, providing dramatic wildlife spectacles. " +
            "Peak season for Serengeti northern circuit. High demand period with premium pricing. " +
            "Best months for witnessing one of nature's greatest shows.",
            false,  // isSystem = false
            false  // isActive = false
        );
        migrationSeason.addPeriod(
            MonthDay.of(7, 1),   // July 1
            MonthDay.of(9, 30),  // September 30
            null,                // Recurring annually
            "Wildebeest migration river crossings at Mara River. Spectacular wildlife viewing with dramatic " +
            "predator-prey interactions. Northern Serengeti at peak activity. Book well in advance."
        );
        seasons.add(migrationSeason);

        // ===== CALVING SEASON (Wildebeest Births) =====
        // January to February: Wildebeest calving season in southern Serengeti
        SeasonData calvingSeason = new SeasonData(
            "Calving Season - Ndutu",
            Season.SeasonType.SPECIAL_EVENT,
            "Wildebeest calving season in southern Serengeti and Ndutu area (January-February). " +
            "Over 400,000 calves born within a short period, attracting predators and creating incredible " +
            "photography opportunities. Excellent wildlife viewing with fewer crowds than peak season. " +
            "Ideal weather conditions - hot but dry. Moderate pricing. Perfect for wildlife photographers.",
            false,  // isSystem = false
            false  // isActive = false
        );
        calvingSeason.addPeriod(
            MonthDay.of(1, 1),   // January 1
            MonthDay.of(2, 28),  // February 28
            null,                // Recurring annually
            "Annual calving season in Ndutu area and southern Serengeti. Newborn wildebeest calves, active " +
            "predators, exceptional photography. Less crowded than peak season with excellent wildlife action."
        );
        seasons.add(calvingSeason);

        // ===== SHOULDER SEASON - SHORT RAINS (Green Season Start) =====
        // November to December: Short rains, lower crowds, good wildlife viewing
        SeasonData shortRainsSeason = new SeasonData(
            "Shoulder Season - Short Rains",
            Season.SeasonType.SHOULDER_SEASON,
            "Short rains period (November-December) marking the start of the green season. " +
            "Dramatically fewer tourists, significantly lower accommodation rates, and still good wildlife viewing. " +
            "Intermittent rain showers, lush green landscapes, and migrating birds arrive. " +
            "Excellent value for budget-conscious travelers. TANAPA reduced entrance fees. " +
            "Perfect for photographers seeking dramatic skies and fewer safari vehicles.",
            false,  // isSystem = false
            false  // isActive = false
        );
        shortRainsSeason.addPeriod(
            MonthDay.of(11, 1),  // November 1
            MonthDay.of(12, 14), // December 14 (before festive season)
            null,                // Recurring annually
            "Short rains season with lower visitor numbers and reduced rates. Green landscapes, good wildlife " +
            "viewing, fewer crowds. Ideal for budget travelers and those seeking authentic safari experience."
        );
        seasons.add(shortRainsSeason);

        // ===== FESTIVE SEASON (Christmas & New Year) =====
        // December 15 to January 7: Holiday season with premium pricing
        SeasonData festiveSeason = new SeasonData(
            "Festive Season - Holidays",
            Season.SeasonType.FESTIVE_SEASON,
            "Premium holiday period covering Christmas and New Year (mid-December to early January). " +
            "Peak pricing across all accommodations and services. High international visitor numbers, " +
            "particularly families. Excellent weather conditions coinciding with wildebeest migration in southern " +
            "Serengeti. Book 6-12 months in advance. Premium rates but guaranteed good weather and wildlife viewing.",
            false,  // isSystem = false
            false  // isActive = false
        );
        festiveSeason.addPeriod(
            MonthDay.of(12, 15), // December 15
            MonthDay.of(1, 7),   // January 7 (year-wrapping period)
            null,                // Recurring annually
            "Christmas and New Year holiday period. Premium pricing, high demand, excellent weather. " +
            "Southern Serengeti migration viewing. Advance booking essential. Year-wrapping festive period."
        );
        seasons.add(festiveSeason);

        // ===== LOW SEASON - LONG RAINS (Green Season) =====
        // March to May: Long rains, lowest prices, fewer tourists
        SeasonData longRainsSeason = new SeasonData(
            "Low Season - Long Rains",
            Season.SeasonType.LOW_SEASON,
            "Long rains season (March-May) offering the lowest rates and smallest crowds of the year. " +
            "Heaviest rainfall period with some lodges closing and muddy roads. However, lush green landscapes, " +
            "excellent bird watching, and dramatic storm photography. Wildlife still visible but requires more effort. " +
            "Budget-friendly with rates 30-50% lower than peak season. April-May sees substantially discounted rates. " +
            "TANAPA reduced entrance fees (USD 50-60). Not recommended for first-time visitors.",
            false,  // isSystem = false
            false  // isActive = false
        );
        longRainsSeason.addPeriod(
            MonthDay.of(3, 1),   // March 1
            MonthDay.of(5, 31),  // May 31
            null,                // Recurring annually
            "Long rains - lowest tourism season. Substantial discounts on accommodation (April-May best deals). " +
            "Challenging road conditions but rewarding for experienced travelers. Lush vegetation, dramatic skies, " +
            "excellent birding. Some camps may be closed."
        );
        seasons.add(longRainsSeason);

        // ===== HIGH SEASON - NORTHERN PARKS =====
        // June to October: Specific high season designation for northern circuit
        SeasonData northernHighSeason = new SeasonData(
            "High Season - Northern Circuit",
            Season.SeasonType.HIGH_SEASON,
            "High season specifically for Tanzania's northern safari circuit including Serengeti, Ngorongoro Crater, " +
            "Tarangire, and Lake Manyara (June-October). Dry season with optimal wildlife viewing conditions. " +
            "Clear sunny days, cool nights, minimal rainfall. Animals concentrated around water sources. " +
            "Expect crowds in popular areas like Ngorongoro Crater and northern Serengeti (July especially). " +
            "Premium rates but guaranteed excellent game viewing. TANAPA fees at USD 70.",
            false,  // isSystem = false
            false  // isActive = false
        );
        northernHighSeason.addPeriod(
            MonthDay.of(6, 1),   // June 1
            MonthDay.of(10, 31), // October 31
            null,                // Recurring annually
            "Northern circuit high season - Serengeti, Ngorongoro, Tarangire, Lake Manyara. Dry conditions, " +
            "concentrated wildlife, busy in popular areas (especially Ngorongoro Crater and northern Serengeti in July)."
        );
        seasons.add(northernHighSeason);

        // ===== ZANZIBAR BEACH SEASON - HIGH =====
        // June to October & December to February: Best beach weather
        SeasonData zanzibarHighSeason = new SeasonData(
            "Zanzibar Beach High Season",
            Season.SeasonType.HIGH_SEASON,
            "Prime beach season for Zanzibar, Pemba, and Tanzania's coastal areas. Dry, sunny weather with calm seas " +
            "ideal for diving, snorkeling, and beach activities. Two peak periods: June-October (dry season) and " +
            "December-February (hot and dry). Minimal rainfall, excellent visibility for underwater activities. " +
            "Peak pricing for beach resorts and hotels. Perfect for post-safari beach extensions.",
            false,  // isSystem = false
            false  // isActive = false
        );
        // First period: June to October
        zanzibarHighSeason.addPeriod(
            MonthDay.of(6, 1),   // June 1
            MonthDay.of(10, 31), // October 31
            null,                // Recurring annually
            "Zanzibar dry season - calm seas, excellent diving and snorkeling, minimal rain, perfect beach weather."
        );
        // Second period: December to February
        zanzibarHighSeason.addPeriod(
            MonthDay.of(12, 1),  // December 1
            MonthDay.of(2, 28),  // February 28
            null,                // Recurring annually
            "Zanzibar hot dry season - ideal beach weather, excellent water sports, peak tourism period."
        );
        seasons.add(zanzibarHighSeason);

        // ===== ZANZIBAR BEACH SEASON - LOW =====
        // March to May: Rainy season for coastal areas
        SeasonData zanzibarLowSeason = new SeasonData(
            "Zanzibar Beach Low Season",
            Season.SeasonType.LOW_SEASON,
            "Low season for Zanzibar and coastal Tanzania during long rains (March-May). Heavy rainfall, high humidity, " +
            "rough seas. Many beach resorts offer significant discounts (30-50% off peak rates). Some properties may close. " +
            "Not ideal for diving/snorkeling due to reduced visibility. Good for budget travelers willing to accept " +
            "rain interruptions. Lush vegetation and fewer tourists. April-May sees the lowest rates.",
            false,  // isSystem = false
            false  // isActive = false
        );
        zanzibarLowSeason.addPeriod(
            MonthDay.of(3, 1),   // March 1
            MonthDay.of(5, 31),  // May 31
            null,                // Recurring annually
            "Zanzibar long rains - lowest beach season rates, heavy rainfall, rough seas. Significant discounts but " +
            "expect weather interruptions. Some resorts closed. April-May best for deals."
        );
        seasons.add(zanzibarLowSeason);

        return seasons;
    }

    /**
     * Helper class to structure season data during initialization
     */
    private static class SeasonData {
        String name;
        Season.SeasonType seasonType;
        String description;
        boolean isSystem;
        boolean isActive;
        List<PeriodData> periods;

        SeasonData(String name, Season.SeasonType seasonType, String description, boolean isSystem, boolean isActive) {
            this.name = name;
            this.seasonType = seasonType;
            this.description = description;
            this.isSystem = isSystem;
            this.isActive = isActive;
            this.periods = new ArrayList<>();
        }

        void addPeriod(MonthDay startDate, MonthDay endDate, Integer year, String notes) {
            periods.add(new PeriodData(startDate, endDate, year, notes));
        }
    }

    /**
     * Helper class to structure period data during initialization
     */
    private static class PeriodData {
        MonthDay startDate;
        MonthDay endDate;
        Integer year;  // null for recurring annual periods
        String notes;

        PeriodData(MonthDay startDate, MonthDay endDate, Integer year, String notes) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.year = year;
            this.notes = notes;
        }
    }
}
