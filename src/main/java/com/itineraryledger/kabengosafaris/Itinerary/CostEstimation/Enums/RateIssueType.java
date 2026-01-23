package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Types of rate issues that can occur during cost estimation.
 */
@Getter
@RequiredArgsConstructor
public enum RateIssueType {
    MISSING("Missing", "No rate found for the given parameters"),
    INACTIVE("Inactive", "Rate exists but is marked as inactive"),
    NO_STO_RATE("No STO Rate", "Rate exists but has no STO rate defined"),
    NO_SEASON("No Season", "Accommodation has no seasons configured"),
    SEASON_NOT_FOUND("Season Not Found", "No applicable season found for the date, using fallback by priority");

    private final String displayName;
    private final String description;
}
