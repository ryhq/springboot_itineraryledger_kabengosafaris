package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for cost details for a single day.
 * Used in PER_DAY calculation mode.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DayCostDetailDTO {

    /**
     * Day number (1-indexed)
     */
    private Integer dayNumber;

    /**
     * Day title from itinerary (e.g., "Arrival in Arusha")
     */
    private String dayTitle;

    /**
     * Actual date of this day
     */
    private LocalDate date;

    /**
     * Season name applicable to this day
     */
    private String seasonName;

    /**
     * Whether this is an overnight day (accommodation applies)
     */
    private Boolean isOvernight;

    /**
     * All cost line items for this day
     */
    @Builder.Default
    private List<CostLineItemDTO> lineItems = new ArrayList<>();

    /**
     * Totals grouped by currency (no currency mixing)
     */
    @Builder.Default
    private List<CurrencyGroupedCostDTO> totalsByCurrency = new ArrayList<>();
}
