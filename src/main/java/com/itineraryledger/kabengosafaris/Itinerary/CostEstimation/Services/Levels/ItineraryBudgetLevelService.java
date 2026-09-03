package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Levels;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationCategory;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.BudgetLevelComparisonDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CostLineItemDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CurrencyGroupedCostDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.DayCostDetailDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.BudgetLevel;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CostItemType;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.ExclusionReason;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Aggregators.PerDayCostAggregator;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What this trip would cost at each of the three levels.
 *
 * <p>Nothing is priced here. Every figure comes from the estimate the Cost tab already shows: the
 * night's booked bed is an ACCOMMODATION line, and every candidate for that night is an excluded
 * line tagged ALTERNATIVE_ACCOMMODATION. Reading the same numbers is the point -- a level column
 * that priced beds its own way could disagree with what adopting it produces, and the office would
 * have no way to tell which of the two was lying.
 *
 * <p>The level is decided by MONEY. A lodge's category is a label somebody typed, it can be wrong,
 * and a hotel can be dear while calling itself mid-range, so each night's candidates are ranked by
 * what that night costs and the cheapest, middle and dearest become Lowest, Medium and High. The
 * category audits the result instead of choosing it.
 *
 * <p>Because the rest of a day does not move when the bed does -- park fees hang off the park visit
 * and activities off the day -- a night's difference IS the trip's difference, so a level's total is
 * the current total plus each night's swap. No second walk of the tree, and no chance of a total
 * that disagrees with its own nights.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ItineraryBudgetLevelService {

    private final PerDayCostAggregator perDayCostAggregator;

    /**
     * Build the three columns.
     *
     * @param itinerary  the tree, already fetched by the caller
     * @param startDate  the date the estimate is priced against, since rates are seasonal
     */
    public BudgetLevelComparisonDTO compare(FullItineraryDTO itinerary, LocalDate startDate) {
        List<DayCostDetailDTO> days = perDayCostAggregator.aggregateByDay(itinerary, startDate);
        List<CurrencyGroupedCostDTO> current = perDayCostAggregator.calculateGrandTotals(days);

        Map<String, FullItineraryDTO.DayAccommodationDTO> rows = rowsByEntryId(itinerary);

        List<NightCandidates> nights = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (DayCostDetailDTO day : days) {
            NightCandidates night = candidatesFor(day, rows);
            if (night == null) {
                continue;
            }
            if (night.mixedCurrency) {
                warnings.add(String.format(
                    "Day %d has options in more than one currency, so it was not ranked and every "
                        + "level keeps the bed that is booked now",
                    day.getDayNumber()));
            }
            nights.add(night);
        }

        int withAChoice = (int) nights.stream().filter(n -> n.options.size() > 1).count();

        List<BudgetLevelComparisonDTO.LevelDTO> levels = new ArrayList<>();
        for (BudgetLevel level : BudgetLevel.values()) {
            levels.add(buildLevel(level, nights, current));
        }

        return BudgetLevelComparisonDTO.builder()
            .itineraryId(itinerary.getId())
            .itineraryName(itinerary.getName())
            .startDate(startDate)
            .currentBudgetCategory(itinerary.getBudgetCategory() == null ? null
                : itinerary.getBudgetCategory().name())
            .currentTotalsByCurrency(current)
            .overnightNights(nights.size())
            .nightsWithAChoice(withAChoice)
            .levels(levels)
            .warnings(warnings)
            .build();
    }

    /** The rows a level would promote, for the caller that actually writes. */
    public List<BudgetLevelComparisonDTO.NightPickDTO> picksFor(
        BudgetLevelComparisonDTO comparison,
        BudgetLevel level
    ) {
        return comparison.getLevels().stream()
            .filter(l -> l.getLevel() == level)
            .findFirst()
            .map(BudgetLevelComparisonDTO.LevelDTO::getNights)
            .orElse(List.of());
    }

    // ── one night's candidates ──────────────────────────────────────────────────────────────────

    /**
     * Every bed recorded for a night, priced, cheapest first.
     *
     * <p>Ranked on the night's TOTAL for the party rather than a unit price, because some rates are
     * quoted per room and some per person sharing: on a two-guest night a per-room 340 and a
     * per-person 225 are 340 and 450, and ranking their unit prices puts them in the wrong order.
     */
    private NightCandidates candidatesFor(
        DayCostDetailDTO day,
        Map<String, FullItineraryDTO.DayAccommodationDTO> rows
    ) {
        List<CostLineItemDTO> beds = new ArrayList<>();
        CostLineItemDTO primary = null;

        for (CostLineItemDTO line : orEmpty(day.getLineItems())) {
            if (line.getItemType() == CostItemType.ACCOMMODATION && line.getEntryId() != null) {
                beds.add(line);
                if (primary == null) {
                    primary = line;
                }
            }
        }
        for (CostLineItemDTO line : orEmpty(day.getExcludedLineItems())) {
            if (line.getExclusionReason() == ExclusionReason.ALTERNATIVE_ACCOMMODATION
                && line.getEntryId() != null) {
                beds.add(line);
            }
        }

        if (beds.isEmpty()) {
            return null;
        }

        NightCandidates night = new NightCandidates();
        night.day = day;
        night.primary = primary;
        night.rows = rows;

        boolean mixed = beds.stream()
            .map(CostLineItemDTO::getCurrency)
            .distinct()
            .count() > 1;

        if (mixed || primary == null) {
            /*
             * Not ranked. Subtracting one currency from another produces a number and it is a lie,
             * and a night whose booked bed is not priced at all has nothing to compare against, so
             * both keep what is booked rather than guessing.
             */
            night.mixedCurrency = mixed;
            night.options = primary != null ? List.of(primary) : List.of(beds.get(0));
            return night;
        }

        beds.sort(Comparator.comparing(line -> nullSafe(line.getStoTotalPrice())));
        night.options = beds;
        return night;
    }

    /**
     * Which of the night's options this level takes.
     *
     * <p>Lowest is the cheapest and High the dearest. Medium is the lower of the two middles, which
     * is the same as "nearest the median, ties to the cheaper", and it keeps
     * Lowest <= Medium <= High true for every night -- and therefore for the trip, since the totals
     * are sums of the nights.
     */
    private CostLineItemDTO pick(NightCandidates night, BudgetLevel level) {
        List<CostLineItemDTO> options = night.options;
        return switch (level) {
            case LOWEST -> options.get(0);
            case HIGH -> options.get(options.size() - 1);
            case MEDIUM -> options.get((options.size() - 1) / 2);
        };
    }

    // ── one column ──────────────────────────────────────────────────────────────────────────────

    private BudgetLevelComparisonDTO.LevelDTO buildLevel(
        BudgetLevel level,
        List<NightCandidates> nights,
        List<CurrencyGroupedCostDTO> current
    ) {
        List<BudgetLevelComparisonDTO.NightPickDTO> picks = new ArrayList<>();
        Map<String, BigDecimal> deltaSto = new LinkedHashMap<>();
        Map<String, BigDecimal> deltaRack = new LinkedHashMap<>();
        Map<String, Integer> mix = new LinkedHashMap<>();

        int changed = 0;
        int already = 0;
        int noChoice = 0;

        for (NightCandidates night : nights) {
            CostLineItemDTO chosen = pick(night, level);
            CostLineItemDTO primary = night.primary != null ? night.primary : chosen;
            boolean isPrimary = chosen.getEntryId() != null
                && chosen.getEntryId().equals(primary.getEntryId());

            if (night.options.size() == 1) {
                noChoice++;
            }
            if (isPrimary) {
                already++;
            } else {
                changed++;
            }

            FullItineraryDTO.DayAccommodationDTO row = night.rows.get(chosen.getEntryId());
            String category = row != null ? row.getAccommodationCategory() : null;
            if (category != null) {
                mix.merge(category, 1, Integer::sum);
            }

            BigDecimal dSto = nullSafe(chosen.getStoTotalPrice())
                .subtract(nullSafe(primary.getStoTotalPrice()));
            BigDecimal dRack = nullSafe(chosen.getRackTotalPrice())
                .subtract(nullSafe(primary.getRackTotalPrice()));

            String currency = chosen.getCurrency();
            if (currency != null && !night.mixedCurrency) {
                deltaSto.merge(currency, dSto, BigDecimal::add);
                deltaRack.merge(currency, dRack, BigDecimal::add);
            }

            picks.add(BudgetLevelComparisonDTO.NightPickDTO.builder()
                .dayId(night.day.getDayId())
                .dayNumber(night.day.getDayNumber())
                .date(night.day.getDate())
                .dayTitle(night.day.getDayTitle())
                .entryId(chosen.getEntryId())
                .accommodationName(chosen.getItemName())
                .roomTypeName(row != null ? row.getRoomTypeName() : null)
                .category(category)
                .sto(chosen.getStoTotalPrice())
                .rack(chosen.getRackTotalPrice())
                .currency(currency)
                .deltaSto(night.mixedCurrency ? null : dSto)
                .deltaRack(night.mixedCurrency ? null : dRack)
                .isCurrentPrimary(isPrimary)
                .optionsOnThisNight(night.options.size())
                .note(noteFor(level, night, chosen, category))
                .build());
        }

        return BudgetLevelComparisonDTO.LevelDTO.builder()
            .level(level)
            .label(level.getLabel())
            .description(level.getDescription())
            .totalsByCurrency(applyDeltas(current, deltaSto, deltaRack))
            .deltaSto(deltaSto)
            .deltaRack(deltaRack)
            .badge(badgeFrom(mix))
            .categoryMix(mix)
            .nightsChanged(changed)
            .nightsAlreadyThere(already)
            .nightsWithoutAChoice(noChoice)
            .nights(picks)
            .build();
    }

    /**
     * What is odd about a pick.
     *
     * <p>The price chose it, so the label is free to disagree, and a disagreement is worth saying
     * out loud: it is either a lodge somebody mislabelled or a rate worth a second look. And where
     * the dearest bed recorded for a night is not a dear bed, "High range" has to admit that rather
     * than imply a luxury option exists.
     */
    private String noteFor(
        BudgetLevel level,
        NightCandidates night,
        CostLineItemDTO chosen,
        String category
    ) {
        if (night.mixedCurrency) {
            return "Not ranked: this night has options priced in more than one currency, so the "
                + "bed that is booked now is kept";
        }
        if (night.options.size() == 1) {
            return "The only bed recorded for this night, so all three levels are the same here";
        }
        if (category == null) {
            return null;
        }

        AccommodationCategory parsed;
        try {
            parsed = AccommodationCategory.valueOf(category);
        } catch (IllegalArgumentException e) {
            return null;
        }
        int rank = BudgetLevel.rankOf(parsed);

        if (level == BudgetLevel.HIGH && rank <= BudgetLevel.rankOf(AccommodationCategory.MID_RANGE)) {
            return "The dearest bed recorded for this night is only " + category.toLowerCase()
                + ". Record a better option if the client wants one";
        }
        if (level == BudgetLevel.LOWEST && rank >= BudgetLevel.rankOf(AccommodationCategory.LUXURY)) {
            return "Cheapest on this night, though it is labelled " + category.toLowerCase()
                + ". Either the label is wrong or this is a good rate";
        }
        return null;
    }

    /** The current totals with each night's swap applied. Accommodation moves; nothing else does. */
    private List<CurrencyGroupedCostDTO> applyDeltas(
        List<CurrencyGroupedCostDTO> current,
        Map<String, BigDecimal> deltaSto,
        Map<String, BigDecimal> deltaRack
    ) {
        List<CurrencyGroupedCostDTO> out = new ArrayList<>();
        for (CurrencyGroupedCostDTO totals : orEmpty(current)) {
            BigDecimal dSto = deltaSto.getOrDefault(totals.getCurrency(), BigDecimal.ZERO);
            BigDecimal dRack = deltaRack.getOrDefault(totals.getCurrency(), BigDecimal.ZERO);
            out.add(CurrencyGroupedCostDTO.builder()
                .currency(totals.getCurrency())
                .accommodationSto(nullSafe(totals.getAccommodationSto()).add(dSto))
                .accommodationRack(nullSafe(totals.getAccommodationRack()).add(dRack))
                .parkFeesSto(totals.getParkFeesSto())
                .parkFeesRack(totals.getParkFeesRack())
                .activitiesSto(totals.getActivitiesSto())
                .activitiesRack(totals.getActivitiesRack())
                .grandTotalSto(nullSafe(totals.getGrandTotalSto()).add(dSto))
                .grandTotalRack(nullSafe(totals.getGrandTotalRack()).add(dRack))
                .build());
        }
        return out;
    }

    /** The commonest category among the beds this level takes, the dearer one on a tie. */
    private String badgeFrom(Map<String, Integer> mix) {
        String best = null;
        int bestCount = -1;
        int bestRank = -1;
        for (Map.Entry<String, Integer> entry : mix.entrySet()) {
            int rank;
            try {
                rank = BudgetLevel.rankOf(AccommodationCategory.valueOf(entry.getKey()));
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (entry.getValue() > bestCount || (entry.getValue() == bestCount && rank > bestRank)) {
                best = entry.getKey();
                bestCount = entry.getValue();
                bestRank = rank;
            }
        }
        if (best == null) {
            return null;
        }
        return BudgetLevel.badgeFor(AccommodationCategory.valueOf(best)).name();
    }

    private Map<String, FullItineraryDTO.DayAccommodationDTO> rowsByEntryId(FullItineraryDTO itinerary) {
        Map<String, FullItineraryDTO.DayAccommodationDTO> rows = new LinkedHashMap<>();
        for (FullItineraryDTO.DayDTO day : orEmpty(itinerary.getDays())) {
            for (FullItineraryDTO.DayAccommodationDTO stay : orEmpty(day.getAccommodations())) {
                if (stay.getId() != null) {
                    rows.put(stay.getId(), stay);
                }
            }
        }
        return rows;
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** One night's beds, cheapest first, with the one that is booked now. */
    private static class NightCandidates {
        DayCostDetailDTO day;
        CostLineItemDTO primary;
        List<CostLineItemDTO> options = List.of();
        boolean mixedCurrency;
        Map<String, FullItineraryDTO.DayAccommodationDTO> rows = Map.of();
    }
}
