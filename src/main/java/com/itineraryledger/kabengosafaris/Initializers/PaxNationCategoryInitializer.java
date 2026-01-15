package com.itineraryledger.kabengosafaris.Initializers;

import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Repositories.PaxNationCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * PaxNationCategoryInitializer - Initializes passenger nationality categories at application startup
 *
 * Creates standard nationality categories used for pricing across accommodations, activities, and parks.
 * These categories ensure consistent nationality-based pricing throughout the system.
 *
 * Runs AFTER PaxAgeCategoryInitializer (Order = HIGHEST_PRECEDENCE + 20)
 *
 * Standard Nationality Categories:
 * 1. Resident (Priority 1): Tanzanian citizens and permanent residents - lowest rates
 * 2. Expatriate (Priority 2): Foreign nationals with work/residence permits - intermediate rates
 * 3. East African (Priority 3): Citizens of EAC member states - intermediate rates
 * 4. Non-Resident (Priority 4): International visitors - highest rates
 *
 * Priority Factor Usage:
 * - When ChargingBasis is PER_VEHICLE or PER_GROUP, the passenger with highest
 *   priorityFactor determines the rate category for the entire group
 * - Example: If a vehicle has both Residents (priority 1) and Non-Residents (priority 4),
 *   the Non-Resident rate would apply to the vehicle charge
 *
 * Behavior:
 * - Only creates categories that don't already exist (checked by name)
 * - All created categories are marked as system (isSystem = true, protected from deletion)
 * - Does not update or delete existing categories
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 11) // Run after PaxAgeCategoryInitializer
public class PaxNationCategoryInitializer implements ApplicationRunner {

    private final PaxNationCategoryRepository paxNationCategoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        printStartBanner();
        boolean success = true;

        try {
            initializePaxNationCategories();
        } catch (Exception e) {
            log.error("Error during pax nation category initialization: {}", e.getMessage(), e);
            success = false;
        } finally {
            printEndBanner(success);
        }
    }

    private void printStartBanner() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════════╗");
        log.info("║                                                                    ║");
        log.info("║          PAX NATION CATEGORY INITIALIZER - START                   ║");
        log.info("║          Standard Passenger Nationality Categories                 ║");
        log.info("║                                                                    ║");
        log.info("╚════════════════════════════════════════════════════════════════════╝");
        log.info("");
    }

    private void printEndBanner(boolean success) {
        log.info("");
        if (success) {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║        ✓ PAX NATION CATEGORY INITIALIZER - COMPLETED               ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        } else {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║        ✗ PAX NATION CATEGORY INITIALIZER - FAILED                  ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        }
        log.info("");
    }

    private void initializePaxNationCategories() {
        log.info("Starting pax nation category initialization...");

        List<PaxNationCategoryData> categoriesData = getPaxNationCategoryDataList();

        int createdCategories = 0;
        int skippedCategories = 0;

        for (PaxNationCategoryData categoryData : categoriesData) {
            // Check if category already exists
            if (paxNationCategoryRepository.existsByNameIgnoreCase(categoryData.name)) {
                log.debug("⊘ Pax nation category already exists: {}", categoryData.name);
                skippedCategories++;
                continue;
            }

            // Create category
            PaxNationCategory category = PaxNationCategory.builder()
                    .name(categoryData.name)
                    .categoryType(categoryData.categoryType)
                    .description(categoryData.description)
                    .priorityFactor(categoryData.priorityFactor)
                    .isActive(true)
                    .isSystem(true) // System categories - protected from deletion
                    .build();

            paxNationCategoryRepository.save(category);
            log.info("✓ Created pax nation category: {} (Priority {}, {})",
                categoryData.name,
                categoryData.priorityFactor,
                categoryData.categoryType);
            createdCategories++;
        }

        log.info("");
        log.info("Pax nation category initialization complete:");
        log.info("  - Created: {} categories", createdCategories);
        log.info("  - Skipped: {} existing categories", skippedCategories);
        log.info("  - Total categories in system: {}", paxNationCategoryRepository.count());
    }

    /**
     * Get standard pax nation category data
     *
     * Standard Categories (all isSystem = true):
     * 1. Resident (Priority 1) - Tanzanian citizens and permanent residents
     * 2. Expatriate (Priority 2) - Foreign nationals with work/residence permits
     * 3. East African (Priority 3) - Citizens of EAC member states
     * 4. Non-Resident (Priority 4) - International visitors
     *
     * Priority factors are designed to:
     * - Rank from lowest rate (1) to highest rate (4)
     * - Determine group pricing when ChargingBasis is PER_VEHICLE or PER_GROUP
     * - Align with TANAPA and NCAA pricing structures
     */
    private List<PaxNationCategoryData> getPaxNationCategoryDataList() {
        List<PaxNationCategoryData> categories = new ArrayList<>();

        // ===== 1. RESIDENT (Priority 1) =====
        // Tanzanian citizens and permanent residents - lowest rates
        categories.add(new PaxNationCategoryData(
            "Resident",
            PaxNationCategory.CategoryType.RESIDENT,
            1,
            "Tanzanian citizens and permanent residents. Eligible for resident rates at TANAPA " +
            "and NCAA parks. Lowest pricing tier. Must provide valid Tanzanian ID or residence permit. " +
            "Includes Tanzanian nationals and those with permanent residence status."
        ));

        // ===== 2. EXPATRIATE (Priority 2) =====
        // Foreign nationals with work/residence permits - intermediate rates
        categories.add(new PaxNationCategoryData(
            "Expatriate",
            PaxNationCategory.CategoryType.EXPATRIATE,
            2,
            "Foreign nationals residing in Tanzania with valid work permits or residence permits. " +
            "Eligible for expatriate rates at some parks and accommodations. Intermediate pricing " +
            "between resident and non-resident rates. Must provide valid work/residence permit."
        ));

        // ===== 3. EAST AFRICAN (Priority 3) =====
        // Citizens of East African Community member states
        categories.add(new PaxNationCategoryData(
            "East African",
            PaxNationCategory.CategoryType.EAST_AFRICAN,
            3,
            "Citizens of East African Community (EAC) member states: Kenya, Uganda, Rwanda, " +
            "Burundi, South Sudan, and DRC. Eligible for East African rates at TANAPA parks. " +
            "Pricing typically between resident and non-resident rates. Must provide valid " +
            "passport or national ID from an EAC member state."
        ));

        // ===== 4. NON-RESIDENT (Priority 4) =====
        // International visitors - highest rates
        categories.add(new PaxNationCategoryData(
            "Non-Resident",
            PaxNationCategory.CategoryType.NON_RESIDENT,
            4,
            "International visitors from outside East Africa. Full non-resident/foreign rates " +
            "apply at all TANAPA and NCAA parks. Highest pricing tier. Includes tourists, " +
            "business visitors, and all other foreign nationals not qualifying for other categories."
        ));

        return categories;
    }

    /**
     * Helper class to structure pax nation category data during initialization
     */
    private static class PaxNationCategoryData {
        String name;
        PaxNationCategory.CategoryType categoryType;
        Integer priorityFactor;
        String description;

        PaxNationCategoryData(String name, PaxNationCategory.CategoryType categoryType,
                             Integer priorityFactor, String description) {
            this.name = name;
            this.categoryType = categoryType;
            this.priorityFactor = priorityFactor;
            this.description = description;
        }
    }
}
