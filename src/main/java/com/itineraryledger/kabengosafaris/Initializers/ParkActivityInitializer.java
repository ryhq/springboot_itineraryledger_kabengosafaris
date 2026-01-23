package com.itineraryledger.kabengosafaris.Initializers;

import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivity;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivityRepository;
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
 * ParkActivityInitializer - Initializes Park-Activity relationships at application startup
 *
 * IMPORTANT: This initializer creates the many-to-many relationships between Parks and Activities.
 *
 * NOTE: "Entrance" (formerly Activity ID 1) was moved to TariffInitializer as "Conservation Fee"
 * (Tariff ID 1). All activity IDs have been shifted down by 1.
 *
 * Activity IDs (shifted - "Entrance" removed):
 * 1. Walking Safari, 2. Ranger, 3. Bush Meals, 4. Canopy Walk,
 * 5. Night Game Drive, 6. Canoeing, 7. Filming, 8. Others, 9. Sport Fishing,
 * 10. Horse Riding, 11. Cycling, 12. Boating, 13. Rhino Viewing, 14. Crater Service,
 * 15. Sun Down, 16. Hot Air Balloon, 17. Olduvai Gorge, 18. Shifting Sand, 19. Laetoli Footprints,
 * 20. Guided Lectures, 21. Bird Watching, 22. Cultural Tours, 23. Photography Safaris,
 * 24. Rock Climbing, 25. Wildlife Tracking
 *
 * Runs AFTER both ParkInitializer and ActivityInitializer (Order = HIGHEST_PRECEDENCE + 8)
 *
 * Behavior:
 * - Only creates relationships that don't already exist
 * - Does not update or delete existing relationships
 * - Skips relationships if either park or activity is missing
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 8) // Run after ParkInitializer and ActivityInitializer
public class ParkActivityInitializer implements ApplicationRunner {

    private final ParkRepository parkRepository;
    private final ActivityRepository activityRepository;
    private final ParkActivityRepository parkActivityRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        printStartBanner();
        boolean success = true;

