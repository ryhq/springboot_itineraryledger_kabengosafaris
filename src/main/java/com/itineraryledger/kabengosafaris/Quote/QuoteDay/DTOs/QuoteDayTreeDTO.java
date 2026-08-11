package com.itineraryledger.kabengosafaris.Quote.QuoteDay.DTOs;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark.ParkEntryType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One quote day with everything on it: its parks (with their fees and
 * activities), its stays and its own activities.
 *
 * The field names deliberately match FullItineraryDTO's day tree. The
 * configuration screen is the same screen for both — a quote's days ARE the
 * itinerary's days, copied at the moment it was priced — so the two payloads
 * being the same shape is what lets one component serve both instead of two
 * that drift apart.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuoteDayTreeDTO {

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
    private String internalNotes;
    private LocalDateTime createdAt;

    private List<DayActivityDTO> activities;
    private List<DayAccommodationDTO> accommodations;
    private List<DayParkDTO> parks;

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

        private List<ParkActivityDTO> activities;
        private List<ParkTariffDTO> tariffs;
    }

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
