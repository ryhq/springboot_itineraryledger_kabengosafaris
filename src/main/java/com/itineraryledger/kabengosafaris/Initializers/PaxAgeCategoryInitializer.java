package com.itineraryledger.kabengosafaris.Initializers;

import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.Repositories.PaxAgeCategoryRepository;
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
 * PaxAgeCategoryInitializer - Initializes passenger age categories at application startup
 *
 * Creates standard age categories used for pricing across accommodations, activities, and parks.
 * These categories ensure consistent age-based pricing throughout the system.
 *
 * Runs AFTER GlobalSeasonInitializer (Order = HIGHEST_PRECEDENCE + 10)
 *
 * Standard Age Categories:
 * 1. Child (0-5 years): Infants and young children, typically free or heavily discounted
 * 2. Youth (6-14 years): Older children and young teens, typically discounted rates
 * 3. Adult (15+ years): Adults and older teens, full pricing applies
 *
 * Behavior:
 * - Only creates categories that don't already exist (checked by name)
 * - All created categories are marked as system (isSystem = true, protected from deletion)
 * - Does not update or delete existing categories
 * - Age ranges are designed to be contiguous with no gaps (0-5, 6-14, 15+)
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // Run after GlobalSeasonInitializer
public class PaxAgeCategoryInitializer implements ApplicationRunner {

    private final PaxAgeCategoryRepository paxAgeCategoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        printStartBanner();
        boolean success = true;

        try {
            initializePaxAgeCategories();
        } catch (Exception e) {
            log.error("Error during pax age category initialization: {}", e.getMessage(), e);
            success = false;
        } finally {
            printEndBanner(success);
        }
    }

    private void printStartBanner() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════════╗");
        log.info("║                                                                    ║");
        log.info("║          PAX AGE CATEGORY INITIALIZER - START                      ║");
        log.info("║          Standard Passenger Age Categories                         ║");
        log.info("║                                                                    ║");
        log.info("╚════════════════════════════════════════════════════════════════════╝");
        log.info("");
    }

    private void printEndBanner(boolean success) {
        log.info("");
        if (success) {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║        ✓ PAX AGE CATEGORY INITIALIZER - COMPLETED                  ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        } else {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║        ✗ PAX AGE CATEGORY INITIALIZER - FAILED                     ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        }
        log.info("");
    }

    private void initializePaxAgeCategories() {
        log.info("Starting pax age category initialization...");

        List<PaxAgeCategoryData> categoriesData = getPaxAgeCategoryDataList();

        int createdCategories = 0;
        int skippedCategories = 0;

        for (PaxAgeCategoryData categoryData : categoriesData) {
            // Check if category already exists
            if (paxAgeCategoryRepository.existsByNameIgnoreCase(categoryData.name)) {
                log.debug("⊘ Pax age category already exists: {}", categoryData.name);
                skippedCategories++;
                continue;
            }

            // Create category
            PaxAgeCategory category = PaxAgeCategory.builder()
                    .name(categoryData.name)
                    .categoryType(categoryData.categoryType)
                    .minAge(categoryData.minAge)
                    .maxAge(categoryData.maxAge)
                    .description(categoryData.description)
                    .isActive(true)
                    .isSystem(true) // System categories - protected from deletion
                    .build();

            paxAgeCategoryRepository.save(category);
            log.info("✓ Created pax age category: {} ({}-{} years, {})",
                categoryData.name,
                categoryData.minAge,
                categoryData.maxAge >= 150 ? "∞" : categoryData.maxAge,
                categoryData.categoryType);
            createdCategories++;
        }

        log.info("");
        log.info("Pax age category initialization complete:");
        log.info("  - Created: {} categories", createdCategories);
        log.info("  - Skipped: {} existing categories", skippedCategories);
        log.info("  - Total categories in system: {}", paxAgeCategoryRepository.count());
    }

    /**
     * Get standard pax age category data
     *
     * Standard Categories (all isSystem = true):
     * 1. Child (0-5 years) - Infants and young children
     * 2. Youth (6-14 years) - Older children and young teens
     * 3. Adult (15+ years) - Adults and older teens
     *
     * Age ranges are designed to:
     * - Be contiguous with no gaps (0-5, 6-14, 15+)
     * - Cover all possible ages from 0 to 150+
     * - Align with common tourism industry pricing tiers
     * - Match TANAPA and NCAA pricing structures
     */
    private List<PaxAgeCategoryData> getPaxAgeCategoryDataList() {
        List<PaxAgeCategoryData> categories = new ArrayList<>();

        // ===== 1. CHILD (0-5 years) =====
        // Infants and young children - typically free or heavily discounted
        categories.add(new PaxAgeCategoryData(
            "Child",
            PaxAgeCategory.CategoryType.CHILD,
            0,
            5,
            "Infants and young children aged 0-5 years. Typically free entry to national parks " +
            "(TANAPA/NCAA policy). Accommodation rates vary - often free when sharing with adults. " +
            "May require car seats for transfers. Some activities have minimum age requirements."
        ));

        // ===== 2. YOUTH (6-14 years) =====
        // Older children and young teens - typically discounted rates
        categories.add(new PaxAgeCategoryData(
            "Youth",
            PaxAgeCategory.CategoryType.YOUTH,
            6,
            14,
            "Youth aged 6-14 years. Eligible for reduced park entrance fees at TANAPA and NCAA parks. " +
            "Accommodation rates typically 30-50% of adult rate when sharing with parents. " +
            "Can participate in most safari activities. Some lodges offer children's programs. " +
            "Walking safaris may have minimum age of 12-16 depending on operator."
        ));

        // ===== 3. ADULT (15+ years) =====
        // Adults and older teens - full pricing applies
        categories.add(new PaxAgeCategoryData(
            "Adult",
            PaxAgeCategory.CategoryType.ADULT,
            15,
            150,
            "Adults and older teens aged 15 years and above. Full adult pricing applies for park " +
            "entrance fees, accommodation, and activities. TANAPA adult park fees apply (currently " +
            "USD 70 high season, USD 50-60 low season for most parks). No restrictions on activities. " +
            "Standard accommodation rates apply."
        ));

        return categories;
    }

    /**
     * Helper class to structure pax age category data during initialization
     */
    private static class PaxAgeCategoryData {
        String name;
        PaxAgeCategory.CategoryType categoryType;
        Integer minAge;
        Integer maxAge;
        String description;

        PaxAgeCategoryData(String name, PaxAgeCategory.CategoryType categoryType,
                          Integer minAge, Integer maxAge, String description) {
            this.name = name;
            this.categoryType = categoryType;
            this.minAge = minAge;
            this.maxAge = maxAge;
            this.description = description;
        }
    }
}
