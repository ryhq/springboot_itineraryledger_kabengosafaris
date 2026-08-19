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
 * Seeds the articles the website is already serving, so a fresh database matches the live site.
 *
 * Idempotent: a post whose slug exists is left exactly as it is, which means editing a seeded
 * article in the panel is safe — the next restart will not overwrite the edit.
 *
 * RESILIENT BY CONSTRUCTION: every post is seeded in its own REQUIRES_NEW transaction inside
 * its own try/catch, and the whole run is wrapped again. A bad seed costs one article and a
 * warning in the log; it can never abort startup. We have had a production crash-loop from an
 * initializer that threw, and this shape is the answer to it.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class BlogInitializer implements ApplicationRunner {

    /**
     * Seeded content belongs to ONE company.
     *
     * seed/blogs.json holds real blog posts written for the company this repo grew up serving. A
     * second company inheriting them would publish somebody else's words on its own website, so this
     * defaults to OFF and an installation that wants them says so. The company already carrying these
     * rows is unaffected: seeding skips what exists.
     */
    @org.springframework.beans.factory.annotation.Value("${app.seed.demo-content.enabled:false}")
    private boolean demoContentEnabled;

    private static final String SEED_FILE = "seed/blogs.json";

    private final BlogSeeder seeder;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) {
        if (!demoContentEnabled) {
            log.info("Skipping seeded blog posts (app.seed.demo-content.enabled is false)");
            return;
        }

        try {
            List<BlogSeeder.SeedBlog> seeds = readSeeds();
            if (seeds.isEmpty()) {
                log.info("BLOG INITIALIZER: nothing to seed ({} not found or empty)", SEED_FILE);
                return;
            }

            int created = 0;
            int skipped = 0;
            int failed = 0;

            for (BlogSeeder.SeedBlog seed : seeds) {
                try {
                    if (seeder.seed(seed)) created++;
                    else skipped++;
                } catch (Exception e) {
                    // one article's problem, not the application's
                    failed++;
                    log.warn("BLOG INITIALIZER: could not seed '{}': {}",
                        seed != null ? seed.slug() : "(no slug)", e.getMessage());
                }
            }

            log.info("BLOG INITIALIZER: {} created, {} already present, {} failed (of {} in {})",
                created, skipped, failed, seeds.size(), SEED_FILE);
        } catch (Exception e) {
            // even reading the file must not be fatal
            log.error("BLOG INITIALIZER: skipped entirely — {}", e.getMessage(), e);
        }
    }

    private List<BlogSeeder.SeedBlog> readSeeds() throws Exception {
        ClassPathResource resource = new ClassPathResource(SEED_FILE);
        if (!resource.exists()) return List.of();
        try (InputStream in = resource.getInputStream()) {
            List<BlogSeeder.SeedBlog> seeds =
                objectMapper.readValue(in, new TypeReference<List<BlogSeeder.SeedBlog>>() {});
            return seeds != null ? seeds : List.of();
        }
    }
}
