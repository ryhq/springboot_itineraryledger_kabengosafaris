package com.itineraryledger.kabengosafaris.Safari.CostEstimation.Services.Calculators;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRate;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CostLineItemDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CostItemType;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core.AccommodationRateLookupService;
import com.itineraryledger.kabengosafaris.Safari.DTOs.FullSafariDTO;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Calculator for safari accommodation costs.
 *
 * - Skips non-overnight days
 * - Skips alternative accommodations
 * - Supports per-person (PPS) and per-room pricing
 * - Returns both STO and Rack rates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SafariAccommodationCostCalculator {

    private final AccommodationRateLookupService accommodationRateLookupService;
    private final IdObfuscator idObfuscator;

    private static final String DEFAULT_CURRENCY = "USD";

    /**
     * Calculate accommodation costs for a day.
     *
     * @param day The day DTO
     * @param dayDate The actual date
     * @param paxList Passenger list for calculating per-person rates
     * @return List of cost line items for accommodations
     */
    public List<CostLineItemDTO> calculateForDay(
            FullSafariDTO.DayDTO day,
            LocalDate dayDate,
            List<FullSafariDTO.PaxDTO> paxList
    ) {
        List<CostLineItemDTO> items = new ArrayList<>();

        // Skip non-overnight days (e.g., departure day)
        if (day.getIsOvernight() != null && !day.getIsOvernight()) {
            return items;
        }

        if (day.getAccommodations() == null || day.getAccommodations().isEmpty()) {
            return items;
        }

        // Calculate total pax count
        int totalPax = calculateTotalPax(paxList);

        for (FullSafariDTO.DayAccommodationDTO accommodation : day.getAccommodations()) {
            // Skip alternative accommodations
            if (Boolean.TRUE.equals(accommodation.getIsAlternative())) {
                continue;
            }

            CostLineItemDTO lineItem = calculateAccommodationCost(
                day.getDayNumber(),
                accommodation,
                dayDate,
                totalPax
            );

            if (lineItem != null) {
                items.add(lineItem);
            }
        }

        return items;
    }

    /**
     * Calculate cost for a single accommodation.
     */
    private CostLineItemDTO calculateAccommodationCost(
            Integer dayNumber,
            FullSafariDTO.DayAccommodationDTO accommodation,
            LocalDate dayDate,
            int totalPax
    ) {
        Long accommodationId = idObfuscator.decodeId(accommodation.getAccommodationId());
        Long roomTypeId = idObfuscator.decodeId(accommodation.getRoomTypeId());
        Long roomStandardId = idObfuscator.decodeId(accommodation.getRoomStandardId());
        Long boardTypeId = idObfuscator.decodeId(accommodation.getBoardTypeId());

        if (accommodationId == null) {
            return null;
        }

        // Lookup rate with automatic season resolution
        AccommodationRateLookupService.LookupResult result = accommodationRateLookupService
            .lookupRateWithIssueLogging(
                accommodationId,
                accommodation.getAccommodationName(),
                roomTypeId,
                roomStandardId,
                boardTypeId,
                dayDate,
                dayNumber
            );

        if (!result.found()) {
            // Issue already logged by the lookup service
            return buildNotFoundLineItem(dayNumber, accommodation);
        }

        AccommodationRate rate = result.rate();
        int roomCount = accommodation.getRoomCount() != null ? accommodation.getRoomCount() : 1;

        // Determine pricing mode
        boolean isPerPerson = rate.getIsPerPerson() != null && rate.getIsPerPerson();
        int quantity = isPerPerson ? totalPax : roomCount;
        String chargingBasis = isPerPerson ? "Per Person Sharing" : "Per Room";
        String notes = accommodation.getBoardTypeName() + " (" + chargingBasis + "/Night)";

        // Get rates
        BigDecimal stoUnitPrice = rate.getStoRate() != null ? rate.getStoRate() : BigDecimal.ZERO;
        BigDecimal rackUnitPrice = rate.getRackRate() != null ? rate.getRackRate() : BigDecimal.ZERO;

        // Calculate totals
        BigDecimal stoTotal = stoUnitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal rackTotal = rackUnitPrice.multiply(BigDecimal.valueOf(quantity));

        String currency = rate.getCurrency() != null ? rate.getCurrency() : DEFAULT_CURRENCY;

        return CostLineItemDTO.builder()
            .dayNumber(dayNumber)
            .itemType(CostItemType.ACCOMMODATION)
            .itemName(accommodation.getAccommodationName() + " - " + accommodation.getRoomTypeName())
            .itemId(accommodation.getAccommodationId())
            .chargingBasis(chargingBasis)
            .quantity(quantity)
            .stoUnitPrice(stoUnitPrice)
            .rackUnitPrice(rackUnitPrice)
            .stoTotalPrice(stoTotal)
            .rackTotalPrice(rackTotal)
            .currency(currency)
            .notes(notes)
            .build();
    }

    /**
     * Build a line item for an accommodation where the rate was not found.
     */
    private CostLineItemDTO buildNotFoundLineItem(
            Integer dayNumber,
            FullSafariDTO.DayAccommodationDTO accommodation
    ) {
        return CostLineItemDTO.builder()
            .dayNumber(dayNumber)
            .itemType(CostItemType.ACCOMMODATION)
            .itemName(accommodation.getAccommodationName() + " - " + accommodation.getRoomTypeName())
            .itemId(accommodation.getAccommodationId())
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

    /**
     * Calculate total pax count from pax list.
     */
    private int calculateTotalPax(List<FullSafariDTO.PaxDTO> paxList) {
        if (paxList == null || paxList.isEmpty()) {
            return 0;
        }

        return paxList.stream()
            .mapToInt(p -> p.getCount() != null ? p.getCount() : 0)
            .sum();
    }
}
