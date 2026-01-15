package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.DTOs;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark.ParkEntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateItineraryDayParkDTO - Data Transfer Object for creating an ItineraryDayPark
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateItineraryDayParkDTO {

    @NotBlank(message = "Park ID is required")
    private String parkId; // Obfuscated ID

    @NotNull(message = "Entry type is required")
    private ParkEntryType entryType;

    private Integer sortOrder;
    private String arrivalTime;
    private String departureTime;
    private String notes;
}
