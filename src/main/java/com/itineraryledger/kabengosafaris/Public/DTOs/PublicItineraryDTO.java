package com.itineraryledger.kabengosafaris.Public.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.ItineraryCostSummaryDTO;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary.ItineraryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.itineraryledger.kabengosafaris.Public.Annotations.Translatable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Public-safe Itinerary DTO with full day-by-day details.
 * Used by the public website to display "Safari Packages" (itinerary templates).
 * Includes cost summary (rack prices only) and nested day structure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicItineraryDTO {

    @Translatable private String name;
    private String code;

    // Status
    private ItineraryStatus status;
    @Translatable private String statusDisplayName;

    // Trip classification
    private TripType tripType;
    @Translatable private String tripTypeDisplayName;
    @Translatable private String tripTypeDescription;

    private BudgetCategory budgetCategory;
    @Translatable private String budgetCategoryDisplayName;
    @Translatable private String budgetCategoryDescription;
    private Integer budgetCategoryTier;

    // Duration
    private Integer totalDays;
    private Integer totalNights;
    private Boolean isDayTrip;

    // Descriptions
    @Translatable private String description;
    @Translatable private String highlights;
    @Translatable private String startLocation;
    @Translatable private String endLocation;

    // Counts
    private Integer carCount;
    private Integer totalPaxCount;
    private Integer totalDaysCount;

    // Primary image (derived from first day's park)
    private String primaryImageUrl;

    // Cost summary (rack prices only - no agent/net prices)
    private Double fromPriceUsd;

    private List<ItineraryCostSummaryDTO> costSummary;

    // Pax breakdown (nationality + age category + count) — tells users "price is for X adults, Y nationality"
    private List<PublicPaxDTO> paxBreakdown;

    // Day-by-day itinerary (for detail view only)
    private List<PublicItineraryDayDTO> days;

    // ========================
    // NESTED DTOs
    // ========================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PublicItineraryDayDTO {
        private Integer dayNumber;
        private String dayTag;
        @Translatable private String title;
        @Translatable private String description;
        @Translatable private String morningActivities;
        @Translatable private String afternoonActivities;
        @Translatable private String eveningActivities;
        @Translatable private String wildlifeHighlights;
        @Translatable private String scenicHighlights;
        @Translatable private String specialNotes;
        @Translatable private String startLocation;
        @Translatable private String endLocation;
        private Integer distanceKm;
        private Boolean isOvernight;
        @Translatable private String mealsIncluded;

        // Random image selected from this day's parks, activities, or accommodations
        private String dayImageUrl;

        private List<DayParkDTO> parks;
        private List<DayActivityDTO> activities;
        private List<DayAccommodationDTO> accommodations;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DayParkDTO {
        private String parkSlug;
        @Translatable private String parkName;
        private String primaryImageUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DayActivityDTO {
        private String activitySlug;
        @Translatable private String activityName;
        private BigDecimal durationHours;
        private Boolean isOptional;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DayAccommodationDTO {
        private String accommodationSlug;
        @Translatable private String accommodationName;
        private String primaryImageUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PublicPaxDTO {
        @Translatable private String nationCategoryName;
        @Translatable private String ageCategoryName;
        private Integer count;
    }
}
