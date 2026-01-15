package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.DTOs;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark.ParkEntryType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ItineraryDayParkDTO - Data Transfer Object for ItineraryDayPark entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryDayParkDTO {
    private String id;
    private String itineraryDayId;
    private String parkId;
    private String parkName;
    private String parkSlug;
    private ParkEntryType entryType;
    private String entryTypeDisplayName;
    private Integer sortOrder;
    private String arrivalTime;
    private String departureTime;
    private String notes;
    private LocalDateTime createdAt;
}
