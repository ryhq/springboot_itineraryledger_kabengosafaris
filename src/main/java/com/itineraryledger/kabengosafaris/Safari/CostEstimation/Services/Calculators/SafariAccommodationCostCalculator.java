package com.itineraryledger.kabengosafaris.Safari.CostEstimation.Services.Calculators;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRate;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CostLineItemDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CostItemType;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.ExclusionReason;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core.AccommodationRateLookupService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark.ParkEntryType;
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

    /**
     * The beds this day could have used instead, priced.
     *
     * Reported as a difference from the booked bed, and because the rest of the day is invariant
     * -- fees and activities are declared per day and do not follow the bed -- that difference is
     * also the difference to the whole safari. A day with alternatives and no primary yet reports
     * them with no delta rather than a delta from zero, which would read as though every option
     * added its full cost.
     */
    public List<CostLineItemDTO> calculateExcludedForDay(
            FullSafariDTO.DayDTO day,
            LocalDate dayDate,
            List<FullSafariDTO.PaxDTO> paxList
    ) {
        List<CostLineItemDTO> excluded = new ArrayList<>();

        if (day.getIsOvernight() != null && !day.getIsOvernight()) {
            return excluded;
        }
        if (day.getAccommodations() == null || day.getAccommodations().isEmpty()) {
            return excluded;
        }

        int totalPax = calculateTotalPax(paxList);

        CostLineItemDTO primary = null;
        for (FullSafariDTO.DayAccommodationDTO candidate : day.getAccommodations()) {
            if (!Boolean.TRUE.equals(candidate.getIsAlternative())) {
                primary = calculateAccommodationCost(day.getDayNumber(), candidate, dayDate, totalPax);
                break;
            }
        }

        String sleepoverPark = sleepoverParkOf(day);

        for (FullSafariDTO.DayAccommodationDTO accommodation : day.getAccommodations()) {
            if (!Boolean.TRUE.equals(accommodation.getIsAlternative())) {
                continue;
            }

            CostLineItemDTO line = calculateAccommodationCost(
                day.getDayNumber(), accommodation, dayDate, totalPax);
            if (line == null) {
                continue;
            }

            line.setExclusionReason(ExclusionReason.ALTERNATIVE_ACCOMMODATION);
            line.setEntryId(accommodation.getId());

            /* Only where both sides are priced in the same money: a delta across currencies is a lie. */
            if (primary != null && primary.getCurrency() != null
                && primary.getCurrency().equals(line.getCurrency())) {
                line.setDeltaVsPrimarySto(
                    nullSafe(line.getStoTotalPrice()).subtract(nullSafe(primary.getStoTotalPrice())));
                line.setDeltaVsPrimaryRack(
                    nullSafe(line.getRackTotalPrice()).subtract(nullSafe(primary.getRackTotalPrice())));
            }

            line.setSleepsElsewhere(sleepsElsewhere(accommodation, sleepoverPark));
            excluded.add(line);
        }

        return excluded;
    }

    /**
     * The park this day sleeps in, or null when it sleeps outside every park.
     *
     * Read off the park visit marked SLEEP_OVER, since that is where the day's concession fee
     * comes from. A day with no sleepover park cannot have a fee that depends on where anybody
     * sleeps, so there is nothing to warn about.
     */
    private String sleepoverParkOf(FullSafariDTO.DayDTO day) {
        if (day.getParks() == null) {
            return null;
        }
        for (FullSafariDTO.DayParkDTO visit : day.getParks()) {
            if (visit.getEntryType() == ParkEntryType.SLEEP_OVER) {
                return visit.getParkName();
            }
        }
        return null;
    }

    /**
     * Whether taking this bed would leave the day's fees describing somewhere nobody is sleeping.
     *
     * Compared on the park's name against the lodge's region and district, which is coarse and
     * deliberately so: this is a prompt to go and look, not an assertion. A false warning costs
     * somebody ten seconds; a missing one costs a concession fee per person per night on a safari
     * that is already running.
     */
    private Boolean sleepsElsewhere(
            FullSafariDTO.DayAccommodationDTO accommodation,
            String sleepoverParkName
    ) {
        if (sleepoverParkName == null || sleepoverParkName.isBlank()) {
            return null;
        }
        String park = flatten(sleepoverParkName);
        String region = flatten(accommodation.getAccommodationRegion());
        String district = flatten(accommodation.getAccommodationDistrict());
        if (region.isEmpty() && district.isEmpty()) {
            return null;
        }
        boolean inside = (!district.isEmpty() && park.contains(district))
            || (!region.isEmpty() && park.contains(region));
        return !inside;
    }

    private static String flatten(String value) {
        return value == null ? "" : value.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
