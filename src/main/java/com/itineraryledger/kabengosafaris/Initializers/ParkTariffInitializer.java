package com.itineraryledger.kabengosafaris.Initializers;

import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariff;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariffRepository;
import com.itineraryledger.kabengosafaris.Tariff.Repositories.TariffRepository;
import com.itineraryledger.kabengosafaris.Tariff.Tariff;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * ParkTariffInitializer - Initializes Park-Tariff relationships at application startup
 *
 * IMPORTANT: This initializer creates the many-to-many relationships between Parks and Tariffs.
 *
 * Runs AFTER TariffInitializer (Order = HIGHEST_PRECEDENCE + 12)
 *
 * Tariff IDs (from TariffInitializer):
 * 1. Public Camping Fee, 2. Special Camping Fee, 3. Seasonal Camping Fee,
 * 4. Fly Campsites Fee, 5. Upper Barafu Camping Fee, 6. Premium Special Camping Fee,
 * 7. Parks Accommodations Fee, 8. Rescue Fee, 9. Concession Fee,
 * 10. Hiking / Mountaineering Fee, 11. WMA
 *
 * Behavior:
 * - Only creates relationships that don't already exist
 * - Does not update or delete existing relationships
 * - Skips relationships if either park or tariff is missing
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 13) // Run after TariffInitializer
public class ParkTariffInitializer implements ApplicationRunner {

    private final ParkRepository parkRepository;
    private final TariffRepository tariffRepository;
    private final ParkTariffRepository parkTariffRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        printStartBanner();
        boolean success = true;

