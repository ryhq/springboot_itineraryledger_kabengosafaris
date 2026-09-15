package com.itineraryledger.kabengosafaris.Inclusion.Services;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.GlobalEnums.LineCategoryScope;
import com.itineraryledger.kabengosafaris.Inclusion.DTOs.InclusionWarningDTO;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteItem;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemType;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Entity.QuoteDayActivity;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Entity.QuoteDayPark;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkActivity.Entity.QuoteDayParkActivity;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.Entity.QuoteDayParkTariff;
import com.itineraryledger.kabengosafaris.Quote.QuoteInclusion.Entity.QuoteInclusion;

import lombok.RequiredArgsConstructor;

/**
 * Where a quote's promise and a quote's prices disagree.
 *
 * <p>A warning, never a refusal. A contradiction is frequently deliberate — park fees priced out
 * because the client pays at the gate is a legitimate quote — so this reports and gets out of the
 * way. Blocking would teach people to phrase the promise around the check instead of fixing it.
 *
 * <p><strong>Only lines that make a checkable claim are checked at all.</strong> Eight of the
 * fourteen standard lines claim nothing a machine can verify — no line type stands for
 * "flying-doctors cover" or "drinking water on game drives" — and the shared
 * {@link LineCategoryScope} reads an unset scope as EVERY category, because for tax and discount
 * that is what it means. Read the same way here, "Drinking water on game drives" would accuse every
 * quote ever written of contradicting itself about park fees, insurance and visas at once. So the
 * null check comes first, before {@code covers()} or {@code parse()} is ever called.
 */
@Service
@RequiredArgsConstructor
public class InclusionAccuracyService {

    /** Truth the day tree carries per line, which the item list cannot. */
    private record TreeLine(QuoteItemType type, String label, Integer dayNumber, boolean includedInPrice) {}

    public List<InclusionWarningDTO> check(Quote quote) {
        List<InclusionWarningDTO> warnings = new ArrayList<>();
        if (quote == null) return warnings;

        List<QuoteInclusion> lines = quote.getInclusionList() == null
            ? List.of() : quote.getInclusionList();

        if (lines.isEmpty()) {
            /*
             * The single most likely real defect after this ships, and it costs an isEmpty() to
             * catch: a quote produced before the chain existed, or one whose lines somebody
             * cleared, states a figure and says nothing about what it buys.
             */
            warnings.add(InclusionWarningDTO.builder()
                .severity("MISSING")
                .message("This quote states a price and says nothing about what it covers. "
                    + "Reset it to the itinerary, or set the lines on the Inclusions tab.")
                .build());
            return warnings;
        }

        List<TreeLine> tree = treeLines(quote);
        Set<QuoteItemType> pricedTypes = pricedTypes(quote);

        for (QuoteInclusion line : lines) {
            /* No claim, nothing to check. This is the branch the whole comment above is about. */
            if (line.getClaimAppliesTo() == null || line.getClaimAppliesTo().isBlank()) continue;

            Set<String> claimed = LineCategoryScope.parse(line.getClaimAppliesTo(), QuoteItemType.class);
            if (claimed.isEmpty()) continue;

            List<TreeLine> outside = tree.stream()
                .filter(t -> t.type() != null && claimed.contains(t.type().name()))
                .filter(t -> !t.includedInPrice())
                .toList();

            boolean anyPriced = pricedTypes.stream().anyMatch(t -> claimed.contains(t.name()))
                || tree.stream().anyMatch(t -> t.type() != null
                    && claimed.contains(t.type().name()) && t.includedInPrice());

            if (line.included()) {
                if (!outside.isEmpty()) {
                    warnings.add(contradicted(line, claimed, outside,
                        "This quote promises \"" + line.getLabel() + "\", but "
                        + outside.size() + " line" + (outside.size() == 1 ? " is" : "s are")
                        + " priced outside the total."));
                } else if (!anyPriced) {
                    warnings.add(InclusionWarningDTO.builder()
                        .severity("UNSUPPORTED")
                        .inclusionLabel(line.getLabel())
                        .isIncluded(true)
                        .categories(new ArrayList<>(claimed))
                        .message("This quote promises \"" + line.getLabel()
                            + "\", but there is nothing of that kind on it at all. Either the line "
                            + "does not belong on this trip, or something is missing from the price.")
                        .totalOffending(0)
                        .build());
                }
            } else {
                /*
                 * The other direction, and the one that actually costs money: the document says a
                 * thing is not included and the customer is being charged for it anyway.
                 */
                List<TreeLine> chargedAnyway = tree.stream()
                    .filter(t -> t.type() != null && claimed.contains(t.type().name()))
                    .filter(TreeLine::includedInPrice)
                    .toList();
                boolean chargedAsItem = pricedTypes.stream().anyMatch(t -> claimed.contains(t.name()));

                if (!chargedAnyway.isEmpty() || chargedAsItem) {
                    warnings.add(contradicted(line, claimed, chargedAnyway,
                        "This quote says \"" + line.getLabel()
                        + "\" is not included, and then charges for it."));
                }
            }
        }

        return warnings;
    }

