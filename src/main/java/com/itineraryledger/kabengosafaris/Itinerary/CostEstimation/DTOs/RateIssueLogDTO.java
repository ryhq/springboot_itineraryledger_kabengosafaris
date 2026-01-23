package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CostItemType;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.RateIssueType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for logging rate issues during cost estimation.
 * Provides detailed information about missing or inactive rates.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RateIssueLogDTO {

    /**
     * Type of issue: MISSING, INACTIVE, NO_STO_RATE
     */
    private RateIssueType issueType;

    /**
     * Type of item: ACCOMMODATION, PARK_FEE, ACTIVITY
     */
    private CostItemType itemType;

    /**
     * Name of the item (e.g., "Conservation Fee", "Serena Lodge")
     */
    private String itemName;

    /**
     * Obfuscated ID of the item
     */
    private String itemId;

    /**
     * Day number where the issue occurred (1-indexed)
     */
    private Integer dayNumber;

    /**
     * Pax category description (e.g., "Non-Resident Adult")
     */
    private String paxCategory;

    /**
     * Season name that was looked up
     */
    private String seasonName;

    /**
     * Human-readable description of the issue
     */
    private String message;
}
