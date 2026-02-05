package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark.ParkEntryType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SafariDayParkDTO - Data Transfer Object for SafariDayPark entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SafariDayParkDTO {
    private String id;
    private String safariDayId;
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

    private LocalDateTime createdAt;
}
