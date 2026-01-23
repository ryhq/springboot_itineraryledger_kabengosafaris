package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for cost details for a single pax category.
 * Used in PER_PAX calculation mode.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaxCategoryCostDTO {

    /**
     * Obfuscated nation category ID
     */
    private String nationCategoryId;

    /**
     * Nation category name (e.g., "Non-Resident")
     */
    private String nationCategoryName;

    /**
     * Obfuscated age category ID
     */
    private String ageCategoryId;

    /**
     * Age category name (e.g., "Adult")
     */
    private String ageCategoryName;

    /**
     * Number of passengers in this category
     */
    private Integer paxCount;

    /**
     * Cost line items specific to this pax category
     * Includes per-person costs and proportional share of group/vehicle costs
     */
    @Builder.Default
    private List<CostLineItemDTO> lineItems = new ArrayList<>();

    /**
     * Totals grouped by currency for this pax category
     */
    @Builder.Default
    private List<CurrencyGroupedCostDTO> totalsByCurrency = new ArrayList<>();

    /**
     * Get combined category description
     */
    public String getCategoryDescription() {
        return nationCategoryName + " " + ageCategoryName;
    }
}
