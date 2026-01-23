package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Calculators;

import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CostLineItemDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CostItemType;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core.ParkTariffRateLookupService;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariff;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariffRepository;
import com.itineraryledger.kabengosafaris.ParkTariffRate.ParkTariffRate;
import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Repositories.PaxNationCategoryRepository;
import com.itineraryledger.kabengosafaris.Season.Season;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Calculator for park tariff costs.
 *
 * - Supports PER_PERSON, PER_VEHICLE, PER_GROUP, FLAT_RATE charging bases
 * - Uses highest priority nation category for group/vehicle rates
 * - Returns both STO and Rack rates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParkTariffCostCalculator {

    private final ParkTariffRateLookupService parkTariffRateLookupService;
    private final ParkTariffRepository parkTariffRepository;
    private final PaxNationCategoryRepository nationCategoryRepository;
    private final IdObfuscator idObfuscator;

    private static final String DEFAULT_CURRENCY = "USD";

    /**
     * Calculate park tariff costs for a day.
     *
     * @param day The day DTO
     * @param dayDate The actual date
     * @param season The applicable season
     * @param paxList Passenger list
     * @param carCount Number of vehicles
     * @return List of cost line items for park tariffs
     */
    public List<CostLineItemDTO> calculateForDay(
            FullItineraryDTO.DayDTO day,
            LocalDate dayDate,
            Season season,
            List<FullItineraryDTO.PaxDTO> paxList,
            int carCount
    ) {
        List<CostLineItemDTO> items = new ArrayList<>();

        if (day.getParks() == null || day.getParks().isEmpty()) {
            return items;
        }

        for (FullItineraryDTO.DayParkDTO park : day.getParks()) {
            if (park.getTariffs() == null || park.getTariffs().isEmpty()) {
                continue;
            }

            Long parkId = idObfuscator.decodeId(park.getParkId());
            if (parkId == null) {
                continue;
            }

            for (FullItineraryDTO.ParkTariffDTO tariff : park.getTariffs()) {
                // Skip if not included in price
                if (tariff.getIsIncludedInPrice() != null && !tariff.getIsIncludedInPrice()) {
                    continue;
                }

                Long tariffId = idObfuscator.decodeId(tariff.getTariffId());
                if (tariffId == null) {
                    continue;
                }

                CostLineItemDTO lineItem = calculateTariffCost(
                    day.getDayNumber(),
                    parkId,
                    park.getParkName(),
                    tariffId,
                    tariff.getTariffName(),
                    season,
                    paxList,
                    carCount
                );

                if (lineItem != null) {
                    items.add(lineItem);
                }
            }
        }

        return items;
    }

    /**
     * Calculate cost for a single park tariff.
     */
    private CostLineItemDTO calculateTariffCost(
            Integer dayNumber,
            Long parkId,
            String parkName,
            Long tariffId,
            String tariffName,
            Season season,
            List<FullItineraryDTO.PaxDTO> paxList,
            int carCount
    ) {
        // Get the ParkTariff to determine charging basis
        Optional<ParkTariff> parkTariffOpt = parkTariffRepository.findByParkIdAndTariffId(parkId, tariffId);
        if (parkTariffOpt.isEmpty()) {
            return buildNotFoundLineItem(dayNumber, parkName, tariffName);
        }

        ParkTariff parkTariff = parkTariffOpt.get();
        ChargingBasis chargingBasis = parkTariff.getTariff().getChargingBasis();

        if (chargingBasis == ChargingBasis.PER_PERSON) {
            return calculatePerPersonTariffCost(
                dayNumber, parkId, parkName, tariffId, tariffName, season, paxList
            );
        } else if (chargingBasis == ChargingBasis.PER_VEHICLE) {
            return calculateGroupTariffCost(
                dayNumber, parkId, parkName, tariffId, tariffName, season, paxList, carCount, chargingBasis
            );
        } else {
            // PER_GROUP, FLAT_RATE, PER_DAY, etc.
            return calculateGroupTariffCost(
                dayNumber, parkId, parkName, tariffId, tariffName, season, paxList, 1, chargingBasis
            );
        }
    }

    /**
     * Calculate per-person tariff cost (sums up for all pax categories).
     */
    private CostLineItemDTO calculatePerPersonTariffCost(
            Integer dayNumber,
            Long parkId,
            String parkName,
            Long tariffId,
            String tariffName,
            Season season,
            List<FullItineraryDTO.PaxDTO> paxList
    ) {
        if (paxList == null || paxList.isEmpty()) {
            return buildNotFoundLineItem(dayNumber, parkName, tariffName);
        }

        BigDecimal stoTotal = BigDecimal.ZERO;
        BigDecimal rackTotal = BigDecimal.ZERO;
        String currency = DEFAULT_CURRENCY;
        int totalPaxCount = 0;
        boolean anyRateFound = false;

        for (FullItineraryDTO.PaxDTO pax : paxList) {
            Long nationCategoryId = idObfuscator.decodeId(pax.getNationCategoryId());
            Long ageCategoryId = idObfuscator.decodeId(pax.getAgeCategoryId());
            int count = pax.getCount() != null ? pax.getCount() : 0;

            if (nationCategoryId == null || ageCategoryId == null || count == 0) {
                continue;
            }

            String paxCategory = pax.getNationCategoryName() + " " + pax.getAgeCategoryName();

            ParkTariffRateLookupService.LookupResult result = parkTariffRateLookupService
                .lookupPersonRateWithIssueLogging(
                    parkId, parkName, tariffId, tariffName, season,
                    nationCategoryId, ageCategoryId, paxCategory, dayNumber
                );

            if (result.found()) {
                ParkTariffRate rate = result.rate();
                BigDecimal stoUnit = rate.getStoRate() != null ? rate.getStoRate() : BigDecimal.ZERO;
                BigDecimal rackUnit = rate.getRackRate() != null ? rate.getRackRate() : BigDecimal.ZERO;

                stoTotal = stoTotal.add(stoUnit.multiply(BigDecimal.valueOf(count)));
                rackTotal = rackTotal.add(rackUnit.multiply(BigDecimal.valueOf(count)));
                totalPaxCount += count;
                currency = rate.getCurrency() != null ? rate.getCurrency() : DEFAULT_CURRENCY;
                anyRateFound = true;
            }
        }

        if (!anyRateFound) {
            return buildNotFoundLineItem(dayNumber, parkName, tariffName);
        }

        // Calculate average unit price for display
        BigDecimal stoUnitPrice = totalPaxCount > 0
            ? stoTotal.divide(BigDecimal.valueOf(totalPaxCount), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        BigDecimal rackUnitPrice = totalPaxCount > 0
            ? rackTotal.divide(BigDecimal.valueOf(totalPaxCount), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return CostLineItemDTO.builder()
            .dayNumber(dayNumber)
            .itemType(CostItemType.PARK_FEE)
            .itemName(tariffName + " - " + parkName)
            .itemId(idObfuscator.encodeId(tariffId))
            .chargingBasis(ChargingBasis.PER_PERSON.getDisplayName())
            .quantity(totalPaxCount)
            .stoUnitPrice(stoUnitPrice)
            .rackUnitPrice(rackUnitPrice)
            .stoTotalPrice(stoTotal)
            .rackTotalPrice(rackTotal)
            .currency(currency)
            .paxCategory("Per Person")
            .build();
    }

    /**
     * Calculate group/vehicle tariff cost.
     */
    private CostLineItemDTO calculateGroupTariffCost(
            Integer dayNumber,
            Long parkId,
            String parkName,
            Long tariffId,
            String tariffName,
            Season season,
            List<FullItineraryDTO.PaxDTO> paxList,
            int quantity,
            ChargingBasis chargingBasis
    ) {
        // Find highest priority nation category
        Long highestPriorityNationId = findHighestPriorityNationCategory(paxList);

        if (highestPriorityNationId == null) {
            return buildNotFoundLineItem(dayNumber, parkName, tariffName);
        }

        String chargingDesc = chargingBasis.getDisplayName();

        ParkTariffRateLookupService.LookupResult result = parkTariffRateLookupService
            .lookupGroupRateWithIssueLogging(
                parkId, parkName, tariffId, tariffName, season,
                highestPriorityNationId, chargingDesc, dayNumber
            );

        if (!result.found()) {
            return buildNotFoundLineItem(dayNumber, parkName, tariffName);
        }

        ParkTariffRate rate = result.rate();
        BigDecimal stoUnitPrice = rate.getStoRate() != null ? rate.getStoRate() : BigDecimal.ZERO;
        BigDecimal rackUnitPrice = rate.getRackRate() != null ? rate.getRackRate() : BigDecimal.ZERO;

        BigDecimal stoTotal = stoUnitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal rackTotal = rackUnitPrice.multiply(BigDecimal.valueOf(quantity));

        String currency = rate.getCurrency() != null ? rate.getCurrency() : DEFAULT_CURRENCY;
        String notes = quantity > 1 ? String.format("%s x %d", chargingDesc, quantity) : chargingDesc;

        return CostLineItemDTO.builder()
            .dayNumber(dayNumber)
            .itemType(CostItemType.PARK_FEE)
            .itemName(tariffName + " - " + parkName)
            .itemId(idObfuscator.encodeId(tariffId))
            .chargingBasis(chargingBasis.name())
            .quantity(quantity)
            .stoUnitPrice(stoUnitPrice)
            .rackUnitPrice(rackUnitPrice)
            .stoTotalPrice(stoTotal)
            .rackTotalPrice(rackTotal)
            .currency(currency)
            .paxCategory(chargingDesc)
            .notes(notes)
            .build();
    }

    /**
     * Find the highest priority nation category from pax list.
     * Higher priorityFactor = higher priority (e.g., Non-Resident > East African > Resident)
     */
    private Long findHighestPriorityNationCategory(List<FullItineraryDTO.PaxDTO> paxList) {
        if (paxList == null || paxList.isEmpty()) {
            return null;
        }

        Long highestPriorityId = null;
        int highestPriority = Integer.MIN_VALUE;

        for (FullItineraryDTO.PaxDTO pax : paxList) {
            Long nationCategoryId = idObfuscator.decodeId(pax.getNationCategoryId());
            if (nationCategoryId == null) {
                continue;
            }

            Optional<PaxNationCategory> categoryOpt = nationCategoryRepository.findById(nationCategoryId);
            if (categoryOpt.isPresent()) {
                PaxNationCategory category = categoryOpt.get();
                int priority = category.getPriorityFactor() != null ? category.getPriorityFactor() : 0;
                if (priority > highestPriority) {
                    highestPriority = priority;
                    highestPriorityId = nationCategoryId;
                }
            }
        }

        // Fallback to first if no priority found
        if (highestPriorityId == null && !paxList.isEmpty()) {
            highestPriorityId = idObfuscator.decodeId(paxList.get(0).getNationCategoryId());
        }

        return highestPriorityId;
    }

    /**
     * Build a line item for a tariff where the rate was not found.
     */
    private CostLineItemDTO buildNotFoundLineItem(
            Integer dayNumber,
            String parkName,
            String tariffName
    ) {
        return CostLineItemDTO.builder()
            .dayNumber(dayNumber)
            .itemType(CostItemType.PARK_FEE)
            .itemName(tariffName + " - " + parkName)
            .chargingBasis("Unknown")
            .quantity(0)
            .stoUnitPrice(BigDecimal.ZERO)
            .rackUnitPrice(BigDecimal.ZERO)
            .stoTotalPrice(BigDecimal.ZERO)
            .rackTotalPrice(BigDecimal.ZERO)
            .currency(DEFAULT_CURRENCY)
            .notes("Rate not found")
            .build();
    }
}
