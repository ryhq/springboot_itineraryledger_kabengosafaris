package com.itineraryledger.kabengosafaris.ParkTariffRate.DTOs;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * UpdateParkTariffRateDTO - Request DTO for updating an existing rate
 *
 * All fields are optional - only provided fields will be updated.
 * Note: parkId, tariffId, seasonId, nationCategoryId, and ageCategory
 * are part of the unique key and cannot be updated. To change these,
 * delete the old rate and create a new one.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateParkTariffRateDTO {

    @Positive(message = "Rack rate must be positive")
    private BigDecimal rackRate;

    @Positive(message = "STO rate must be positive if provided")
    private BigDecimal stoRate;

    /**
     * Set to true to clear the STO rate (different from providing null)
     */
    private Boolean clearStoRate;

    private String currency;

    private String notes;

    private Boolean isActive;
}
