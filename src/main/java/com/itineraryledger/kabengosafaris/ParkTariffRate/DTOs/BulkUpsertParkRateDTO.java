package com.itineraryledger.kabengosafaris.ParkTariffRate.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * BulkUpsertParkRateDTO - Request DTO for bulk upsert operations
 *
 * The upsert logic will automatically determine whether to create or update
 * based on existing rate records for the given combination of:
 * park + tariff + season + nationCategory + ageCategory
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpsertParkRateDTO {

    @NotBlank(message = "Park ID is required")
    private String parkId;

    @NotBlank(message = "Tariff ID is required")
    private String tariffId;

    @NotBlank(message = "Season ID is required")
    private String seasonId;

    @NotBlank(message = "Nation category ID is required")
    private String nationCategoryId;

    /**
     * Age category ID - required for PER_PERSON tariffs, optional for others
     */
    private String ageCategoryId;

    @Positive(message = "Rack rate must be positive")
    private BigDecimal rackRate;

    @Positive(message = "STO rate must be positive if provided")
    private BigDecimal stoRate;

    @NotBlank(message = "Currency is required")
    private String currency;

    private String notes;

    private Boolean isActive;
}
