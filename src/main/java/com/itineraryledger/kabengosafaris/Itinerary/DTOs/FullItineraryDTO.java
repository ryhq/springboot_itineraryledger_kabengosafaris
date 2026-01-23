package com.itineraryledger.kabengosafaris.Itinerary.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary.ItineraryStatus;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark.ParkEntryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * FullItineraryDTO - Complete itinerary data with all nested entities
 *
 * Structure:
 * Itinerary
 * ├── paxList (passenger configurations)
 * └── days (ordered by dayNumber)
 *     ├── activities (standalone activities, ordered by sortOrder)
 *     ├── accommodations (lodging options)
 *     └── parks (park visits, ordered by sortOrder)
 *         ├── activities (park-specific activities, ordered by sortOrder)
 *         └── tariffs (park entry fees/tariffs)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FullItineraryDTO {

    // ========================
    // ITINERARY FIELDS
    // ========================
    private String id;
    private String name;
    private String code;
    private ItineraryStatus status;
    private String statusDisplayName;
    private TripType tripType;
    private String tripTypeDisplayName;
    private String tripTypeDescription;
    private BudgetCategory budgetCategory;
    private String budgetCategoryDisplayName;
    private String budgetCategoryDescription;
    private Integer budgetCategoryTier;
    private Integer totalDays;
    private Integer totalNights;
    private Boolean isDayTrip;
    private Integer carCount;
    private String description;
    private String highlights;
    private String startLocation;
    private String endLocation;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========================
    // SUMMARY STATISTICS
    // ========================
    private Integer totalPaxCount;
    private Integer totalDaysCount;
    private Integer totalParksCount;
    private Integer totalActivitiesCount;
    private Integer totalAccommodationsCount;

    // ========================
    // NESTED DATA
    // ========================
    private List<PaxDTO> paxList;
    private List<DayDTO> days;

    // ========================
    // NESTED DTO CLASSES
    // ========================

    /**
     * Passenger configuration
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaxDTO {
        private String id;
        private String nationCategoryId;
        private String nationCategoryName;
        private String ageCategoryId;
        private String ageCategoryName;
        private Integer count;
        private String notes;
    }

    /**
     * Day in the itinerary
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DayDTO {
        private String id;
        private Integer dayNumber;
        private String dayTag;
        private String title;
        private String description;
        private String morningActivities;
        private String afternoonActivities;
        private String eveningActivities;
        private String wildlifeHighlights;
        private String scenicHighlights;
        private String specialNotes;
        private String startLocation;
        private String endLocation;
        private Integer distanceKm;
        private Boolean isOvernight;
        private String mealsIncluded;
        private LocalDateTime createdAt;

        // Nested data
        private List<DayActivityDTO> activities;
        private List<DayAccommodationDTO> accommodations;
        private List<DayParkDTO> parks;
    }

    /**
     * Standalone activity for a day (not tied to a park)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DayActivityDTO {
        private String id;
        private String activityId;
        private String activityName;
        private String activitySlug;
        private Integer sortOrder;
        private BigDecimal durationHours;
        private String startTime;
        private String endTime;
        private String notes;
        private Boolean isIncludedInPrice;
        private Boolean isOptional;
    }

    /**
     * Accommodation for a day
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DayAccommodationDTO {
        private String id;
        private String accommodationId;
        private String accommodationName;
        private String accommodationSlug;
        private String roomTypeId;
        private String roomTypeName;
        private Integer roomTypeMaxOccupancy;
        private Integer roomTypeMinOccupancy;
        private String roomStandardId;
        private String roomStandardName;
        private String boardTypeId;
        private String boardTypeName;
        private Integer roomCount;
        private Boolean isAlternative;
        private String notes;
    }

    /**
     * Park visit for a day
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DayParkDTO {
        private String id;
        private String parkId;
        private String parkName;
        private String parkSlug;
        private ParkEntryType entryType;
        private String entryTypeDisplayName;
        private Integer sortOrder;
        private String arrivalTime;
        private String departureTime;
        private String notes;

        // Nested data
        private List<ParkActivityDTO> activities;
        private List<ParkTariffDTO> tariffs;
    }

    /**
     * Activity within a park visit
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ParkActivityDTO {
        private String id;
        private String activityId;
        private String activityName;
        private Integer sortOrder;
        private BigDecimal durationHours;
        private String startTime;
        private String endTime;
        private String notes;
        private Boolean isIncludedInPrice;
    }

    /**
     * Tariff for a park visit
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ParkTariffDTO {
        private String id;
        private String tariffId;
        private String tariffName;
        private String notes;
        private Boolean isIncludedInPrice;
    }
}
