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
    ACTIVITY("Activity", "Activity and tour costs");

    private final String displayName;
    private final String description;
}
