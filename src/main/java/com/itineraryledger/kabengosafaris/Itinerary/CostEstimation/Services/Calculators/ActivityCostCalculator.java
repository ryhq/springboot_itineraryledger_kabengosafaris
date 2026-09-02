package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Calculators;

import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.ActivityTariffRate;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CostLineItemDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CostItemType;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core.ActivityRateLookupService;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.ExclusionReason;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO;
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
 * Calculator for activity costs.
 *
 * Handles both:
 * - Standalone activities (ItineraryDayActivity - not tied to a park)
 * - Park activities (ItineraryDayParkActivity - within a park visit)
 *
 * - Supports PER_PERSON, PER_VEHICLE, PER_GROUP, FLAT_RATE charging bases
 * - Park-specific rates take precedence over global rates
 * - Returns both STO and Rack rates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityCostCalculator {

    private final ActivityRateLookupService activityRateLookupService;
    private final ActivityRepository activityRepository;
    private final PaxNationCategoryRepository nationCategoryRepository;
    private final IdObfuscator idObfuscator;

    private static final String DEFAULT_CURRENCY = "USD";

    /**
     * Calculate activity costs for a day.
     *
     * @param day The day DTO
     * @param dayDate The actual date
     * @param season The applicable season
     * @param paxList Passenger list
     * @param carCount Number of vehicles
     * @return List of cost line items for activities
     */
    public List<CostLineItemDTO> calculateForDay(
            FullItineraryDTO.DayDTO day,
            LocalDate dayDate,
            Season season,
            List<FullItineraryDTO.PaxDTO> paxList,
            int carCount
    ) {
        List<CostLineItemDTO> items = new ArrayList<>();

        // 1. Process standalone day activities
        if (day.getActivities() != null) {
            for (FullItineraryDTO.DayActivityDTO activity : day.getActivities()) {
                // Skip if not included in price or is optional
                if ((activity.getIsIncludedInPrice() != null && !activity.getIsIncludedInPrice())
                    || Boolean.TRUE.equals(activity.getIsOptional())) {
                    continue;
                }

                CostLineItemDTO lineItem = calculateStandaloneActivityCost(
                    day.getDayNumber(),
                    activity,
                    season,
                    paxList,
                    carCount
                );

                if (lineItem != null && lineItem.getStoTotalPrice().compareTo(BigDecimal.ZERO) > 0
                    || lineItem.getRackTotalPrice().compareTo(BigDecimal.ZERO) > 0) {
                    items.add(lineItem);
                }
            }
        }

        // 2. Process park activities
        if (day.getParks() != null) {
            for (FullItineraryDTO.DayParkDTO park : day.getParks()) {
                if (park.getActivities() == null) {
                    continue;
                }

                Long parkId = idObfuscator.decodeId(park.getParkId());

                for (FullItineraryDTO.ParkActivityDTO parkActivity : park.getActivities()) {
                    // Skip if not included in price
                    if (parkActivity.getIsIncludedInPrice() != null && !parkActivity.getIsIncludedInPrice()) {
                        continue;
                    }

                    CostLineItemDTO lineItem = calculateParkActivityCost(
                        day.getDayNumber(),
                        parkActivity,
                        parkId,
                        park.getParkName(),
                        season,
                        paxList,
                        carCount
                    );

                    if (lineItem != null && (lineItem.getStoTotalPrice().compareTo(BigDecimal.ZERO) > 0
                        || lineItem.getRackTotalPrice().compareTo(BigDecimal.ZERO) > 0)) {
                        items.add(lineItem);
                    }
                }
            }
        }

        return items;
    }

    /**
     * Calculate cost for a standalone activity.
     */
    private CostLineItemDTO calculateStandaloneActivityCost(
            Integer dayNumber,
            FullItineraryDTO.DayActivityDTO activity,
            Season season,
            List<FullItineraryDTO.PaxDTO> paxList,
            int carCount
    ) {
        Long activityId = idObfuscator.decodeId(activity.getActivityId());
        if (activityId == null) {
            return null;
        }

        return calculateActivityCost(
            dayNumber,
            activityId,
            activity.getActivityName(),
            null, // No park for standalone activities
            null,
            season,
            paxList,
            carCount
        );
    }

    /**
     * Calculate cost for a park activity.
     */
    private CostLineItemDTO calculateParkActivityCost(
            Integer dayNumber,
            FullItineraryDTO.ParkActivityDTO parkActivity,
            Long parkId,
            String parkName,
            Season season,
            List<FullItineraryDTO.PaxDTO> paxList,
            int carCount
    ) {
        Long activityId = idObfuscator.decodeId(parkActivity.getActivityId());
        if (activityId == null) {
            return null;
        }

        return calculateActivityCost(
            dayNumber,
            activityId,
            parkActivity.getActivityName(),
            parkId,
            parkName,
            season,
            paxList,
            carCount
        );
    }

    /**
     * Calculate cost for an activity (common logic for standalone and park activities).
     */
    private CostLineItemDTO calculateActivityCost(
            Integer dayNumber,
            Long activityId,
            String activityName,
            Long parkId,
            String parkName,
            Season season,
            List<FullItineraryDTO.PaxDTO> paxList,
            int carCount
    ) {
        // Get the activity to check if it has a tariff
        Optional<Activity> activityOpt = activityRepository.findById(activityId);
        if (activityOpt.isEmpty()) {
            return buildNotFoundLineItem(dayNumber, activityName, parkName);
        }

        Activity activity = activityOpt.get();

        // Skip if activity has no tariff (included/free activity)
        if (activity.getHasTariff() == null || !activity.getHasTariff()) {
            return buildIncludedLineItem(dayNumber, activityName, parkName);
        }

        ChargingBasis chargingBasis = activity.getChargingBasis() != null
            ? activity.getChargingBasis()
            : ChargingBasis.PER_PERSON;

        if (chargingBasis == ChargingBasis.PER_PERSON) {
            return calculatePerPersonActivityCost(
                dayNumber, activityId, activityName, parkId, parkName, season, paxList
            );
        } else if (chargingBasis == ChargingBasis.PER_VEHICLE) {
            return calculateGroupActivityCost(
                dayNumber, activityId, activityName, parkId, parkName, season, paxList, carCount, chargingBasis
            );
        } else {
            // PER_GROUP, FLAT_RATE, etc.
            return calculateGroupActivityCost(
                dayNumber, activityId, activityName, parkId, parkName, season, paxList, 1, chargingBasis
            );
        }
    }

    /**
     * Calculate per-person activity cost.
     */
    private CostLineItemDTO calculatePerPersonActivityCost(
            Integer dayNumber,
            Long activityId,
            String activityName,
            Long parkId,
            String parkName,
            Season season,
            List<FullItineraryDTO.PaxDTO> paxList
    ) {
        if (paxList == null || paxList.isEmpty()) {
            return buildNotFoundLineItem(dayNumber, activityName, parkName);
        }

        BigDecimal stoTotal = BigDecimal.ZERO;
        BigDecimal rackTotal = BigDecimal.ZERO;
        String currency = DEFAULT_CURRENCY;
        int totalPaxCount = 0;
        boolean anyRateFound = false;
        // kept alongside the sum so per-pax mode can attribute instead of averaging
        List<CostLineItemDTO.PaxShareDTO> paxShares = new ArrayList<>();

        for (FullItineraryDTO.PaxDTO pax : paxList) {
            Long nationCategoryId = idObfuscator.decodeId(pax.getNationCategoryId());
            Long ageCategoryId = idObfuscator.decodeId(pax.getAgeCategoryId());
            int count = pax.getCount() != null ? pax.getCount() : 0;

            if (nationCategoryId == null || ageCategoryId == null || count == 0) {
                continue;
            }

            String paxCategory = pax.getNationCategoryName() + " " + pax.getAgeCategoryName();

            ActivityRateLookupService.LookupResult result = activityRateLookupService
                .lookupPersonRateWithIssueLogging(
                    activityId, activityName, parkId, parkName, season,
                    nationCategoryId, ageCategoryId, paxCategory, dayNumber
                );

            if (result.found()) {
                ActivityTariffRate rate = result.rate();
                BigDecimal stoUnit = rate.getStoRate() != null ? rate.getStoRate() : BigDecimal.ZERO;
                BigDecimal rackUnit = rate.getRackRate() != null ? rate.getRackRate() : BigDecimal.ZERO;

                BigDecimal shareSto = stoUnit.multiply(BigDecimal.valueOf(count));
                BigDecimal shareRack = rackUnit.multiply(BigDecimal.valueOf(count));

                stoTotal = stoTotal.add(shareSto);
                rackTotal = rackTotal.add(shareRack);
                totalPaxCount += count;
                currency = rate.getCurrency() != null ? rate.getCurrency() : DEFAULT_CURRENCY;
                anyRateFound = true;

                paxShares.add(CostLineItemDTO.PaxShareDTO.builder()
                    .nationCategoryId(pax.getNationCategoryId())
                    .ageCategoryId(pax.getAgeCategoryId())
                    .paxCategory(paxCategory)
                    .count(count)
                    .stoUnitPrice(stoUnit)
                    .rackUnitPrice(rackUnit)
                    .stoTotalPrice(shareSto)
                    .rackTotalPrice(shareRack)
                    .build());
            }
        }

        if (!anyRateFound) {
            return buildNotFoundLineItem(dayNumber, activityName, parkName);
        }

        // Calculate average unit price for display
        BigDecimal stoUnitPrice = totalPaxCount > 0
            ? stoTotal.divide(BigDecimal.valueOf(totalPaxCount), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        BigDecimal rackUnitPrice = totalPaxCount > 0
            ? rackTotal.divide(BigDecimal.valueOf(totalPaxCount), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        String itemName = activityName + (parkName != null ? " @ " + parkName : "");

        return CostLineItemDTO.builder()
            .dayNumber(dayNumber)
            .itemType(CostItemType.ACTIVITY)
            .itemName(itemName)
            .itemId(idObfuscator.encodeId(activityId))
            .chargingBasis(ChargingBasis.PER_PERSON.getDisplayName())
            .quantity(totalPaxCount)
            .stoUnitPrice(stoUnitPrice)
            .rackUnitPrice(rackUnitPrice)
            .stoTotalPrice(stoTotal)
            .rackTotalPrice(rackTotal)
            .currency(currency)
            .paxCategory("Per Person")
            .paxShares(paxShares)
            .build();
    }

    /**
     * Calculate group/vehicle activity cost.
     */
    private CostLineItemDTO calculateGroupActivityCost(
            Integer dayNumber,
            Long activityId,
            String activityName,
            Long parkId,
            String parkName,
            Season season,
            List<FullItineraryDTO.PaxDTO> paxList,
            int quantity,
            ChargingBasis chargingBasis
    ) {
        // Find highest priority nation category
        Long highestPriorityNationId = findHighestPriorityNationCategory(paxList);

        if (highestPriorityNationId == null) {
            return buildNotFoundLineItem(dayNumber, activityName, parkName);
        }

        String chargingDesc = chargingBasis.getDisplayName();

        ActivityRateLookupService.LookupResult result = activityRateLookupService
            .lookupGroupRateWithIssueLogging(
                activityId, activityName, parkId, parkName, season,
                highestPriorityNationId, chargingDesc, dayNumber
            );

        if (!result.found()) {
            return buildNotFoundLineItem(dayNumber, activityName, parkName);
        }

        ActivityTariffRate rate = result.rate();
        BigDecimal stoUnitPrice = rate.getStoRate() != null ? rate.getStoRate() : BigDecimal.ZERO;
        BigDecimal rackUnitPrice = rate.getRackRate() != null ? rate.getRackRate() : BigDecimal.ZERO;

        BigDecimal stoTotal = stoUnitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal rackTotal = rackUnitPrice.multiply(BigDecimal.valueOf(quantity));

        String currency = rate.getCurrency() != null ? rate.getCurrency() : DEFAULT_CURRENCY;
        String notes = quantity > 1 ? String.format("%s x %d", chargingDesc, quantity) : chargingDesc;
        String itemName = activityName + (parkName != null ? " @ " + parkName : "");

        return CostLineItemDTO.builder()
            .dayNumber(dayNumber)
            .itemType(CostItemType.ACTIVITY)
            .itemName(itemName)
            .itemId(idObfuscator.encodeId(activityId))
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
     * Build a line item for an included (free) activity.
     */
    private CostLineItemDTO buildIncludedLineItem(
            Integer dayNumber,
            String activityName,
            String parkName
    ) {
        String itemName = activityName + (parkName != null ? " @ " + parkName : "");

        return CostLineItemDTO.builder()
            .dayNumber(dayNumber)
            .itemType(CostItemType.ACTIVITY)
            .itemName(itemName)
            .chargingBasis("Included")
            .quantity(1)
            .stoUnitPrice(BigDecimal.ZERO)
            .rackUnitPrice(BigDecimal.ZERO)
            .stoTotalPrice(BigDecimal.ZERO)
            .rackTotalPrice(BigDecimal.ZERO)
            .currency(DEFAULT_CURRENCY)
            .notes("Included in package")
            .build();
    }

    /**
     * Build a line item for an activity where the rate was not found.
     */
    private CostLineItemDTO buildNotFoundLineItem(
            Integer dayNumber,
            String activityName,
            String parkName
    ) {
        String itemName = activityName + (parkName != null ? " @ " + parkName : "");

        return CostLineItemDTO.builder()
            .dayNumber(dayNumber)
            .itemType(CostItemType.ACTIVITY)
            .itemName(itemName)
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
     * The activities this day offers but does not charge for.
     *
     * Two different things end up here and the reason says which. An OPTIONAL activity is one the
     * client may add, like a Maasai boma visit or a balloon flight; a NOT_INCLUDED one is a line
     * somebody deliberately switched off. Both were dropped silently before this, so an itinerary
     * could carry a priced upsell that no cost screen would ever show, and the office would quote
     * it from memory or not at all.
     *
     * A sibling of calculateForDay, so nothing here can reach a total: the aggregator puts this
     * list in its own field.
     */
    public List<CostLineItemDTO> calculateExcludedForDay(
        FullItineraryDTO.DayDTO day,
        LocalDate dayDate,
        Season season,
        List<FullItineraryDTO.PaxDTO> paxList,
        int carCount
    ) {
        List<CostLineItemDTO> excluded = new ArrayList<>();

        if (day.getActivities() != null) {
            for (FullItineraryDTO.DayActivityDTO activity : day.getActivities()) {
                boolean notIncluded = activity.getIsIncludedInPrice() != null
                    && !activity.getIsIncludedInPrice();
                boolean optional = Boolean.TRUE.equals(activity.getIsOptional());
                if (!notIncluded && !optional) {
                    continue;
                }

                CostLineItemDTO line = calculateStandaloneActivityCost(
                    day.getDayNumber(), activity, season, paxList, carCount);
                if (line == null) {
                    continue;
                }
                /* Optional wins the label: it is the one a client can say yes to. */
                line.setExclusionReason(optional
                    ? ExclusionReason.OPTIONAL_ACTIVITY
                    : ExclusionReason.NOT_INCLUDED_IN_PRICE);
                line.setEntryId(activity.getId());
                excluded.add(line);
            }
        }

        if (day.getParks() != null) {
            for (FullItineraryDTO.DayParkDTO park : day.getParks()) {
                if (park.getActivities() == null) {
                    continue;
                }
                Long parkId = idObfuscator.decodeId(park.getParkId());
                for (FullItineraryDTO.ParkActivityDTO parkActivity : park.getActivities()) {
                    if (parkActivity.getIsIncludedInPrice() == null
                        || parkActivity.getIsIncludedInPrice()) {
                        continue;
                    }
                    CostLineItemDTO line = calculateParkActivityCost(
                        day.getDayNumber(), parkActivity, parkId, park.getParkName(),
                        season, paxList, carCount);
                    if (line == null) {
                        continue;
                    }
                    /*
                     * A park activity has no optional flag of its own, so "not included" is all
                     * this side can say. In practice it is used the same way: the Maasai boma
                     * visit on a Ngorongoro transit is recorded exactly like this.
                     */
                    line.setExclusionReason(ExclusionReason.NOT_INCLUDED_IN_PRICE);
                    line.setEntryId(parkActivity.getId());
                    excluded.add(line);
                }
            }
        }

        return excluded;
    }
}
