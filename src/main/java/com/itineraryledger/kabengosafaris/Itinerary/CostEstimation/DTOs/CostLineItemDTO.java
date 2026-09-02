package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CostItemType;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.ExclusionReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

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

    /**
     * Why this line is shown but not counted. Null on every line that IS counted.
     *
     * Its presence is the whole contract of the excluded list: a line carrying a reason must never
     * reach a total, and a line in the totals must never carry one.
     */
    private ExclusionReason exclusionReason;

    /**
     * The day-child row this line came from, for a control that wants to change it.
     *
     * Not the same as itemId, which holds the CATALOGUE id: the accommodation, the activity, the
     * tariff. A screen offering "make this one primary" has to address the row on the day, and the
     * catalogue id cannot do that, least of all when the same lodge appears on four nights.
     */
    private String entryId;

    /**
     * The park visit a fee or in-park activity hangs off, where there is one.
     *
     * Those rows are addressed through their visit, not directly off the day, so entryId alone
     * cannot reach them.
     */
    private String parentEntryId;

    /**
     * What choosing this alternative would do to the trip, against the bed currently booked.
     *
     * It is the TRIP delta, not just the night's, and that is not a simplification. Park fees,
     * tariffs and activities are declared per day and do not move when the bed does, so the only
     * figure that changes is this one. Set on accommodation alternatives only.
     */
    private BigDecimal deltaVsPrimarySto;

    private BigDecimal deltaVsPrimaryRack;

    /**
     * True when this alternative sleeps somewhere the day's fees do not describe.
     *
     * Park fees hang off the park VISIT, not off where anybody sleeps. Swap a camp inside the
     * Serengeti for a lodge in Karatu and the concession fee stays on the day, quietly wrong by a
     * fee per person per night. The estimator cannot fix that on its own, so it says so instead.
     */
    private Boolean sleepsElsewhere;

    /**
     * For a per-person item, what each pax band actually pays.
     *
     * The line above is the sum across every band with an averaged unit price,
     * which is what a day breakdown wants to read. It is NOT enough to answer
     * "what does this itinerary cost per adult": an adult park fee and a youth
     * park fee are different rates, and once summed they cannot be told apart.
     * Per-pax mode used to split the sum by headcount, which handed every band
     * the same number no matter what its own rate was.
     *
     * Empty for group, vehicle and per-room items — those genuinely are shared.
     */
    private List<PaxShareDTO> paxShares;

    /** One band's share of a per-person line. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaxShareDTO {
        private String nationCategoryId;
        private String ageCategoryId;
        private String paxCategory;
        private Integer count;
        private BigDecimal stoUnitPrice;
        private BigDecimal rackUnitPrice;
        private BigDecimal stoTotalPrice;
        private BigDecimal rackTotalPrice;
    }
}
