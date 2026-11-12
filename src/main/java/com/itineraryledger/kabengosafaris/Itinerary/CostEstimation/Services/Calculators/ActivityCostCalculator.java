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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

                /*
                 * The guard used to read `a != null && b > 0 || c > 0`, which binds as
                 * `(a && b) || c` — so a null line item fell through to dereference c.
                 */
                for (CostLineItemDTO lineItem : calculateStandaloneActivityCost(
                    day.getDayNumber(),
                    activity,
                    season,
                    paxList,
                    carCount
                )) {
                    if (carriesAnyCharge(lineItem)) {
                        items.add(lineItem);
                    }
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

                    for (CostLineItemDTO lineItem : calculateParkActivityCost(
                        day.getDayNumber(),
                        parkActivity,
                        parkId,
                        park.getParkName(),
                        season,
                        paxList,
                        carCount
                    )) {
                        if (carriesAnyCharge(lineItem)) {
                            items.add(lineItem);
                        }
                    }
                }
            }
        }

        return items;
    }

    /**
     * Is there any money on this line at all?
     *
     * <p>A zero line is an activity we do not charge for, and listing it among the costs reads as
     * a price of nothing rather than as no price.
     */
    private boolean carriesAnyCharge(CostLineItemDTO lineItem) {
        if (lineItem == null) {
            return false;
        }
        BigDecimal sto = lineItem.getStoTotalPrice();
        BigDecimal rack = lineItem.getRackTotalPrice();
        return (sto != null && sto.compareTo(BigDecimal.ZERO) > 0)
            || (rack != null && rack.compareTo(BigDecimal.ZERO) > 0);
    }

    /**
     * Calculate cost for a standalone activity.
     */
    private List<CostLineItemDTO> calculateStandaloneActivityCost(
            Integer dayNumber,
            FullItineraryDTO.DayActivityDTO activity,
            Season season,
            List<FullItineraryDTO.PaxDTO> paxList,
            int carCount
    ) {
        Long activityId = idObfuscator.decodeId(activity.getActivityId());
        if (activityId == null) {
            return List.of();
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
    private List<CostLineItemDTO> calculateParkActivityCost(
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
            return List.of();
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
     *
     * <p>Returns a list because a per-person activity can be charged in more than one currency at
     * once, when the party mixes nationalities. See {@link CurrencyBucket}.
     */
    private List<CostLineItemDTO> calculateActivityCost(
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
            return List.of(buildNotFoundLineItem(dayNumber, activityName, parkName));
        }

        Activity activity = activityOpt.get();

        // Skip if activity has no tariff (included/free activity)
        if (activity.getHasTariff() == null || !activity.getHasTariff()) {
            return List.of(buildIncludedLineItem(dayNumber, activityName, parkName));
        }

        ChargingBasis chargingBasis = activity.getChargingBasis() != null
            ? activity.getChargingBasis()
            : ChargingBasis.PER_PERSON;

        if (chargingBasis == ChargingBasis.PER_PERSON) {
            return calculatePerPersonActivityCost(
                dayNumber, activityId, activityName, parkId, parkName, season, paxList
            );
        } else if (chargingBasis == ChargingBasis.PER_VEHICLE) {
            /* One vehicle rate, one currency: nothing to split. */
            return List.of(calculateGroupActivityCost(
                dayNumber, activityId, activityName, parkId, parkName, season, paxList, carCount, chargingBasis
            ));
        } else {
            // PER_GROUP, FLAT_RATE, etc.
            return List.of(calculateGroupActivityCost(
                dayNumber, activityId, activityName, parkId, parkName, season, paxList, 1, chargingBasis
            ));
        }
    }

    /**
     * Calculate per-person activity cost, one line per currency the bands are charged in.
     *
     * <p>Activity rates mix currencies even within one nation band — the rate card holds 238
     * East African rows in shillings and 57 in dollars, because some suppliers quote residents one
     * way and some the other. Both are legitimate, so the engine reads whatever each row says
     * rather than assuming a house currency. See {@link CurrencyBucket}.
     */
    private List<CostLineItemDTO> calculatePerPersonActivityCost(
            Integer dayNumber,
            Long activityId,
            String activityName,
            Long parkId,
            String parkName,
            Season season,
            List<FullItineraryDTO.PaxDTO> paxList
    ) {
        if (paxList == null || paxList.isEmpty()) {
            return List.of(buildNotFoundLineItem(dayNumber, activityName, parkName));
        }

        /*
         * One bucket per currency. The shares inside each are kept alongside its sum so per-pax
         * mode can attribute instead of averaging.
         */
        Map<String, CurrencyBucket> byCurrency = CurrencyBucket.ledger();

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

                String currency = rate.getCurrency() != null ? rate.getCurrency() : DEFAULT_CURRENCY;

                byCurrency.computeIfAbsent(currency, c -> new CurrencyBucket())
                    .add(shareSto, shareRack, count, CostLineItemDTO.PaxShareDTO.builder()
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

        if (byCurrency.isEmpty()) {
            return List.of(buildNotFoundLineItem(dayNumber, activityName, parkName));
        }

        String itemName = activityName + (parkName != null ? " @ " + parkName : "");

        List<CostLineItemDTO> items = new ArrayList<>();
        for (Map.Entry<String, CurrencyBucket> entry : byCurrency.entrySet()) {
            CurrencyBucket bucket = entry.getValue();
            items.add(CostLineItemDTO.builder()
                .dayNumber(dayNumber)
                .itemType(CostItemType.ACTIVITY)
                .itemName(itemName)
                .itemId(idObfuscator.encodeId(activityId))
                .chargingBasis(ChargingBasis.PER_PERSON.getDisplayName())
                .quantity(bucket.paxCount())
                .stoUnitPrice(bucket.averageSto())
                .rackUnitPrice(bucket.averageRack())
                .stoTotalPrice(bucket.sto())
                .rackTotalPrice(bucket.rack())
                .currency(entry.getKey())
                .paxCategory("Per Person")
                .paxShares(bucket.shares())
                .build());
        }
        return items;
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

                for (CostLineItemDTO line : calculateStandaloneActivityCost(
                    day.getDayNumber(), activity, season, paxList, carCount)) {
                    /* Optional wins the label: it is the one a client can say yes to. */
                    line.setExclusionReason(optional
                        ? ExclusionReason.OPTIONAL_ACTIVITY
                        : ExclusionReason.NOT_INCLUDED_IN_PRICE);
                    line.setEntryId(activity.getId());
                    excluded.add(line);
                }
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
                    for (CostLineItemDTO line : calculateParkActivityCost(
                        day.getDayNumber(), parkActivity, parkId, park.getParkName(),
                        season, paxList, carCount)) {
                        /*
                         * A park activity has no optional flag of its own, so "not included" is
                         * all this side can say. In practice it is used the same way: the Maasai
                         * boma visit on a Ngorongoro transit is recorded exactly like this.
                         */
                        line.setExclusionReason(ExclusionReason.NOT_INCLUDED_IN_PRICE);
                        line.setEntryId(parkActivity.getId());
                        line.setParentEntryId(park.getId());
                        excluded.add(line);
                    }
                }
            }
        }

        return excluded;
    }
}
