package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for costs grouped by currency.
 * Contains STO and Rack totals for each cost category.
 * No currency mixing - all values are in the specified currency.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrencyGroupedCostDTO {

    /**
     * Currency code (e.g., "USD", "TZS", "KES")
     */
    private String currency;

    // ========================
    // ACCOMMODATION
    // ========================

    /**
     * STO total for accommodation
     */
    @Builder.Default
    private BigDecimal accommodationSto = BigDecimal.ZERO;

    /**
     * Rack total for accommodation
     */
    @Builder.Default
    private BigDecimal accommodationRack = BigDecimal.ZERO;

    // ========================
    // PARK FEES
    // ========================

    /**
     * STO total for park fees
     */
    @Builder.Default
    private BigDecimal parkFeesSto = BigDecimal.ZERO;

    /**
     * Rack total for park fees
     */
    @Builder.Default
    private BigDecimal parkFeesRack = BigDecimal.ZERO;

    // ========================
    // ACTIVITIES
    // ========================

    /**
     * STO total for activities
     */
    @Builder.Default
    private BigDecimal activitiesSto = BigDecimal.ZERO;

    /**
     * Rack total for activities
     */
    @Builder.Default
    private BigDecimal activitiesRack = BigDecimal.ZERO;

    // ========================
    // GRAND TOTALS
    // ========================

    /**
     * Combined STO total (accommodation + park fees + activities)
     */
    @Builder.Default
    private BigDecimal grandTotalSto = BigDecimal.ZERO;

    /**
     * Combined Rack total (accommodation + park fees + activities)
     */
    @Builder.Default
    private BigDecimal grandTotalRack = BigDecimal.ZERO;

    // ========================
    // HELPER METHODS
    // ========================

    /**
     * Recalculate grand totals from individual components
     */
    public void calculateGrandTotals() {
        this.grandTotalSto = (accommodationSto != null ? accommodationSto : BigDecimal.ZERO)
            .add(parkFeesSto != null ? parkFeesSto : BigDecimal.ZERO)
            .add(activitiesSto != null ? activitiesSto : BigDecimal.ZERO);

        this.grandTotalRack = (accommodationRack != null ? accommodationRack : BigDecimal.ZERO)
            .add(parkFeesRack != null ? parkFeesRack : BigDecimal.ZERO)
            .add(activitiesRack != null ? activitiesRack : BigDecimal.ZERO);
    }

    /**
     * Add accommodation costs
     */
    public void addAccommodationCost(BigDecimal sto, BigDecimal rack) {
        this.accommodationSto = (this.accommodationSto != null ? this.accommodationSto : BigDecimal.ZERO)
            .add(sto != null ? sto : BigDecimal.ZERO);
        this.accommodationRack = (this.accommodationRack != null ? this.accommodationRack : BigDecimal.ZERO)
            .add(rack != null ? rack : BigDecimal.ZERO);
    }

    /**
     * Add park fee costs
     */
    public void addParkFeeCost(BigDecimal sto, BigDecimal rack) {
        this.parkFeesSto = (this.parkFeesSto != null ? this.parkFeesSto : BigDecimal.ZERO)
            .add(sto != null ? sto : BigDecimal.ZERO);
        this.parkFeesRack = (this.parkFeesRack != null ? this.parkFeesRack : BigDecimal.ZERO)
            .add(rack != null ? rack : BigDecimal.ZERO);
    }

    /**
     * Add activity costs
     */
    public void addActivityCost(BigDecimal sto, BigDecimal rack) {
        this.activitiesSto = (this.activitiesSto != null ? this.activitiesSto : BigDecimal.ZERO)
            .add(sto != null ? sto : BigDecimal.ZERO);
        this.activitiesRack = (this.activitiesRack != null ? this.activitiesRack : BigDecimal.ZERO)
            .add(rack != null ? rack : BigDecimal.ZERO);
    }
}
