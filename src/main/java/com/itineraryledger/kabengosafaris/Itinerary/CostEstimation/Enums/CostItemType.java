package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Types of cost items in a cost estimation.
 */
@Getter
@RequiredArgsConstructor
public enum CostItemType {
    ACCOMMODATION("Accommodation", "Lodging and room costs"),
    PARK_FEE("Park Fee", "Park entry and conservation fees"),
    ACTIVITY("Activity", "Activity and tour costs"),
    /*
     * ⚠️ Adding a value here is only HALF the change. The aggregators total by an if/else chain over
     * this enum and SILENTLY DROP anything they do not recognise, so a new type without a matching
     * branch in PerDayCostAggregator.calculateTotalsByCurrency, PerPaxCostAggregator and
     * CurrencyGroupedCostDTO produces line items that appear on screen and are missing from the
     * total — which is worse than not showing them at all.
     */
    FLIGHT("Flight", "Air fares, their taxes and the markup on them");

    private final String displayName;
    private final String description;
}
