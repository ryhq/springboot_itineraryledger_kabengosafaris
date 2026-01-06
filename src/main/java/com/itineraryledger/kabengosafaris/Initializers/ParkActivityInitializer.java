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
     * Format: Map<ParkID, List<ActivityIDs>>
     *
     * Data extracted from INSERT INTO `park_activity` statements in production SQL
     */
    private Map<Long, List<Long>> getParkActivityMappings() {
        Map<Long, List<Long>> mappings = new HashMap<>();

        // Park 1: Serengeti National Park
        // Activities: Entrance, Walking Safari, Ranger, Bush Meals, Filming, Others
        mappings.put(1L, Arrays.asList(1L, 2L, 3L, 4L, 8L, 9L));

        // Park 2: Ngorongoro Conservation Area
        // Activities: Entrance, Bush Meals, Horse Riding, Crater Service, Sun Down, Hot Air Balloon, Olduvai Gorge, Shifting Sand, Laetoli Footprints
        mappings.put(2L, Arrays.asList(1L, 4L, 11L, 15L, 16L, 17L, 18L, 19L, 20L));

        // Park 3: Tarangire National Park
        // Activities: Entrance, Walking Safari, Ranger, Bush Meals, Night Game Drive, Filming, Others
        mappings.put(3L, Arrays.asList(1L, 2L, 3L, 4L, 6L, 8L, 9L));

        // Park 4: Lake Manyara National Park
        // Activities: Entrance, Walking Safari, Ranger, Bush Meals, Canopy Walk, Night Game Drive, Canoeing, Filming, Others, Sport Fishing
        mappings.put(4L, Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L));

        // Park 5: Ruaha National Park
        // Activities: Entrance, Walking Safari, Ranger, Bush Meals, Night Game Drive, Filming, Others, Sport Fishing, Boating
        mappings.put(5L, Arrays.asList(1L, 2L, 3L, 4L, 6L, 8L, 9L, 10L, 13L));

        // Park 6: Mikumi National Park
        // Activities: Entrance, Walking Safari, Ranger, Bush Meals, Night Game Drive, Filming, Others
        mappings.put(6L, Arrays.asList(1L, 2L, 3L, 4L, 6L, 8L, 9L));

        // Park 7: Katavi National Park
        // Activities: Entrance, Walking Safari, Ranger, Bush Meals, Night Game Drive, Filming, Others
        mappings.put(7L, Arrays.asList(1L, 2L, 3L, 4L, 6L, 8L, 9L));

        // Park 8: Selous Game Reserve - SKIPPED (No mappings in production database)

        // Park 9: Arusha National Park
        // Activities: Entrance, Walking Safari, Ranger, Bush Meals, Canoeing, Filming, Others, Horse Riding, Cycling
        mappings.put(9L, Arrays.asList(1L, 2L, 3L, 4L, 7L, 8L, 9L, 11L, 12L));

        // Park 10: Gombe National Park
        // Activities: Entrance, Walking Safari, Ranger, Bush Meals, Filming, Others, Sport Fishing, Boating
        mappings.put(10L, Arrays.asList(1L, 2L, 3L, 4L, 8L, 9L, 10L, 13L));

        // Park 11: Burigi-Chato National Park
        // Activities: Entrance, Walking Safari, Ranger, Bush Meals, Night Game Drive, Canoeing, Filming, Others, Sport Fishing, Boating
        mappings.put(11L, Arrays.asList(1L, 2L, 3L, 4L, 6L, 7L, 8L, 9L, 10L, 13L));

        // Park 12: Kilimanjaro National Park
        // Activities: Entrance, Cycling
        mappings.put(12L, Arrays.asList(1L, 12L));

        // Park 13: Mahale Mountains National Park
        // Activities: Entrance, Walking Safari, Ranger, Bush Meals, Canoeing, Filming, Others, Sport Fishing, Boating
        mappings.put(13L, Arrays.asList(1L, 2L, 3L, 4L, 7L, 8L, 9L, 10L, 13L));

        // Park 14: Mkomazi National Park
        // Activities: Entrance, Walking Safari, Ranger, Bush Meals, Night Game Drive, Filming, Others, Rhino Viewing
        mappings.put(14L, Arrays.asList(1L, 2L, 3L, 4L, 6L, 8L, 9L, 14L));

        // Park 15: Rubondo Island National Park
        // Activities: Entrance, Walking Safari, Ranger, Bush Meals, Canoeing, Filming, Others, Sport Fishing, Boating
        mappings.put(15L, Arrays.asList(1L, 2L, 3L, 4L, 7L, 8L, 9L, 10L, 13L));

        // Park 16: Saadani National Park
        // Activities: Entrance, Walking Safari, Ranger, Bush Meals, Night Game Drive, Filming, Others, Sport Fishing, Cycling, Boating
        mappings.put(16L, Arrays.asList(1L, 2L, 3L, 4L, 6L, 8L, 9L, 10L, 12L, 13L));

        // Park 17: Udzungwa Mountains National Park
        // Activities: Entrance, Walking Safari, Ranger, Bush Meals, Filming, Others
        mappings.put(17L, Arrays.asList(1L, 2L, 3L, 4L, 8L, 9L));

        // Park 18: Saanane National Park
        // Activities: Entrance, Walking Safari, Ranger, Bush Meals, Filming, Others, Sport Fishing
        mappings.put(18L, Arrays.asList(1L, 2L, 3L, 4L, 8L, 9L, 10L));

        // Park 19: Ibanda-Kyerwa National Park
        // Activities: Entrance, Walking Safari, Ranger, Bush Meals, Night Game Drive, Filming, Others, Sport Fishing
        mappings.put(19L, Arrays.asList(1L, 2L, 3L, 4L, 6L, 8L, 9L, 10L));

        // Park 20: Kitulo National Park
        // Activities: Entrance, Walking Safari, Ranger, Bush Meals, Filming, Others
        mappings.put(20L, Arrays.asList(1L, 2L, 3L, 4L, 8L, 9L));

        // Park 21: Nyerere National Park
        // Activities: Entrance, Walking Safari, Ranger, Bush Meals, Night Game Drive, Canoeing, Filming, Others, Sport Fishing, Boating
        mappings.put(21L, Arrays.asList(1L, 2L, 3L, 4L, 6L, 7L, 8L, 9L, 10L, 13L));

        // Park 22: Rumanyika-Karagwe National Park
        // Activities: Entrance, Walking Safari, Ranger, Bush Meals, Canoeing, Filming, Others, Sport Fishing, Boating
        mappings.put(22L, Arrays.asList(1L, 2L, 3L, 4L, 7L, 8L, 9L, 10L, 13L));

        // Park 23: Ugalla River National Park
        // Activities: Entrance, Walking Safari, Ranger, Bush Meals, Night Game Drive, Filming, Others, Sport Fishing, Cycling, Boating
        mappings.put(23L, Arrays.asList(1L, 2L, 3L, 4L, 6L, 8L, 9L, 10L, 12L, 13L));

        return mappings;
    }
}
