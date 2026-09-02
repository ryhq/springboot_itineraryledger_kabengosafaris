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
    /**
     * The day itself, not its position.
     *
     * A cost screen that can change something has to name the row it is changing, and a day number
     * is not an address. Without this the only way to act on what the estimate shows was to go and
     * find the same day on another tab.
     */
    private String dayId;

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
     * Priced, and deliberately not in any total.
     *
     * Alternative beds, optional activities, and fees somebody switched off. They were discarded
     * before this existed, which meant the office could see what it had chosen and never what it
     * had passed over.
     *
     * The aggregators sum lineItems and only lineItems. Nothing here reaches a figure a client is
     * ever shown, and a test holds that line, because the day this list leaks into a total the
     * whole feature becomes a way to over-quote.
     */
    @Builder.Default
    private List<CostLineItemDTO> excludedLineItems = new ArrayList<>();

    /**
     * Totals grouped by currency (no currency mixing)
     */
    @Builder.Default
    private List<CurrencyGroupedCostDTO> totalsByCurrency = new ArrayList<>();
}
