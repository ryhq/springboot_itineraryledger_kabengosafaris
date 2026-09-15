package com.itineraryledger.kabengosafaris.Initializers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Inclusion.Entity.InclusionItem;
import com.itineraryledger.kabengosafaris.Inclusion.Repository.InclusionItemRepository;
import com.itineraryledger.kabengosafaris.Inclusion.Services.ItineraryInclusionReader;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryInclusion.Entity.ItineraryInclusion;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Turns each itinerary's typed promise into rows against the catalogue.
 *
 * <p>Runs once in effect: an itinerary that already has rows is skipped, so this is a no-op on
 * every later boot.
 *
 * <p><strong>All or nothing per itinerary.</strong> {@link ItineraryInclusionReader} falls back to
 * the old text columns only while an itinerary has ZERO rows, so linking three of an itinerary's
 * four lines would stop the fallback and silently drop the fourth. Three itineraries have their
 * entire promise typed as one 300-character paragraph — one "line" that is really a sentence of
 * prose — and those are left exactly as they are, still served by the fallback, for a human to
 * split up when they choose. Better a trip that has not been converted than one converted badly.
 *
 * <p>Each itinerary is written in its own REQUIRES_NEW transaction inside its own try/catch, and
 * the whole sweep is wrapped again: a backfill must never be the reason an application will not
 * start.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE - 90)
public class ItineraryInclusionBackfillInitializer implements ApplicationRunner {

    private final ItineraryRepository itineraries;
    private final ItineraryInclusionBackfill backfill;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<Long> candidates = itineraries.findAll().stream()
                .filter(i -> i.getInclusionList() == null || i.getInclusionList().isEmpty())
                .filter(i -> notBlank(i.getInclusions()) || notBlank(i.getExclusions()))
                .map(Itinerary::getId)
                .toList();

            if (candidates.isEmpty()) return;

            int converted = 0;
            int leftAsProse = 0;
            int failed = 0;
            int created = 0;

            for (Long id : candidates) {
                try {
                    ItineraryInclusionBackfill.Outcome outcome = backfill.convert(id);
                    switch (outcome.result()) {
                        case CONVERTED -> converted++;
                        case LEFT_AS_PROSE -> leftAsProse++;
                    }
                    created += outcome.itemsCreated();
                } catch (Exception e) {
                    failed++;
                    log.warn("INCLUSION BACKFILL: itinerary {} could not be converted: {}",
                        id, e.getMessage());
                }
            }

            log.info("INCLUSION BACKFILL: {} itinerar{} converted, {} left as prose for a human to "
                + "split, {} failed, {} new catalogue line(s) created",
                converted, converted == 1 ? "y" : "ies", leftAsProse, failed, created);
        } catch (Exception e) {
            log.error("INCLUSION BACKFILL: sweep failed, itineraries keep their typed text: {}",
                e.getMessage(), e);
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * The per-itinerary write, in its own transaction.
     *
     * <p>A separate bean because a REQUIRES_NEW boundary is only honoured through the proxy — a
     * self-call from the runner above would silently join the outer transaction and one bad
     * itinerary would roll back every itinerary before it.
     */
    @Component
    @RequiredArgsConstructor
    @Slf4j
    public static class ItineraryInclusionBackfill {

        /**
         * Longer than this and it is prose, not a line.
         *
         * <p>The longest of the fourteen house lines is 51 characters. The three paragraphs are
         * over 300. 200 leaves room for a genuinely long line without mistaking a paragraph for one.
         */
        private static final int LINE_LIMIT = 200;

        private final ItineraryRepository itineraries;
        private final InclusionItemRepository inclusionItems;

        public record Outcome(Result result, int itemsCreated) {}

        public enum Result { CONVERTED, LEFT_AS_PROSE }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public Outcome convert(Long itineraryId) {
            Itinerary itinerary = itineraries.findById(itineraryId).orElse(null);
            if (itinerary == null) return new Outcome(Result.LEFT_AS_PROSE, 0);

            List<String> included = ItineraryInclusionReader.splitLines(itinerary.getInclusions());
            List<String> excluded = ItineraryInclusionReader.splitLines(itinerary.getExclusions());

            /*
             * Decided before anything is written. One paragraph anywhere and the whole itinerary
             * stays on the fallback, because the fallback is all-or-nothing per itinerary.
             */
            boolean anyProse = included.stream().anyMatch(l -> l.length() > LINE_LIMIT)
                || excluded.stream().anyMatch(l -> l.length() > LINE_LIMIT);
            if (anyProse) {
                log.info("INCLUSION BACKFILL: {} keeps its typed text — a line of {} characters is "
                    + "a paragraph, not a list item",
                    itinerary.getCode(),
                    java.util.stream.Stream.concat(included.stream(), excluded.stream())
                        .mapToInt(String::length).max().orElse(0));
                return new Outcome(Result.LEFT_AS_PROSE, 0);
            }

            int created = 0;
            int position = 1;
            List<ItineraryInclusion> rows = new ArrayList<>();

            for (String line : included) {
                Resolution r = resolve(line, true);
                created += r.created() ? 1 : 0;
                rows.add(ItineraryInclusion.builder()
                    .inclusionItem(r.item()).isIncluded(true).sortOrder(position++).build());
            }
            for (String line : excluded) {
                Resolution r = resolve(line, false);
                created += r.created() ? 1 : 0;
                rows.add(ItineraryInclusion.builder()
                    .inclusionItem(r.item()).isIncluded(false).sortOrder(position++).build());
            }

            if (rows.isEmpty()) return new Outcome(Result.LEFT_AS_PROSE, created);

            for (ItineraryInclusion row : rows) itinerary.addInclusion(row);

            /*
             * Cleared, so there is one answer to the question rather than two. The reader would
             * never consult them again anyway, but the next person to read the column would have
             * no way of knowing that.
             */
            itinerary.setInclusions(null);
            itinerary.setExclusions(null);
            itineraries.save(itinerary);

            return new Outcome(Result.CONVERTED, created);
        }

        private record Resolution(InclusionItem item, boolean created) {}

        /**
         * An existing line, or a new catalogue entry for one nobody else uses.
         *
         * <p>A line this backfill invents is {@code isStandard = false}: the Arusha day trip's
         * "Arusha National Park entrance and conservation fees" is true of that trip and of no
         * other, and a standard line would put it on every Serengeti itinerary created afterwards.
         *
         * <p>Near-duplicates are NOT merged. "Professional multilingual safari guide" and
         * "Professional speaking safari guide" are two different promises, and a fuzzy match that
         * folded one into the other would change what a customer was told. Two rows that a person
         * can merge later beats one row that says something nobody wrote.
         */
        private Resolution resolve(String line, boolean included) {
            var existing = inclusionItems.findByLabelIgnoringCaseAndSpace(line);
            if (existing.isPresent()) return new Resolution(existing.get(), false);

            InclusionItem item = inclusionItems.saveAndFlush(InclusionItem.builder()
                .label(line)
                .category("Trip-specific")
                .displayOrder(safe(inclusionItems.findMaxDisplayOrder()) + 1)
                .isActive(true)
                .isSystem(false)
                .isStandard(false)
                .defaultIncluded(included)
                /* Nothing is claimed for text nobody has reviewed — a guess here fires warnings. */
                .claimAppliesTo(null)
                .internalNotes("Created from an itinerary's typed text when the catalogue arrived.")
                .build());
            item.setCode(item.generateCode());
            inclusionItems.save(item);
            return new Resolution(item, true);
        }

        private int safe(Integer value) {
            return value != null ? value : 0;
        }
    }
}
