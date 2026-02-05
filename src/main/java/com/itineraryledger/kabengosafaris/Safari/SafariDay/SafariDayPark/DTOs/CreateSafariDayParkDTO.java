package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.DTOs;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark.ParkEntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateSafariDayParkDTO - Data Transfer Object for creating a SafariDayPark
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSafariDayParkDTO {

    @NotBlank(message = "Park ID is required")
    private String parkId; // Obfuscated ID

    @NotNull(message = "Entry type is required")
    private ParkEntryType entryType;

    private String arrivalTime;
    private String departureTime;
    private String notes;
}
