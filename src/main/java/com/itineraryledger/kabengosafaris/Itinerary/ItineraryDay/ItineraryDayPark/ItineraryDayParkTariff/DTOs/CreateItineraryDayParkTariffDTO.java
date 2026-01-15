package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateItineraryDayParkTariffDTO - Data Transfer Object for creating an ItineraryDayParkTariff
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateItineraryDayParkTariffDTO {

    @NotBlank(message = "Park ID is required")
    private String parkId; // Obfuscated ID (must match parent park)

    @NotBlank(message = "Tariff ID is required")
    private String tariffId; // Obfuscated ID

    private String notes;
    private Boolean isIncludedInPrice;
}
