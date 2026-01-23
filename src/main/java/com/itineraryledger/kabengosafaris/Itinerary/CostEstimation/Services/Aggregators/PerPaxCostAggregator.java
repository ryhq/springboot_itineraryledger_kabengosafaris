package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Aggregators;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CostLineItemDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CurrencyGroupedCostDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.PaxCategoryCostDTO;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Aggregator for PER_PAX calculation mode.
 *
 * Aggregates all costs grouped by passenger category:
 * - Per-person costs directly attributed
 * - Group/vehicle costs distributed proportionally among pax categories
 * - Totals grouped by currency (no currency mixing)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PerPaxCostAggregator {

    private final AccommodationCostCalculator accommodationCostCalculator;
    private final ParkTariffCostCalculator parkTariffCostCalculator;
    private final ActivityCostCalculator activityCostCalculator;
    private final SeasonResolverService seasonResolverService;

    /**
     * Aggregate costs by pax category.
     *
     * @param itinerary The full itinerary DTO
     * @param startDate The trip start date
     * @return List of pax category cost details
     */
    public List<PaxCategoryCostDTO> aggregateByPax(
            FullItineraryDTO itinerary,
            LocalDate startDate
    ) {
        List<PaxCategoryCostDTO> paxCostDetails = new ArrayList<>();

        if (itinerary.getPaxList() == null || itinerary.getPaxList().isEmpty()) {
            return paxCostDetails;
        }

        int carCount = itinerary.getCarCount() != null ? itinerary.getCarCount() : 1;
        List<FullItineraryDTO.PaxDTO> paxList = itinerary.getPaxList();
        int totalPax = calculateTotalPax(paxList);

        // Initialize pax category cost containers
        Map<String, PaxCategoryCostDTO> paxCostMap = new LinkedHashMap<>();
        for (FullItineraryDTO.PaxDTO pax : paxList) {
            String key = pax.getNationCategoryId() + "-" + pax.getAgeCategoryId();
            paxCostMap.put(key, PaxCategoryCostDTO.builder()
                .nationCategoryId(pax.getNationCategoryId())
                .nationCategoryName(pax.getNationCategoryName())
                .ageCategoryId(pax.getAgeCategoryId())
                .ageCategoryName(pax.getAgeCategoryName())
                .paxCount(pax.getCount())
                .lineItems(new ArrayList<>())
                .totalsByCurrency(new ArrayList<>())
                .build());
        }

        // Process all days
        if (itinerary.getDays() != null) {
            for (FullItineraryDTO.DayDTO day : itinerary.getDays()) {
                LocalDate dayDate = startDate.plusDays(day.getDayNumber() - 1);
                Season globalSeason = seasonResolverService.resolveGlobalSeason(dayDate);

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

                // Distribute costs to pax categories
                distributeItemsToPax(allItems, paxCostMap, paxList, totalPax, carCount);
            }
        }

        // Calculate totals by currency for each pax category
        for (PaxCategoryCostDTO paxCost : paxCostMap.values()) {
            paxCost.setTotalsByCurrency(calculateTotalsByCurrency(paxCost.getLineItems()));
        }

        return new ArrayList<>(paxCostMap.values());
    }

    /**
     * Calculate grand totals by currency from all pax cost details.
     *
     * @param paxCostDetails List of pax category cost details
     * @return List of currency grouped costs representing grand totals
     */
    public List<CurrencyGroupedCostDTO> calculateGrandTotals(List<PaxCategoryCostDTO> paxCostDetails) {
        Map<String, CurrencyGroupedCostDTO> totalsMap = new HashMap<>();

        for (PaxCategoryCostDTO paxDetail : paxCostDetails) {
            if (paxDetail.getTotalsByCurrency() == null) {
                continue;
            }

            for (CurrencyGroupedCostDTO paxTotal : paxDetail.getTotalsByCurrency()) {
                String currency = paxTotal.getCurrency();
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

                // Add pax totals to grand total
                grandTotal.addAccommodationCost(paxTotal.getAccommodationSto(), paxTotal.getAccommodationRack());
                grandTotal.addParkFeeCost(paxTotal.getParkFeesSto(), paxTotal.getParkFeesRack());
                grandTotal.addActivityCost(paxTotal.getActivitiesSto(), paxTotal.getActivitiesRack());
            }
        }

        // Calculate grand totals
        List<CurrencyGroupedCostDTO> result = new ArrayList<>(totalsMap.values());
        result.forEach(CurrencyGroupedCostDTO::calculateGrandTotals);

        return result;
    }

    /**
     * Distribute cost items to pax categories.
     *
     * Per-person items: Distributed based on the item's pax category.
     * Group/vehicle items: Distributed proportionally based on pax count.
     */
    private void distributeItemsToPax(
            List<CostLineItemDTO> items,
            Map<String, PaxCategoryCostDTO> paxCostMap,
            List<FullItineraryDTO.PaxDTO> paxList,
            int totalPax,
            int carCount
    ) {
        for (CostLineItemDTO item : items) {
            String chargingBasis = item.getChargingBasis();

            if (isPerPersonCharging(chargingBasis)) {
                // Per-person items: distribute proportionally to each pax category
                // Since the calculator already sums up all pax, we distribute based on count
                for (FullItineraryDTO.PaxDTO pax : paxList) {
                    String key = pax.getNationCategoryId() + "-" + pax.getAgeCategoryId();
                    PaxCategoryCostDTO paxCost = paxCostMap.get(key);
                    if (paxCost == null) continue;

                    int paxCount = pax.getCount() != null ? pax.getCount() : 0;
                    if (paxCount == 0 || totalPax == 0) continue;

                    // Calculate proportional cost for this pax category
                    BigDecimal proportion = BigDecimal.valueOf(paxCount)
                        .divide(BigDecimal.valueOf(totalPax), 10, RoundingMode.HALF_UP);

                    CostLineItemDTO paxItem = createProportionalItem(item, proportion, paxCount);
                    paxCost.getLineItems().add(paxItem);
                }
            } else {
                // Group/vehicle/flat items: distribute proportionally
                for (FullItineraryDTO.PaxDTO pax : paxList) {
                    String key = pax.getNationCategoryId() + "-" + pax.getAgeCategoryId();
                    PaxCategoryCostDTO paxCost = paxCostMap.get(key);
                    if (paxCost == null) continue;

                    int paxCount = pax.getCount() != null ? pax.getCount() : 0;
                    if (paxCount == 0 || totalPax == 0) continue;

                    // Proportional share based on pax count
                    BigDecimal proportion = BigDecimal.valueOf(paxCount)
                        .divide(BigDecimal.valueOf(totalPax), 10, RoundingMode.HALF_UP);

                    CostLineItemDTO paxItem = createProportionalItem(item, proportion, paxCount);
                    paxItem.setNotes(item.getNotes() + " (Share: " + paxCount + "/" + totalPax + ")");
                    paxCost.getLineItems().add(paxItem);
                }
            }
        }
    }

    /**
     * Create a proportional cost item for a specific pax category.
     */
    private CostLineItemDTO createProportionalItem(CostLineItemDTO original, BigDecimal proportion, int paxCount) {
        BigDecimal stoTotal = original.getStoTotalPrice() != null
            ? original.getStoTotalPrice().multiply(proportion).setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        BigDecimal rackTotal = original.getRackTotalPrice() != null
            ? original.getRackTotalPrice().multiply(proportion).setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // Calculate per-person unit price for this category
        BigDecimal stoUnit = paxCount > 0
            ? stoTotal.divide(BigDecimal.valueOf(paxCount), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        BigDecimal rackUnit = paxCount > 0
            ? rackTotal.divide(BigDecimal.valueOf(paxCount), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return CostLineItemDTO.builder()
            .dayNumber(original.getDayNumber())
            .itemType(original.getItemType())
            .itemName(original.getItemName())
            .itemId(original.getItemId())
            .chargingBasis(original.getChargingBasis())
            .quantity(paxCount)
            .stoUnitPrice(stoUnit)
            .rackUnitPrice(rackUnit)
            .stoTotalPrice(stoTotal)
            .rackTotalPrice(rackTotal)
            .currency(original.getCurrency())
            .paxCategory(original.getPaxCategory())
            .notes(original.getNotes())
            .build();
    }

    /**
     * Check if the charging basis is per-person.
     */
    private boolean isPerPersonCharging(String chargingBasis) {
        if (chargingBasis == null) return false;
        return chargingBasis.equalsIgnoreCase("PER_PERSON")
            || chargingBasis.equalsIgnoreCase("Per Person")
            || chargingBasis.equalsIgnoreCase("Per Person Sharing");
    }

    /**
     * Calculate total pax count from pax list.
     */
    private int calculateTotalPax(List<FullItineraryDTO.PaxDTO> paxList) {
        if (paxList == null || paxList.isEmpty()) {
            return 0;
        }

        return paxList.stream()
            .mapToInt(p -> p.getCount() != null ? p.getCount() : 0)
            .sum();
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
