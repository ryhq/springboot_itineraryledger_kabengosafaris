package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.DTOs;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark.ParkEntryType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateItineraryDayParkDTO - Data Transfer Object for updating an ItineraryDayPark
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateItineraryDayParkDTO {

    private ParkEntryType entryType;
    private String arrivalTime;
    private String departureTime;
    private String notes;
}
