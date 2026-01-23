package com.itineraryledger.kabengosafaris.Itinerary.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * DTO for itinerary cost estimation response.
 * Provides a quick budget overview without creating a formal quotation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItineraryCostEstimationDTO {

    // ========================
    // ITINERARY REFERENCE
    // ========================
    private String itineraryId;
    private String itineraryCode;
    private String itineraryName;
    private Integer totalDays;
    private Integer totalNights;

    // ========================
    // DATE CONTEXT (if provided)
    // ========================
    private LocalDate startDate;
    private LocalDate endDate;
    private String seasonName;

    // ========================
    // PASSENGER SUMMARY
    // ========================
    private Integer totalPax;
    private List<PaxCategorySummary> paxBreakdown;

    // ========================
    // COST BREAKDOWN BY CATEGORY
    // ========================
    private CostBreakdown accommodationCosts;
    private CostBreakdown parkFeeCosts;
    private CostBreakdown activityCosts;

    // ========================
    // DAY-BY-DAY BREAKDOWN
    // ========================
    private List<DayCostSummary> daySummaries;

    // ========================
    // TOTALS
    // ========================
    private String currency;
    private BigDecimal subtotal;
    private BigDecimal perPersonCost;

    // ========================
    // MULTI-CURRENCY VIEW
    // ========================
    private Map<String, BigDecimal> subtotalByCurrency;

    // ========================
    // METADATA
    // ========================
    private String rateType; // "RACK" or "STO" (Special Tour Operator)
    private Boolean hasIncompleteRates;
    private List<String> warnings;
    private String estimatedAt;

    // ========================
    // NESTED DTOs
    // ========================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaxCategorySummary {
        private String nationCategoryId;
        private String nationCategoryName;
        private String ageCategoryId;
        private String ageCategoryName;
        private Integer count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CostBreakdown {
        private BigDecimal total;
        private String currency;
        private Integer itemCount;
        private List<CostLineItem> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CostLineItem {
        private Integer dayNumber;
        private String itemType;
        private String itemName;
        private String referenceId;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private String currency;
        private String paxCategory; // For per-person items
        private String notes;
        private Boolean rateFound;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DayCostSummary {
        private Integer dayNumber;
        private String dayTitle;
        private LocalDate date;
        private String seasonName;
        private BigDecimal accommodationCost;
        private BigDecimal parkFeeCost;
        private BigDecimal activityCost;
        private BigDecimal dayTotal;
        private String currency;
    }
}
