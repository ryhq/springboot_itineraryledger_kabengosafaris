package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateSafariDayParkTariffDTO - Data Transfer Object for creating a SafariDayParkTariff
 *
 * Standard fields for creating tariff entries. Safari-specific fields are initialized
 * on creation or updated later via UpdateSafariDayParkTariffDTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSafariDayParkTariffDTO {

    @NotBlank(message = "Park ID is required")
    private String parkId; // Obfuscated ID (must match parent park)

    @NotBlank(message = "Tariff ID is required")
    private String tariffId; // Obfuscated ID

    private String notes;
    private Boolean isIncludedInPrice;
}
