package com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationRateDTOs;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * UpdateAccommodationRateDTO - Request DTO for updating an existing accommodation rate
 *
 * All fields are optional - only provided fields will be updated.
 * Note: accommodationId, seasonId, roomTypeId, roomStandardId, and boardTypeId
 * are part of the unique key and cannot be updated. To change these,
 * delete the old rate and create a new one.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccommodationRateDTO {

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

    /**
     * Rate charging model
     *
     * TRUE: Per Person Sharing (PPS) - rate is per guest
     * FALSE: Per Room - rate is per room regardless of occupancy
     */
    private Boolean isPerPerson;
}
