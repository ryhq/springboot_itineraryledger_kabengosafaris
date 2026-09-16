package com.itineraryledger.kabengosafaris.Initializers;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Inclusion.Entity.InclusionItem;
import com.itineraryledger.kabengosafaris.Inclusion.Repository.InclusionItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The standard lines a trip's promise is assembled from.
 *
 * <p>These fourteen are not invented here. They are the exact text 58 of the 65 live itineraries
 * already carried, byte for byte, pasted into each one by hand — which is what this replaces. The
 * wording is preserved character for character (including the × in "4×4" and the ampersands), so
 * that the backfill can match existing itineraries against it by exact text and no customer-facing
 * sentence changes on the day this ships.
 *
 * <p>Idempotent by label: an item that exists is left alone, never updated and never duplicated.
 * A later boot tops up anything missing, so deleting one by accident is recoverable by restarting;
 * editing one deliberately survives, because the office's wording outranks ours.
 *
 * <p>Runs after the pax categories so that the whole catalogue family initialises together.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class InclusionItemInitializer implements ApplicationRunner {

    private final InclusionItemRepository repository;

    /**
     * One seed line.
     *
     * @param claimAppliesTo LineCategoryScope text, or null for "this sentence claims nothing a
     *                       machine can check". Null is set deliberately and often: a scope that
     *                       fires on every quote produces a warning everybody learns to ignore.
     */
    private record Seed(
        String label,
        String category,
        boolean defaultIncluded,
        String claimAppliesTo
    ) {}

    private static final List<Seed> SEEDS = List.of(
        /* --- what a Kabengo safari includes, in the order they have always printed --- */
        new Seed("Private 4×4 safari vehicle with pop-up roof", "Transport", true, "TRANSPORT"),
        /*
         * Null, not GUIDE. Checked against real quotes: they only ever carry ACCOMMODATION,
         * PARK_FEE, ACTIVITY and TRANSPORT items. The guide is inside the day rate and is never
         * billed as a line of its own, so a GUIDE claim reported "nothing of that kind on this
         * quote" on every quote ever written.
         */
        new Seed("Professional multilingual safari guide", "Guiding", true, null),
        new Seed("All park, conservation & crater-service fees", "Park & conservation fees", true, "PARK_FEE"),
        new Seed("Accommodation with meals as listed in the itinerary", "Accommodation & meals", true, "ACCOMMODATION,MEALS"),
        new Seed("Airport transfers on arrival & departure", "Transport", true, "TRANSPORT"),
        /* No line type stands for bottled water, so it claims nothing and is never checked. */
        new Seed("Drinking water on game drives", "Provisions", true, null),
        /* Tax is not a QuoteItemType — it is a percentage over other lines. */
        new Seed("Government taxes & levies", "Taxes", true, null),
        /* Null for the same reason: cover is bundled, never an INSURANCE line. */
        new Seed("Flying-doctors emergency evacuation cover", "Safety & cover", true, null),

        /* --- what it does not, same order --- */
        /*
         * VISA, not TRANSPORT. An internal hopper between airstrips IS a transport line and is
         * routinely included, so scoping this to TRANSPORT would accuse every flying safari of
         * contradicting itself.
         */
        new Seed("International flights & visas", "Personal & optional", false, "VISA"),
        /*
         * Null, not INSURANCE: the flying-doctors cover above is an INSURANCE line we DO include,
         * so this would report a contradiction on every standard quote.
         */
        new Seed("Travel & medical insurance", "Personal & optional", false, null),
        new Seed("Tips & gratuities for guide and lodge staff", "Personal & optional", false, null),
        new Seed("Drinks & premium beverages", "Personal & optional", false, null),
        new Seed("Laundry, personal items & souvenirs", "Personal & optional", false, null),
        /* Null, not ACTIVITY: every game drive is an included ACTIVITY line. */
        new Seed("Optional activities (e.g. balloon safari, cultural visits)", "Personal & optional", false, null)
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            int created = 0;
            int existing = 0;
            int order = 1;

            for (Seed seed : SEEDS) {
                int position = order++;
                if (repository.findByLabelIgnoringCaseAndSpace(seed.label()).isPresent()) {
                    existing++;
                    continue;
                }
                InclusionItem item = repository.saveAndFlush(InclusionItem.builder()
                    .label(seed.label())
                    .category(seed.category())
                    .displayOrder(position)
                    .isActive(true)
                    .isSystem(true)
                    .isStandard(true)
                    .defaultIncluded(seed.defaultIncluded())
                    .claimAppliesTo(seed.claimAppliesTo())
                    .build());
                item.setCode(item.generateCode());
                repository.save(item);
                created++;
            }

            if (created > 0) {
                log.info("INCLUSION CATALOGUE: seeded {} standard line(s), {} already present",
                    created, existing);
            } else {
                log.debug("INCLUSION CATALOGUE: all {} standard lines already present", existing);
            }
        } catch (Exception e) {
            /* A catalogue that failed to seed must never be the reason the application will not start. */
            log.error("INCLUSION CATALOGUE: seeding failed, the standard lines are missing: {}",
                e.getMessage(), e);
        }
    }
}
