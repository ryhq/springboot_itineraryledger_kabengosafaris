package com.itineraryledger.kabengosafaris.Itinerary.DTOs;

import com.itineraryledger.kabengosafaris.Inclusion.DTOs.InclusionLineDTO;

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
        private List<DayFlightDTO> flights;
    }

    /**
     * A flight on a day.
     *
     * <p>Carries the fare's own figures rather than a rate id, because the cost engine needs the net
     * fare, the tax and the markup that is actually in force, and the markup is a cascade — this
     * line's, then the fare's, then the airline's. Resolving it once when the DTO is built means the
     * calculator and the panel cannot disagree about which one applied.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DayFlightDTO {
        private String id;
        private String flightRouteId;
        private String flightFareId;
        private String airlineName;
        /** "ARS → ZNZ". */
        private String sectorLabel;
        private String originCode;
        private String destinationCode;
        private String etd;
        private String eta;
        /** Null on 71 of Air Excel's 350 rows; "AM" on seven of those. */
        private String departureLabel;
        private BigDecimal netFare;
        private BigDecimal taxesAndFees;
        private BigDecimal childPercent;
        private String currency;
        /** PERCENT or AMOUNT, already resolved down the cascade. */
        private String markupType;
        private BigDecimal markupValue;
        /** "this flight", "the fare" or the airline's name — so a quote can say where it came from. */
        private String markupSource;
        private Integer passengerCount;
        private Integer sortOrder;
        private String notes;
        private Boolean isAlternative;
        private Boolean isIncludedInPrice;
        /** The sector is flown on request only, which warns but never blocks. */
        private Boolean isOnRequest;
        private Integer minimumSeats;
        /** Which months it flies, so the calculator can warn about an out-of-season date. */
        private String operatingMonths;
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
        /*
         * Where the lodge is, so the estimator can notice an alternative that sleeps outside the
         * park whose fees the day is carrying. Park fees hang off the park visit and do not follow
         * the bed, so the swap that looks cheapest can be the one that quietly loses a fee.
         */
        private String accommodationRegion;
        private String accommodationDistrict;
        /*
         * The lodge's own category, which is a label somebody typed and may be wrong. It never
         * decides a level -- the money does -- but it is what lets a level column say "cheapest
         * here, and labelled Ultra-Luxury", which is either a mislabelled lodge or a good deal,
         * and both are worth somebody's attention.
         */
        private String accommodationCategory;
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

    /**
     * The highlights as a list a document can print, however they happen to be stored.
     *
     * <p>Most itineraries hold this field as a JSON array, because that is what the website wants.
     * The PDF templates printed the field straight out, so "Trip Highlights" on every itinerary
     * document a customer has ever received read {@code ["Tarangire elephants", "Crater descent"]},
     * brackets, quotes and all. 46 of 60 itineraries are stored that way.
     *
     * <p>A getter rather than a stored field: SpEL resolves getters, the value is derived, and
     * changing the stored shape would break the website that expects the array.
     */
    public java.util.List<String> getHighlightsList() {
        if (highlights == null || highlights.isBlank()) return java.util.List.of();
        String trimmed = highlights.trim();
        if (trimmed.startsWith("[")) {
            try {
                java.util.List<String> parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(trimmed, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {});
                return parsed.stream().filter(h -> h != null && !h.isBlank()).map(String::trim).toList();
            } catch (Exception e) {
                // unparseable: fall through and show it as written rather than showing nothing
            }
        }
        // a plain value may still be several lines, or one sentence
        java.util.List<String> lines = java.util.Arrays.stream(trimmed.split("\\r?\\n"))
            .map(String::trim).filter(l -> !l.isEmpty()).toList();
        return lines.isEmpty() ? java.util.List.of(trimmed) : lines;
    }

    // =====================================================================
    // WHAT THE PRICE COVERS
    // =====================================================================

    /**
     * The lines this document states about its own price, snapshotted when it was produced.
     *
     * <p>Never read live from the catalogue: a customer holds a copy of this document, and a
     * catalogue edit that changed what our copy says would make ours the one that looked altered.
     */
    private java.util.List<InclusionLineDTO> inclusions;

    /**
     * What is included, as plain lines.
     *
     * <p>A getter rather than a stored field, the same shape as {@code highlightsList}: SpEL
     * resolves getters, so a template can iterate a value nothing has to store or keep in step.
     */
    public java.util.List<String> getInclusionsList() {
        return linesWhere(true);
    }

    /** What is not included, as plain lines. Stated on the document, never derived from the above. */
    public java.util.List<String> getExclusionsList() {
        return linesWhere(false);
    }

    private java.util.List<String> linesWhere(boolean included) {
        if (inclusions == null || inclusions.isEmpty()) return java.util.List.of();
        return inclusions.stream()
            .filter(line -> line != null && line.isIncluded() == included)
            .map(InclusionLineDTO::getLabel)
            .filter(label -> label != null && !label.isBlank())
            .map(String::trim)
            .toList();
    }

}
