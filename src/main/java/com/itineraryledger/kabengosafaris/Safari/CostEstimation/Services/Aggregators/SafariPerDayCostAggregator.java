package com.itineraryledger.kabengosafaris.Safari.CostEstimation.Services.Aggregators;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CostLineItemDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CurrencyGroupedCostDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.DayCostDetailDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CostItemType;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core.SeasonResolverService;
import com.itineraryledger.kabengosafaris.Safari.CostEstimation.Services.Calculators.SafariAccommodationCostCalculator;
import com.itineraryledger.kabengosafaris.Safari.CostEstimation.Services.Calculators.SafariActivityCostCalculator;
import com.itineraryledger.kabengosafaris.Safari.CostEstimation.Services.Calculators.SafariParkTariffCostCalculator;
import com.itineraryledger.kabengosafaris.Safari.DTOs.FullSafariDTO;
import com.itineraryledger.kabengosafaris.Season.Season;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregator for PER_DAY calculation mode for Safari.
 *
 * Aggregates all costs grouped by day:
 * - All line items (accommodation, park fees, activities) for each day
 * - Totals grouped by currency (no currency mixing)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SafariPerDayCostAggregator {

    private final SafariAccommodationCostCalculator safariAccommodationCostCalculator;
    private final SafariParkTariffCostCalculator safariParkTariffCostCalculator;
    private final SafariActivityCostCalculator safariActivityCostCalculator;
    private final SeasonResolverService seasonResolverService;

    /**
     * Aggregate costs by day.
     *
     * @param safari The full safari DTO
     * @return List of day cost details
     */
    public List<DayCostDetailDTO> aggregateByDay(FullSafariDTO safari) {
        List<DayCostDetailDTO> dayCostDetails = new ArrayList<>();

        if (safari.getDays() == null || safari.getDays().isEmpty()) {
            return dayCostDetails;
        }

        int carCount = safari.getCarCount() != null ? safari.getCarCount() : 1;
        List<FullSafariDTO.PaxDTO> paxList = safari.getPaxList();

        for (FullSafariDTO.DayDTO day : safari.getDays()) {
            // Safari days already have dates from the entity
            Season globalSeason = seasonResolverService.resolveGlobalSeason(day.getDate());
            String seasonName = globalSeason != null ? globalSeason.getName() : "Unknown Season";

            // Calculate all costs for this day
            List<CostLineItemDTO> accommodationItems = safariAccommodationCostCalculator.calculateForDay(
                day, day.getDate(), paxList
            );

            List<CostLineItemDTO> parkFeeItems = safariParkTariffCostCalculator.calculateForDay(
                day, day.getDate(), globalSeason, paxList, carCount
            );

            List<CostLineItemDTO> activityItems = safariActivityCostCalculator.calculateForDay(
                day, day.getDate(), globalSeason, paxList, carCount
            );

            // Combine all line items
            List<CostLineItemDTO> allItems = new ArrayList<>();
            allItems.addAll(accommodationItems);
            allItems.addAll(parkFeeItems);
            allItems.addAll(activityItems);

            // Calculate totals by currency
            List<CurrencyGroupedCostDTO> totalsByCurrency = calculateTotalsByCurrency(allItems);

            // Build day cost detail
            DayCostDetailDTO dayDetail = DayCostDetailDTO.builder()
                .dayNumber(day.getDayNumber())
                .dayTitle(day.getTitle())
                .date(day.getDate())
                .seasonName(seasonName)
                .isOvernight(day.getIsOvernight())
                .lineItems(allItems)
                .totalsByCurrency(totalsByCurrency)
                .build();

            dayCostDetails.add(dayDetail);
        }

        return dayCostDetails;
    }

    /**
     * Calculate grand totals by currency from all day details.
     *
     * @param dayCostDetails List of day cost details
     * @return List of currency grouped costs representing grand totals
     */
    public List<CurrencyGroupedCostDTO> calculateGrandTotals(List<DayCostDetailDTO> dayCostDetails) {
        Map<String, CurrencyGroupedCostDTO> totalsMap = new HashMap<>();

        for (DayCostDetailDTO dayDetail : dayCostDetails) {
            if (dayDetail.getTotalsByCurrency() == null) {
                continue;
            }

            for (CurrencyGroupedCostDTO dayTotal : dayDetail.getTotalsByCurrency()) {
                String currency = dayTotal.getCurrency();
                CurrencyGroupedCostDTO grandTotal = totalsMap.computeIfAbsent(currency, k ->
                    CurrencyGroupedCostDTO.builder()
                        .currency(k)
                        .accommodationSto(BigDecimal.ZERO)
                        .accommodationRack(BigDecimal.ZERO)
                        .parkFeesSto(BigDecimal.ZERO)
                        .parkFeesRack(BigDecimal.ZERO)
                        .activitiesSto(BigDecimal.ZERO)
                        .activitiesRack(BigDecimal.ZERO)
                        .grandTotalSto(BigDecimal.ZERO)
                        .grandTotalRack(BigDecimal.ZERO)
                        .build()
                );

                // Add day totals to grand total
                grandTotal.addAccommodationCost(dayTotal.getAccommodationSto(), dayTotal.getAccommodationRack());
                grandTotal.addParkFeeCost(dayTotal.getParkFeesSto(), dayTotal.getParkFeesRack());
                grandTotal.addActivityCost(dayTotal.getActivitiesSto(), dayTotal.getActivitiesRack());
            }
        }

        // Calculate grand totals
        List<CurrencyGroupedCostDTO> result = new ArrayList<>(totalsMap.values());
        result.forEach(CurrencyGroupedCostDTO::calculateGrandTotals);

        return result;
    }

    /**
     * Calculate totals by currency for a list of line items.
     */
    private List<CurrencyGroupedCostDTO> calculateTotalsByCurrency(List<CostLineItemDTO> items) {
        Map<String, CurrencyGroupedCostDTO> currencyTotals = new HashMap<>();

        for (CostLineItemDTO item : items) {
            String currency = item.getCurrency() != null ? item.getCurrency() : "USD";

            CurrencyGroupedCostDTO totals = currencyTotals.computeIfAbsent(currency, k ->
                CurrencyGroupedCostDTO.builder()
                    .currency(k)
                    .accommodationSto(BigDecimal.ZERO)
                    .accommodationRack(BigDecimal.ZERO)
                    .parkFeesSto(BigDecimal.ZERO)
                    .parkFeesRack(BigDecimal.ZERO)
                    .activitiesSto(BigDecimal.ZERO)
                    .activitiesRack(BigDecimal.ZERO)
                    .grandTotalSto(BigDecimal.ZERO)
                    .grandTotalRack(BigDecimal.ZERO)
                    .build()
            );

            // Add to appropriate category
            BigDecimal stoPrice = item.getStoTotalPrice() != null ? item.getStoTotalPrice() : BigDecimal.ZERO;
            BigDecimal rackPrice = item.getRackTotalPrice() != null ? item.getRackTotalPrice() : BigDecimal.ZERO;

            if (item.getItemType() == CostItemType.ACCOMMODATION) {
                totals.addAccommodationCost(stoPrice, rackPrice);
            } else if (item.getItemType() == CostItemType.PARK_FEE) {
                totals.addParkFeeCost(stoPrice, rackPrice);
            } else if (item.getItemType() == CostItemType.ACTIVITY) {
                totals.addActivityCost(stoPrice, rackPrice);
            }
        }

        // Calculate grand totals for each currency
        List<CurrencyGroupedCostDTO> result = new ArrayList<>(currencyTotals.values());
        result.forEach(CurrencyGroupedCostDTO::calculateGrandTotals);

        return result;
    }
}