    private InclusionWarningDTO contradicted(
        QuoteInclusion line, Set<String> claimed, List<TreeLine> offending, String message
    ) {
        /* Capped at five: a fourteen-day trip would otherwise return a wall nobody reads. */
        List<InclusionWarningDTO.OffendingLine> shown = offending.stream()
            .limit(5)
            .map(t -> new InclusionWarningDTO.OffendingLine(
                t.dayNumber(), t.label(), t.type() != null ? t.type().name() : null))
            .toList();

        return InclusionWarningDTO.builder()
            .severity("CONTRADICTED")
            .inclusionLabel(line.getLabel())
            .isIncluded(line.included())
            .categories(new ArrayList<>(claimed))
            .message(message)
            .offendingLines(shown)
            .totalOffending(offending.size())
            .build();
    }

    /**
     * The per-line truth, which only the day tree has.
     *
     * <p>Accommodation is absent on purpose: there is no {@code isIncludedInPrice} on a day's
     * accommodation rows, so an ACCOMMODATION or MEALS claim can only ever be checked for
     * existence, never for genuine coverage. Nothing here should imply otherwise.
     */
    private List<TreeLine> treeLines(Quote quote) {
        List<TreeLine> lines = new ArrayList<>();
        if (quote.getDays() == null) return lines;

        for (QuoteDay day : quote.getDays()) {
            if (day.getActivities() != null) {
                for (QuoteDayActivity activity : day.getActivities()) {
                    lines.add(new TreeLine(
                        QuoteItemType.ACTIVITY,
                        activity.getActivity() != null ? activity.getActivity().getName() : "Activity",
                        day.getDayNumber(),
                        !Boolean.FALSE.equals(activity.getIsIncludedInPrice())));
                }
            }
            if (day.getParks() == null) continue;
            for (QuoteDayPark park : day.getParks()) {
                if (park.getParkTariffs() != null) {
                    for (QuoteDayParkTariff tariff : park.getParkTariffs()) {
                        lines.add(new TreeLine(
                            QuoteItemType.PARK_FEE,
                            tariff.getParkTariff() != null && tariff.getParkTariff().getTariff() != null
                                ? tariff.getParkTariff().getTariff().getName() : "Park fee",
                            day.getDayNumber(),
                            !Boolean.FALSE.equals(tariff.getIsIncludedInPrice())));
                    }
                }
                if (park.getParkActivities() != null) {
                    for (QuoteDayParkActivity activity : park.getParkActivities()) {
                        lines.add(new TreeLine(
                            QuoteItemType.ACTIVITY,
                            activity.getParkActivity() != null
                                && activity.getParkActivity().getActivity() != null
                                ? activity.getParkActivity().getActivity().getName() : "Park activity",
                            day.getDayNumber(),
                            !Boolean.FALSE.equals(activity.getIsIncludedInPrice())));
                    }
                }
            }
        }
        return lines;
    }

    /** The kinds of line the quote actually charges for. */
    private Set<QuoteItemType> pricedTypes(Quote quote) {
        Set<QuoteItemType> types = new LinkedHashSet<>();
        if (quote.getItems() == null) return types;
        for (QuoteItem item : quote.getItems()) {
            if (item.getItemType() != null) types.add(item.getItemType());
        }
        return types;
    }
}
