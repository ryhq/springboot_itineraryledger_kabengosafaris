package com.itineraryledger.kabengosafaris.Safari.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerSource;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerType;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary.ItineraryStatus;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark.ParkEntryType;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariPhase;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * FullSafariDTO - Complete safari data with all nested entities
 *
 * Structure:
 * Safari
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
public class FullSafariDTO {

    // ========================
    // SAFARI FIELDS
    // ========================
    private String id;
    private CustomerDTO customer;
    private ItineraryDTO itinerary;
    private String name;
    private String code;
    private String slug;
    private SafariState state;
    private String stateDisplayName;
    private String stateReason;
    private LocalDateTime stateChangedAt;
    private SafariPhase currentPhase;
    private String currentPhaseDisplayName;
    private Boolean isUrgentPhase;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
    private Integer totalNights;
    private Boolean isDayTrip;
    private Integer carCount;
    private String description;
    private String highlights;
    private String startLocation;
    private String endLocation;
    private String specialRequests;
    private String dietaryRequirements;
    private String internalNotes;
    private String emergencyContact;
    private Boolean isActive;
    private Integer currentDayNumber;
    private Long daysUntilStart;
    private Long daysSinceEnd;
    private Boolean hasStarted;
    private Boolean hasEnded;
    private Boolean isInProgress;
    private Boolean isEditable;
    private Boolean isCancellable;
    private String createdByName;
    private String updatedByName;
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
     * Day in the safari
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DayDTO {
        private String id;
        private Integer dayNumber;
        private LocalDate date;
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

        // Safari-specific fields
        private String actualStartTime;
        private String actualEndTime;
        private String weatherConditions;
        private String dayNotes;
        private String highlightsOfDay;

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

        // Safari-specific fields
        private Boolean wasCompleted;
        private String actualStartTime;
        private String actualEndTime;
        private String completionNotes;
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
        /*
         * Where the lodge is, so the estimator can notice an alternative that sleeps outside the
         * park whose fees the day is carrying. Park fees hang off the park visit and do not follow
         * the bed, so the swap that looks cheapest can be the one that quietly loses a fee.
         */
        private String accommodationRegion;
        private String accommodationDistrict;
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

        // Safari-specific fields
        private String confirmationNumber;
        private LocalDateTime checkInTime;
        private LocalDateTime checkOutTime;
        private String actualRoomNumbers;
        private String guestFeedback;
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

        // Safari-specific fields
        private String actualArrivalTime;
        private String actualDepartureTime;
        private String entryReceiptNumber;
        private String wildlifeSightings;
        private String visitNotes;
        private Boolean feesPaid;
        private LocalDateTime feesPaidAt;
        private String weatherConditions;

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

        // Safari-specific fields
        private Boolean wasCompleted;
        private String actualStartTime;
        private String actualEndTime;
        private String completionNotes;
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
        private String parkId;
        private String parkName;
        private String tariffId;
        private String tariffName;
        private String notes;
        private Boolean isIncludedInPrice;

        // Safari-specific fields
        private Boolean isPaid;
        private LocalDateTime paidAt;
        private String receiptNumber;
        private String paymentNotes;
        private Integer paxCount;
        private Boolean isWaived;
        private String waiverReason;
    }

    /**
     * Customer information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CustomerDTO {
        private String id;
        private String code;
        private CustomerType customerType;
        private String customerTypeDisplayName;
        private String title;
        private String firstName;
        private String lastName;
        private String companyName;
        private String displayName;
        private String primaryEmail;
        private String primaryPhone;
        private String nationality;
        private String residency;
        private String passportNumber;
        private LocalDate passportExpiry;
        private LocalDate dateOfBirth;
        private Boolean passportExpiringSoon;
        private String address;
        private String city;
        private String state;
        private String country;
        private String postalCode;
        private String fullAddress;
        private String preferredLanguage;
        private String preferredCurrency;
        private CustomerSource source;
        private String sourceDisplayName;
        private String referredBy;
        private String dietaryRequirements;
        private String medicalConditions;
        private String specialRequests;
        private String interests;
        private Boolean isVip;
    }

    /**
     * Itinerary information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ItineraryDTO {
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
        private Integer totalPaxCount;
        private Integer totalDaysCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
