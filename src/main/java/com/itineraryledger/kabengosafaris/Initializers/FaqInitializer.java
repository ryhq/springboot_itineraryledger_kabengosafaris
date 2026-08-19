package com.itineraryledger.kabengosafaris.Initializers;

import java.io.InputStream;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Seeds the global FAQ list the website already publishes.
 *
 * Idempotent on the QUESTION, because that is what a reader recognises and what an editor
 * would notice being duplicated. Same resilience as BlogInitializer: one FAQ per transaction,
 * every failure logged and skipped, startup never at risk.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE - 99)
public class FaqInitializer implements ApplicationRunner {

    /**
     * Seeded content belongs to ONE company.
     *
     * seed/faqs.json holds real FAQs written for the company this repo grew up serving. A
     * second company inheriting them would publish somebody else's words on its own website, so this
     * defaults to OFF and an installation that wants them says so. The company already carrying these
     * rows is unaffected: seeding skips what exists.
     */
    @org.springframework.beans.factory.annotation.Value("${app.seed.demo-content.enabled:false}")
    private boolean demoContentEnabled;

    private static final String SEED_FILE = "seed/faqs.json";

    private final FaqSeeder seeder;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) {
        if (!demoContentEnabled) {
            log.info("Skipping seeded FAQs (app.seed.demo-content.enabled is false)");
            return;
        }

        try {
            List<FaqSeeder.SeedFaq> seeds = readSeeds();
            if (seeds.isEmpty()) {
                log.info("FAQ INITIALIZER: nothing to seed ({} not found or empty)", SEED_FILE);
                return;
            }

            int created = 0;
            int skipped = 0;
            int failed = 0;
            int position = 1;

            for (FaqSeeder.SeedFaq seed : seeds) {
                try {
                    if (seeder.seed(seed, position)) created++;
                    else skipped++;
                } catch (Exception e) {
                    failed++;
                    log.warn("FAQ INITIALIZER: could not seed '{}': {}",
                        seed != null ? seed.q() : "(no question)", e.getMessage());
                }
                /* the position advances regardless, so the order matches the file */
                position++;
            }

            log.info("FAQ INITIALIZER: {} created, {} already present, {} failed (of {} in {})",
                created, skipped, failed, seeds.size(), SEED_FILE);
        } catch (Exception e) {
            log.error("FAQ INITIALIZER: skipped entirely — {}", e.getMessage(), e);
        }
    }

    private List<FaqSeeder.SeedFaq> readSeeds() throws Exception {
        ClassPathResource resource = new ClassPathResource(SEED_FILE);
        if (!resource.exists()) return List.of();
        try (InputStream in = resource.getInputStream()) {
            List<FaqSeeder.SeedFaq> seeds =
                objectMapper.readValue(in, new TypeReference<List<FaqSeeder.SeedFaq>>() {});
            return seeds != null ? seeds : List.of();
        }
    }
}
