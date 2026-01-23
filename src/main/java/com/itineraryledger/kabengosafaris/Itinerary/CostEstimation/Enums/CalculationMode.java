package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Calculation mode for cost estimation.
 *
 * PER_DAY: Costs grouped by day with daily subtotals
 * PER_PAX: Costs grouped by passenger category with per-pax subtotals
 */
@Getter
@RequiredArgsConstructor
public enum CalculationMode {
    PER_DAY("Per Day", "Costs calculated and grouped by day"),
    PER_PAX("Per PAX", "Costs calculated and grouped by passenger category");

    private final String displayName;
    private final String description;
}