        try {
            initializeParkTariffs();
        } catch (Exception e) {
            log.error("Error during park-tariff relationship initialization: {}", e.getMessage(), e);
            success = false;
        } finally {
            printEndBanner(success);
        }
    }

    private void printStartBanner() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════════╗");
        log.info("║                                                                    ║");
        log.info("║          PARK-TARIFF INITIALIZER - START                           ║");
        log.info("║                                                                    ║");
        log.info("╚════════════════════════════════════════════════════════════════════╝");
        log.info("");
    }

    private void printEndBanner(boolean success) {
        log.info("");
        if (success) {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║        ✓ PARK-TARIFF INITIALIZER - COMPLETED                       ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        } else {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║        ✗ PARK-TARIFF INITIALIZER - FAILED                          ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        }
        log.info("");
    }

    private void initializeParkTariffs() {
        log.info("Starting Park-Tariff relationship initialization...");

        // Get all parks and tariffs from database
        List<Park> parks = parkRepository.findAll();
        List<Tariff> tariffs = tariffRepository.findAll();

        if (parks.isEmpty()) {
            log.warn("No parks found in database. Skipping park-tariff initialization.");
            log.warn("Please ensure ParkInitializer has run successfully first.");
            return;
        }

        if (tariffs.isEmpty()) {
            log.warn("No tariffs found in database. Skipping park-tariff initialization.");
            log.warn("Please ensure TariffInitializer has run successfully first.");
            return;
        }

        // Create maps for quick lookup
        Map<Long, Park> parkMap = new HashMap<>();
        Map<Long, Tariff> tariffMap = new HashMap<>();

        for (Park park : parks) {
            parkMap.put(park.getId(), park);
        }

        for (Tariff tariff : tariffs) {
            tariffMap.put(tariff.getId(), tariff);
        }

        log.info("Found {} parks and {} tariffs in database", parks.size(), tariffs.size());

        // Get park-tariff mappings from production database
        Map<Long, List<Long>> parkTariffMappings = getParkTariffMappings();

        int createdCount = 0;
        int skippedCount = 0;
        int errorCount = 0;

        for (Map.Entry<Long, List<Long>> entry : parkTariffMappings.entrySet()) {
            Long parkId = entry.getKey();
            List<Long> tariffIds = entry.getValue();

            Park park = parkMap.get(parkId);
            if (park == null) {
                log.warn("Park with ID {} not found in database. Skipping {} tariff mappings.", parkId, tariffIds.size());
                errorCount += tariffIds.size();
                continue;
            }

            for (Long tariffId : tariffIds) {
                Tariff tariff = tariffMap.get(tariffId);
                if (tariff == null) {
                    log.warn("Tariff with ID {} not found in database for park '{}'. Skipping.", tariffId, park.getName());
                    errorCount++;
                    continue;
                }

                // Check if relationship already exists
                if (parkTariffRepository.existsByParkIdAndTariffId(parkId, tariffId)) {
                    log.debug("⊘ Relationship already exists: {} <-> {}", park.getName(), tariff.getName());
                    skippedCount++;
                    continue;
                }

                // Create new relationship
                ParkTariff parkTariff = ParkTariff.builder()
                        .park(park)
                        .tariff(tariff)
                        .notes(null)
                        .build();

                parkTariffRepository.save(parkTariff);
                log.info("✓ Created relationship: {} <-> {}", park.getName(), tariff.getName());
                createdCount++;
            }
        }

        log.info("");
        log.info("Park-Tariff relationship initialization complete:");
        log.info("  - Created: {} relationships", createdCount);
        log.info("  - Skipped: {} existing relationships", skippedCount);
        log.info("  - Errors: {} (missing parks or tariffs)", errorCount);
        log.info("  - Total: {} relationships in system", parkTariffRepository.count());
    }

    /**
     * Get park-tariff mappings from production database (springboot_itineraryledger_kabengosafaris.sql)
     *
     * IMPORTANT: These mappings match the production database exactly.
     * DO NOT modify unless the production database has been updated!
     *
     * Format: Map<ParkID, List<TariffIDs>>
     *
     * Tariff IDs reference:
     * 1. Public Camping Fee
     * 2. Special Camping Fee
     * 3. Seasonal Camping Fee
     * 4. Fly Campsites Fee
     * 5. Upper Barafu Camping Fee
     * 6. Premium Special Camping Fee
     * 7. Parks Accommodations Fee
     * 8. Rescue Fee
     * 9. Concession Fee
     * 10. Hiking / Mountaineering Fee
     * 11. WMA (Wildlife Management Area)
     *
     * Data extracted from INSERT INTO `parks_tariffs` statements in production SQL
     */
    private Map<Long, List<Long>> getParkTariffMappings() {
        Map<Long, List<Long>> mappings = new HashMap<>();

        // Park 1: Serengeti National Park
        // Tariffs: Public Camping, Special Camping, Seasonal Camping, Fly Campsites, Premium Special Camping, Concession, WMA
        mappings.put(1L, Arrays.asList(1L, 2L, 3L, 4L, 6L, 9L, 11L));

        // Park 2: Ngorongoro Conservation Area
        // Tariffs: Public Camping, Special Camping, Seasonal Camping, Fly Campsites, Concession
        mappings.put(2L, Arrays.asList(1L, 2L, 3L, 4L, 9L));

        // Park 3: Tarangire National Park
        // Tariffs: Public Camping, Special Camping, Seasonal Camping, Fly Campsites, Concession, WMA
        mappings.put(3L, Arrays.asList(1L, 2L, 3L, 4L, 9L, 11L));

        // Park 4: Lake Manyara National Park
        // Tariffs: Public Camping, Special Camping, Seasonal Camping, Fly Campsites, Concession, WMA
        mappings.put(4L, Arrays.asList(1L, 2L, 3L, 4L, 9L, 11L));

        // Park 5: Ruaha National Park
        // Tariffs: Public Camping, Special Camping, Seasonal Camping, Fly Campsites, Concession
        mappings.put(5L, Arrays.asList(1L, 2L, 3L, 4L, 9L));

        // Park 6: Mikumi National Park
        // Tariffs: Public Camping, Special Camping, Seasonal Camping, Fly Campsites, Concession
        mappings.put(6L, Arrays.asList(1L, 2L, 3L, 4L, 9L));

        // Park 7: Katavi National Park
        // Tariffs: Public Camping, Special Camping, Seasonal Camping, Fly Campsites, Concession
        mappings.put(7L, Arrays.asList(1L, 2L, 3L, 4L, 9L));

        // Park 8: Selous Game Reserve
        // Tariffs: Public Camping, Special Camping, Seasonal Camping, Fly Campsites
        mappings.put(8L, Arrays.asList(1L, 2L, 3L, 4L));

        // Park 9: Arusha National Park
        // Tariffs: Public Camping, Special Camping, Seasonal Camping, Fly Campsites, Concession
        mappings.put(9L, Arrays.asList(1L, 2L, 3L, 4L, 9L));

        // Park 10: Gombe Stream National Park
        // Tariffs: Public Camping, Special Camping, Seasonal Camping, Fly Campsites, Concession
        mappings.put(10L, Arrays.asList(1L, 2L, 3L, 4L, 9L));

        // Park 11: Burigi-Chato National Park
        // Tariffs: Public Camping, Special Camping, Seasonal Camping, Fly Campsites, Concession
        mappings.put(11L, Arrays.asList(1L, 2L, 3L, 4L, 9L));

        // Park 12: Kilimanjaro National Park
        // Tariffs: Public Camping, Special Camping, Seasonal Camping, Fly Campsites, Upper Barafu Camping, Parks Accommodations, Rescue, Concession
        mappings.put(12L, Arrays.asList(1L, 2L, 3L, 4L, 5L, 7L, 8L, 9L));

        // Park 13: Mahale Mountains National Park
        // Tariffs: Public Camping, Special Camping, Seasonal Camping, Fly Campsites, Concession
        mappings.put(13L, Arrays.asList(1L, 2L, 3L, 4L, 9L));

        // Park 14: Mkomazi National Park
        // Tariffs: Public Camping, Special Camping, Seasonal Camping, Fly Campsites, Concession
        mappings.put(14L, Arrays.asList(1L, 2L, 3L, 4L, 9L));

        // Park 15: Rubondo Island National Park
        // Tariffs: Public Camping, Special Camping, Seasonal Camping, Fly Campsites, Concession
        mappings.put(15L, Arrays.asList(1L, 2L, 3L, 4L, 9L));

        // Park 16: Saadani National Park
        // Tariffs: Public Camping, Special Camping, Seasonal Camping, Fly Campsites, Concession
        mappings.put(16L, Arrays.asList(1L, 2L, 3L, 4L, 9L));

        // Park 17: Udzungwa Mountains National Park
        // Tariffs: Public Camping, Special Camping, Seasonal Camping, Fly Campsites, Concession
        mappings.put(17L, Arrays.asList(1L, 2L, 3L, 4L, 9L));

        // Park 18: Saanane Island National Park
        // Tariffs: Public Camping, Special Camping, Seasonal Camping, Fly Campsites, Concession
        mappings.put(18L, Arrays.asList(1L, 2L, 3L, 4L, 9L));

        // Park 19: Ibanda-Kyerwa National Park
        // Tariffs: Public Camping, Special Camping, Seasonal Camping, Fly Campsites, Concession
        mappings.put(19L, Arrays.asList(1L, 2L, 3L, 4L, 9L));

        // Park 20: Kitulo National Park
        // Tariffs: Public Camping, Special Camping, Seasonal Camping, Fly Campsites, Concession
        mappings.put(20L, Arrays.asList(1L, 2L, 3L, 4L, 9L));

        // Park 21: Nyerere National Park
        // Tariffs: Public Camping, Special Camping, Seasonal Camping, Fly Campsites, Concession, Hiking / Mountaineering
        mappings.put(21L, Arrays.asList(1L, 2L, 3L, 4L, 9L, 10L));

        // Park 22: Rumanyika-Karagwe National Park
        // Tariffs: Public Camping, Special Camping, Seasonal Camping, Fly Campsites, Concession
        mappings.put(22L, Arrays.asList(1L, 2L, 3L, 4L, 9L));

        // Park 23: Ugalla River National Park
        // Tariffs: Public Camping, Special Camping, Seasonal Camping, Fly Campsites, Hiking / Mountaineering
        mappings.put(23L, Arrays.asList(1L, 2L, 3L, 4L, 10L));

        return mappings;
    }
}
