package com.itineraryledger.kabengosafaris.ActivityTariffRate.DTOs;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * UpdateActivityTariffRateDTO - Request DTO for updating an existing activity rate
 *
 * All fields are optional - only provided fields will be updated.
 * Note: activityId, parkId, seasonId, nationCategoryId, and ageCategoryId
 * are part of the unique key and cannot be updated. To change these,
 * delete the old rate and create a new one.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateActivityTariffRateDTO {

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
