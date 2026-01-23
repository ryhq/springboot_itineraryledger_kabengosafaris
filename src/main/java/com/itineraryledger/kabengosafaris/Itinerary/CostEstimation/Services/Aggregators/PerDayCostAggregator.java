package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Aggregators;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CostLineItemDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CurrencyGroupedCostDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.DayCostDetailDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CostItemType;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Calculators.AccommodationCostCalculator;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Calculators.ActivityCostCalculator;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Calculators.ParkTariffCostCalculator;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core.SeasonResolverService;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO;
import com.itineraryledger.kabengosafaris.Season.Season;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregator for PER_DAY calculation mode.
 *
 * Aggregates all costs grouped by day:
 * - All line items (accommodation, park fees, activities) for each day
 * - Totals grouped by currency (no currency mixing)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PerDayCostAggregator {

    private final AccommodationCostCalculator accommodationCostCalculator;
    private final ParkTariffCostCalculator parkTariffCostCalculator;
    private final ActivityCostCalculator activityCostCalculator;
    private final SeasonResolverService seasonResolverService;

    /**
     * Aggregate costs by day.
     *
     * @param itinerary The full itinerary DTO
     * @param startDate The trip start date
     * @return List of day cost details
     */
    public List<DayCostDetailDTO> aggregateByDay(
            FullItineraryDTO itinerary,
            LocalDate startDate
    ) {
        List<DayCostDetailDTO> dayCostDetails = new ArrayList<>();

        if (itinerary.getDays() == null || itinerary.getDays().isEmpty()) {
            return dayCostDetails;
        }

        int carCount = itinerary.getCarCount() != null ? itinerary.getCarCount() : 1;
        List<FullItineraryDTO.PaxDTO> paxList = itinerary.getPaxList();

        for (FullItineraryDTO.DayDTO day : itinerary.getDays()) {
            LocalDate dayDate = startDate.plusDays(day.getDayNumber() - 1);
            Season globalSeason = seasonResolverService.resolveGlobalSeason(dayDate);
            String seasonName = globalSeason != null ? globalSeason.getName() : "Unknown Season";

            // Calculate all costs for this day
            List<CostLineItemDTO> accommodationItems = accommodationCostCalculator.calculateForDay(
                day, dayDate, paxList
            );

            List<CostLineItemDTO> parkFeeItems = parkTariffCostCalculator.calculateForDay(
                day, dayDate, globalSeason, paxList, carCount
            );

            List<CostLineItemDTO> activityItems = activityCostCalculator.calculateForDay(
                day, dayDate, globalSeason, paxList, carCount
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
                .date(dayDate)
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
