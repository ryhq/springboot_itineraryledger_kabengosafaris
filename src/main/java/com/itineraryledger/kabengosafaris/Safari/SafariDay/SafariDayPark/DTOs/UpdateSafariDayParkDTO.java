package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.DTOs;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark.ParkEntryType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * UpdateSafariDayParkDTO - Data Transfer Object for updating a SafariDayPark
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSafariDayParkDTO {

    private ParkEntryType entryType;
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
}