        try {
            initializeParkActivities();
        } catch (Exception e) {
            log.error("Error during park-activity relationship initialization: {}", e.getMessage(), e);
            success = false;
        } finally {
            printEndBanner(success);
        }
    }

    private void printStartBanner() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════════╗");
        log.info("║                                                                    ║");
        log.info("║          PARK-ACTIVITY INITIALIZER - START                         ║");
        log.info("║                                                                    ║");
        log.info("╚════════════════════════════════════════════════════════════════════╝");
        log.info("");
    }

    private void printEndBanner(boolean success) {
        log.info("");
        if (success) {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║        ✓ PARK-ACTIVITY INITIALIZER - COMPLETED                     ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        } else {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║        ✗ PARK-ACTIVITY INITIALIZER - FAILED                        ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        }
        log.info("");
    }

    private void initializeParkActivities() {
        log.info("Starting Park-Activity relationship initialization...");

        // Get all parks and activities from database
        List<Park> parks = parkRepository.findAll();
        List<Activity> activities = activityRepository.findAll();

        if (parks.isEmpty()) {
            log.warn("No parks found in database. Skipping park-activity initialization.");
            log.warn("Please ensure ParkInitializer has run successfully first.");
            return;
        }

        if (activities.isEmpty()) {
            log.warn("No activities found in database. Skipping park-activity initialization.");
            log.warn("Please ensure ActivityInitializer has run successfully first.");
            return;
        }

        // Create maps for quick lookup
        Map<Long, Park> parkMap = new HashMap<>();
        Map<Long, Activity> activityMap = new HashMap<>();

        for (Park park : parks) {
            parkMap.put(park.getId(), park);
        }

        for (Activity activity : activities) {
            activityMap.put(activity.getId(), activity);
        }

        log.info("Found {} parks and {} activities in database", parks.size(), activities.size());

        // Get park-activity mappings from production database
        Map<Long, List<Long>> parkActivityMappings = getParkActivityMappings();

        int createdCount = 0;
        int skippedCount = 0;
        int errorCount = 0;

        for (Map.Entry<Long, List<Long>> entry : parkActivityMappings.entrySet()) {
            Long parkId = entry.getKey();
            List<Long> activityIds = entry.getValue();

            Park park = parkMap.get(parkId);
            if (park == null) {
                log.warn("Park with ID {} not found in database. Skipping {} activity mappings.", parkId, activityIds.size());
                errorCount += activityIds.size();
                continue;
            }

            for (Long activityId : activityIds) {
                Activity activity = activityMap.get(activityId);
                if (activity == null) {
                    log.warn("Activity with ID {} not found in database for park '{}'. Skipping.", activityId, park.getName());
                    errorCount++;
                    continue;
                }

                // Check if relationship already exists
                if (parkActivityRepository.existsByParkIdAndActivityId(parkId, activityId)) {
                    log.debug("⊘ Relationship already exists: {} <-> {}", park.getName(), activity.getName());
                    skippedCount++;
                    continue;
                }

                // Create new relationship
                ParkActivity parkActivity = ParkActivity.builder()
                        .park(park)
                        .activity(activity)
                        .notes(null)
                        .build();

                parkActivityRepository.save(parkActivity);
                log.info("✓ Created relationship: {} <-> {}", park.getName(), activity.getName());
                createdCount++;
            }
        }

        log.info("");
        log.info("Park-Activity relationship initialization complete:");
        log.info("  - Created: {} relationships", createdCount);
        log.info("  - Skipped: {} existing relationships", skippedCount);
        log.info("  - Errors: {} (missing parks or activities)", errorCount);
        log.info("  - Total: {} relationships in system", parkActivityRepository.count());
    }

    /**
     * Get park-activity mappings from production database (idealafr_idealafricantravels.sql)
     *
     * IMPORTANT: These mappings match the production database exactly.
     * DO NOT modify unless the production database has been updated!
     *
     * NOTE: "Entrance" (Activity ID 1) was removed and moved to TariffInitializer as
     * "Conservation Fee" (Tariff ID 1). All activity IDs have been shifted down by 1.
     *
     * New Activity IDs (shifted):
     * 1. Walking Safari (was 2), 2. Ranger (was 3), 3. Bush Meals (was 4), 4. Canopy Walk (was 5),
     * 5. Night Game Drive (was 6), 6. Canoeing (was 7), 7. Filming (was 8), 8. Others (was 9),
     * 9. Sport Fishing (was 10), 10. Horse Riding (was 11), 11. Cycling (was 12), 12. Boating (was 13),
     * 13. Rhino Viewing (was 14), 14. Crater Service (was 15), 15. Sun Down (was 16),
     * 16. Hot Air Balloon (was 17), 17. Olduvai Gorge (was 18), 18. Shifting Sand (was 19),
     * 19. Laetoli Footprints (was 20), 20. Guided Lectures (was 21), 21. Bird Watching (was 22),
     * 22. Cultural Tours (was 23), 23. Photography Safaris (was 24), 24. Rock Climbing (was 25),
     * 25. Wildlife Tracking (was 26)
     *
     * Format: Map<ParkID, List<ActivityIDs>>
     *
     * Data extracted from INSERT INTO `park_activity` statements in production SQL
     */
    private Map<Long, List<Long>> getParkActivityMappings() {
        Map<Long, List<Long>> mappings = new HashMap<>();

        // Park 1: Serengeti National Park
        // Activities: Walking Safari, Ranger, Bush Meals, Filming, Others
        // (Entrance removed - now Conservation Fee tariff)
        mappings.put(1L, Arrays.asList(1L, 2L, 3L, 7L, 8L));

        // Park 2: Ngorongoro Conservation Area
        // Activities: Bush Meals, Horse Riding, Crater Service, Sun Down, Hot Air Balloon, Olduvai Gorge, Shifting Sand, Laetoli Footprints
        // (Entrance removed - now Conservation Fee tariff)
        mappings.put(2L, Arrays.asList(3L, 10L, 14L, 15L, 16L, 17L, 18L, 19L));

        // Park 3: Tarangire National Park
        // Activities: Walking Safari, Ranger, Bush Meals, Night Game Drive, Filming, Others
        // (Entrance removed - now Conservation Fee tariff)
        mappings.put(3L, Arrays.asList(1L, 2L, 3L, 5L, 7L, 8L));

        // Park 4: Lake Manyara National Park
        // Activities: Walking Safari, Ranger, Bush Meals, Canopy Walk, Night Game Drive, Canoeing, Filming, Others, Sport Fishing
        // (Entrance removed - now Conservation Fee tariff)
        mappings.put(4L, Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L));

        // Park 5: Ruaha National Park
        // Activities: Walking Safari, Ranger, Bush Meals, Night Game Drive, Filming, Others, Sport Fishing, Boating
        // (Entrance removed - now Conservation Fee tariff)
        mappings.put(5L, Arrays.asList(1L, 2L, 3L, 5L, 7L, 8L, 9L, 12L));

        // Park 6: Mikumi National Park
        // Activities: Walking Safari, Ranger, Bush Meals, Night Game Drive, Filming, Others
        // (Entrance removed - now Conservation Fee tariff)
        mappings.put(6L, Arrays.asList(1L, 2L, 3L, 5L, 7L, 8L));

        // Park 7: Katavi National Park
        // Activities: Walking Safari, Ranger, Bush Meals, Night Game Drive, Filming, Others
        // (Entrance removed - now Conservation Fee tariff)
        mappings.put(7L, Arrays.asList(1L, 2L, 3L, 5L, 7L, 8L));

        // Park 8: Selous Game Reserve - SKIPPED (No mappings in production database)

        // Park 9: Arusha National Park
        // Activities: Walking Safari, Ranger, Bush Meals, Canoeing, Filming, Others, Horse Riding, Cycling
        // (Entrance removed - now Conservation Fee tariff)
        mappings.put(9L, Arrays.asList(1L, 2L, 3L, 6L, 7L, 8L, 10L, 11L));

        // Park 10: Gombe National Park
        // Activities: Walking Safari, Ranger, Bush Meals, Filming, Others, Sport Fishing, Boating
        // (Entrance removed - now Conservation Fee tariff)
        mappings.put(10L, Arrays.asList(1L, 2L, 3L, 7L, 8L, 9L, 12L));

        // Park 11: Burigi-Chato National Park
        // Activities: Walking Safari, Ranger, Bush Meals, Night Game Drive, Canoeing, Filming, Others, Sport Fishing, Boating
        // (Entrance removed - now Conservation Fee tariff)
        mappings.put(11L, Arrays.asList(1L, 2L, 3L, 5L, 6L, 7L, 8L, 9L, 12L));

        // Park 12: Kilimanjaro National Park
        // Activities: Cycling
        // (Entrance removed - now Conservation Fee tariff)
        mappings.put(12L, Arrays.asList(11L));

        // Park 13: Mahale Mountains National Park
        // Activities: Walking Safari, Ranger, Bush Meals, Canoeing, Filming, Others, Sport Fishing, Boating
        // (Entrance removed - now Conservation Fee tariff)
        mappings.put(13L, Arrays.asList(1L, 2L, 3L, 6L, 7L, 8L, 9L, 12L));

        // Park 14: Mkomazi National Park
        // Activities: Walking Safari, Ranger, Bush Meals, Night Game Drive, Filming, Others, Rhino Viewing
        // (Entrance removed - now Conservation Fee tariff)
        mappings.put(14L, Arrays.asList(1L, 2L, 3L, 5L, 7L, 8L, 13L));

        // Park 15: Rubondo Island National Park
        // Activities: Walking Safari, Ranger, Bush Meals, Canoeing, Filming, Others, Sport Fishing, Boating
        // (Entrance removed - now Conservation Fee tariff)
        mappings.put(15L, Arrays.asList(1L, 2L, 3L, 6L, 7L, 8L, 9L, 12L));

        // Park 16: Saadani National Park
        // Activities: Walking Safari, Ranger, Bush Meals, Night Game Drive, Filming, Others, Sport Fishing, Cycling, Boating
        // (Entrance removed - now Conservation Fee tariff)
        mappings.put(16L, Arrays.asList(1L, 2L, 3L, 5L, 7L, 8L, 9L, 11L, 12L));

        // Park 17: Udzungwa Mountains National Park
        // Activities: Walking Safari, Ranger, Bush Meals, Filming, Others
        // (Entrance removed - now Conservation Fee tariff)
        mappings.put(17L, Arrays.asList(1L, 2L, 3L, 7L, 8L));

        // Park 18: Saanane National Park
        // Activities: Walking Safari, Ranger, Bush Meals, Filming, Others, Sport Fishing
        // (Entrance removed - now Conservation Fee tariff)
        mappings.put(18L, Arrays.asList(1L, 2L, 3L, 7L, 8L, 9L));

        // Park 19: Ibanda-Kyerwa National Park
        // Activities: Walking Safari, Ranger, Bush Meals, Night Game Drive, Filming, Others, Sport Fishing
        // (Entrance removed - now Conservation Fee tariff)
        mappings.put(19L, Arrays.asList(1L, 2L, 3L, 5L, 7L, 8L, 9L));

        // Park 20: Kitulo National Park
        // Activities: Walking Safari, Ranger, Bush Meals, Filming, Others
        // (Entrance removed - now Conservation Fee tariff)
        mappings.put(20L, Arrays.asList(1L, 2L, 3L, 7L, 8L));

        // Park 21: Nyerere National Park
        // Activities: Walking Safari, Ranger, Bush Meals, Night Game Drive, Canoeing, Filming, Others, Sport Fishing, Boating
        // (Entrance removed - now Conservation Fee tariff)
        mappings.put(21L, Arrays.asList(1L, 2L, 3L, 5L, 6L, 7L, 8L, 9L, 12L));

        // Park 22: Rumanyika-Karagwe National Park
        // Activities: Walking Safari, Ranger, Bush Meals, Canoeing, Filming, Others, Sport Fishing, Boating
        // (Entrance removed - now Conservation Fee tariff)
        mappings.put(22L, Arrays.asList(1L, 2L, 3L, 6L, 7L, 8L, 9L, 12L));

        // Park 23: Ugalla River National Park
        // Activities: Walking Safari, Ranger, Bush Meals, Night Game Drive, Filming, Others, Sport Fishing, Cycling, Boating
        // (Entrance removed - now Conservation Fee tariff)
        mappings.put(23L, Arrays.asList(1L, 2L, 3L, 5L, 7L, 8L, 9L, 11L, 12L));

        // Park 26: Kigosi National Park
        // Activities: Ranger, Night Game Drive, Filming, Sport Fishing, Cycling, Boating, Hot Air Balloon
        mappings.put(26L, Arrays.asList(2L, 5L, 7L, 9L, 11L, 12L, 16L));

        return mappings;
    }
}
