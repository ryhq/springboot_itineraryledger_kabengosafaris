package com.itineraryledger.kabengosafaris.ActivityTariffRate.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ActivityTariffRateDTO - Response DTO for ActivityTariffRate entity
 *
 * Used for API responses when returning rate data.
 * Contains obfuscated IDs and display-friendly fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityTariffRateDTO {

    /**
     * Obfuscated rate ID
     */
    private String id;

    // Activity information
    private String activityId;
    private String activityName;
    private String activityChargingBasis;

    // Park information (null for global rates)
    private String parkId;
    private String parkName;
    private Boolean isGlobalRate;

    // Season information
    private String seasonId;
    private String seasonName;
    private String seasonType;

    // Nation category information
    private String nationCategoryId;
    private String nationCategoryName;

    // Age category information (null for non-PER_PERSON activities)
    private String ageCategoryId;
    private String ageCategoryName;
    private String ageCategoryAgeRange;

    /**
     * Rate information
     *
     * Rack Rate: The price charged to the customer (revenue)
     * STO Rate: The cost we pay on behalf of the customer (expense)
     * Profit Amount: Rack Rate - STO Rate (the profit we make)
     * Profit Percentage: (Profit Amount / Rack Rate) * 100
     *
     * Business logic: Rack Rate >= STO Rate (we charge at least what we pay)
     */
    private BigDecimal rackRate;
    private BigDecimal stoRate;
    private String currency;
    private BigDecimal profitAmount;
    private BigDecimal profitPercentage;

    // Metadata
    private String notes;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
