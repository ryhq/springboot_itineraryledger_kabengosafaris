package com.itineraryledger.kabengosafaris.ActivityTariffRate.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * BulkUpsertActivityRateDTO - Request DTO for bulk upsert operations
 *
 * The upsert logic will automatically determine whether to create or update
 * based on existing rate records for the given combination of:
 * activity + park + season + nationCategory + ageCategory
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpsertActivityRateDTO {

    @NotBlank(message = "Activity ID is required")
    private String activityId;

    /**
     * Optional park ID for park-specific rates.
     * If null, the rate applies globally to the activity.
     */
    private String parkId;

    @NotBlank(message = "Season ID is required")
    private String seasonId;

    @NotBlank(message = "Nation category ID is required")
    private String nationCategoryId;

    /**
     * Age category - required for PER_PERSON activities, optional for others
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
