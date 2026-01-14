package com.itineraryledger.kabengosafaris.ParkTariffRate.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ParkRateMatrixResponseDTO - Response DTO for the rate matrix/grid UI
 *
 * Provides all necessary data for rendering a rate input matrix:
 * - Park and Tariff information
 * - List of active global seasons (with exclusion support)
 * - List of active nation categories (with exclusion support)
 * - List of active age categories (only for PER_PERSON tariffs, with exclusion support)
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
public class ParkRateMatrixResponseDTO {

    /**
     * Current park being viewed
     */
    private ParkInfo park;

    /**
     * Tariff information
     */
    private TariffInfo tariff;

    /**
     * List of all parks where this tariff is used
     * Allows users to see which other parks have this tariff configured
     */
    private List<ParkInfo> parksWithTariff;

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
     * NULL or empty if tariff charging basis is NOT PER_PERSON
     */
    private List<AgeCategoryInfo> ageCategories;

    /**
     * Whether age categories are applicable for this tariff
     * TRUE only when tariff.chargingBasis == PER_PERSON
     */
    private Boolean includesAgeCategories;

    /**
     * Existing rates for the park-tariff combination (filtered by same exclusions)
     * These are rates that already exist in the database
     */
    private List<ParkTariffRateDTO> existingRates;

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
    public static class ParkInfo {
        private String id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TariffInfo {
        private String id;
        private String name;
        private String chargingBasis;
        private String chargingBasisDisplayName;
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
