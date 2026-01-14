package com.itineraryledger.kabengosafaris.ActivityTariffRate.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ActivityRateMatrixResponseDTO - Response DTO for the rate matrix/grid UI
 *
 * Provides all necessary data for rendering a rate input matrix:
 * - List of active global seasons (with exclusion support)
 * - List of active nation categories (with exclusion support)
 * - List of active age categories (only for PER_PERSON activities, with exclusion support)
 * - Activity information including charging basis
 * - Existing rates filtered by the same exclusions
 *
 * The frontend can use this data to render a grid where:
 * - Rows = Season × Nation Category (× Age Category for PER_PERSON)
 * - Columns = STO Rate, Rack Rate, Currency
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityRateMatrixResponseDTO {

    /**
     * Activity information
     */
    private ActivityInfo activity;

    /**
     * Park information (null for global rates)
     */
    private ParkInfo park;

    /**
     * Whether this is for global rates (no park)
     */
    private Boolean isGlobalRateMatrix;

    /**
     * List of parks that offer this activity (for dropdown selection)
     * Allows user to switch between global rates and park-specific rates
     */
    private List<ParkInfo> availableParks;

    /**
     * List of active global seasons (after exclusions applied)
     */
    private List<SeasonInfo> seasons;

    /**
     * List of active nation categories (after exclusions applied)
     */
    private List<NationCategoryInfo> nationCategories;

    /**
     * List of active age categories (after exclusions applied)
     * NULL or empty if activity charging basis is NOT PER_PERSON
     */
    private List<AgeCategoryInfo> ageCategories;

    /**
     * Whether age categories are applicable for this activity
     * TRUE only when activity.chargingBasis == PER_PERSON
     */
    private Boolean includesAgeCategories;

    /**
     * Existing rates for the activity (filtered by same exclusions)
     * These are rates that already exist in the database
     */
    private List<ActivityTariffRateDTO> existingRates;

    /**
     * Summary counts
     */
    private MatrixSummary summary;

    // ========================
    // NESTED DTOs
    // ========================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActivityInfo {
        private String id;
        private String name;
        private String chargingBasis;
        private String chargingBasisDisplayName;
        private Boolean hasTariff;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ParkInfo {
        private String id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SeasonInfo {
        private String id;
        private String name;
        private String seasonType;
        private String seasonTypeDisplayName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NationCategoryInfo {
        private String id;
        private String name;
        private String categoryType;
        private Integer priorityFactor;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AgeCategoryInfo {
        private String id;
        private String name;
        private String categoryType;
        private String ageRange;
        private Integer minAge;
        private Integer maxAge;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MatrixSummary {
        /**
         * Total possible rate combinations based on the matrix
         * For PER_PERSON: seasons × nationCategories × ageCategories
         * For others: seasons × nationCategories
         */
        private Integer totalPossibleRates;

        /**
         * Number of rates that already exist
         */
        private Integer existingRatesCount;

        /**
         * Number of rates yet to be configured
         */
        private Integer missingRatesCount;

        /**
         * Number of seasons in the matrix
         */
        private Integer seasonsCount;

        /**
         * Number of nation categories in the matrix
         */
        private Integer nationCategoriesCount;

        /**
         * Number of age categories in the matrix (0 if not applicable)
         */
        private Integer ageCategoriesCount;
    }
}
