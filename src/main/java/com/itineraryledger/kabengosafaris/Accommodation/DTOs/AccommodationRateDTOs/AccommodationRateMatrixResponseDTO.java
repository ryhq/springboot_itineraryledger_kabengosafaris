package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRateDTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AccommodationRateMatrixResponseDTO - Response DTO for the rate matrix/grid UI
 *
 * Provides all necessary data for rendering a rate input matrix:
 * - Accommodation information
 * - List of accommodation-specific seasons (with exclusion support)
 * - List of room types (with exclusion support)
 * - List of room standards (with exclusion support)
 * - List of board types (with exclusion support)
 * - Existing rates filtered by the same exclusions
 *
 * The matrix for accommodation rates:
 * Season x RoomStandard x RoomType x BoardType | STO Rate | Rack Rate | Currency
 *
 * OR filtered by season:
 * RoomStandard x RoomType x BoardType | STO Rate | Rack Rate | Currency
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccommodationRateMatrixResponseDTO {

    /**
     * Accommodation information
     */
    private AccommodationInfo accommodation;

    /**
     * Currently selected season (null if viewing all seasons)
     */
    private SeasonInfo currentSeason;

    /**
     * List of accommodation-specific seasons (after exclusions applied)
     */
    private List<SeasonInfo> seasons;

    /**
     * List of room types for this accommodation (after exclusions applied)
     */
    private List<RoomTypeInfo> roomTypes;

    /**
     * List of room standards for this accommodation (after exclusions applied)
     */
    private List<RoomStandardInfo> roomStandards;

    /**
     * List of board types for this accommodation (after exclusions applied)
     */
    private List<BoardTypeInfo> boardTypes;

    /**
     * Existing rates for the accommodation (filtered by same exclusions)
     */
    private List<AccommodationRateDTO> existingRates;

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
    public static class AccommodationInfo {
        private String id;
        private String name;
        private String accommodationType;
        private String category;
        private Boolean isActive;
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
        private Boolean isActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoomTypeInfo {
        private String id;
        private String name;
        private String bedConfiguration;
        private Integer maxOccupancy;
        private Boolean isActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoomStandardInfo {
        private String id;
        private String name;
        private String viewType;
        private Boolean isActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BoardTypeInfo {
        private String id;
        private String name;
        private String mealsIncluded;
        private Boolean breakfastIncluded;
        private Boolean lunchIncluded;
        private Boolean dinnerIncluded;
        private Boolean isActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MatrixSummary {
        /**
         * Total possible rate combinations based on the matrix
         * = seasons x roomTypes x roomStandards x boardTypes
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
         * Number of room types in the matrix
         */
        private Integer roomTypesCount;

        /**
         * Number of room standards in the matrix
         */
        private Integer roomStandardsCount;

        /**
         * Number of board types in the matrix
         */
        private Integer boardTypesCount;
    }
}
