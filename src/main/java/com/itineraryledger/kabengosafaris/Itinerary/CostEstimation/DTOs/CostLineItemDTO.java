package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CostItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for a single cost line item in cost estimation.
 * Contains both STO and Rack prices.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CostLineItemDTO {

    /**
     * Day number (1-indexed)
     */
    private Integer dayNumber;

    /**
     * Type of item: ACCOMMODATION, PARK_FEE, ACTIVITY
     */
    private CostItemType itemType;

    /**
     * Display name (e.g., "Conservation Fee - Serengeti")
     */
    private String itemName;

    /**
     * Obfuscated ID of the item
     */
    private String itemId;

    /**
     * Charging basis (e.g., "PER_PERSON", "PER_VEHICLE")
     */
    private String chargingBasis;

    /**
     * Quantity used (pax count, car count, or 1 for flat rate)
     */
    private Integer quantity;

    /**
     * STO unit price
     */
    private BigDecimal stoUnitPrice;

    /**
     * Rack unit price
     */
    private BigDecimal rackUnitPrice;

    /**
     * STO total price (stoUnitPrice * quantity)
     */
    private BigDecimal stoTotalPrice;

    /**
     * Rack total price (rackUnitPrice * quantity)
     */
    private BigDecimal rackTotalPrice;

    /**
     * Currency code (e.g., "USD", "TZS")
     */
    private String currency;

    /**
     * Pax category description (e.g., "Non-Resident Adult")
     * Only populated for per-person items
     */
    private String paxCategory;

    /**
     * Additional context or notes
     */
    private String notes;
}
