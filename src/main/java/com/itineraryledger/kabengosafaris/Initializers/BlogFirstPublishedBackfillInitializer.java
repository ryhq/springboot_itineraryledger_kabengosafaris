package com.itineraryledger.kabengosafaris.Initializers;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Blog.Entity.Blog;
import com.itineraryledger.kabengosafaris.Blog.Repository.BlogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Fills in first_published_at for articles that were already live when the column arrived.
 *
 * The six seeded posts were published before there was anywhere to record it, and the seeder
 * is idempotent — it skipped them, so the column stayed null and the record page reported
 * "First published: Never" about articles the sitemap has been serving for weeks.
 *
 * Runs once in effect: a post that has a date is left alone, so this is a no-op on every
 * later boot. Each row goes in its own REQUIRES_NEW transaction inside its own try/catch, and
 * the whole sweep is wrapped again — a backfill must never be the reason an application will
 * not start.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE - 98)
public class BlogFirstPublishedBackfillInitializer implements ApplicationRunner {

    private final BlogRepository blogRepository;
    private final BlogFirstPublishedBackfill backfill;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<Blog> published = blogRepository.findByIsPublishedTrueOrderByDisplayOrderAscPublishDateDesc();
            int filled = 0;
            int failed = 0;

            for (Blog blog : published) {
                if (blog.getFirstPublishedAt() != null) continue;
                try {
                    backfill.stamp(blog.getId());
                    filled++;
                } catch (Exception e) {
                    failed++;
                    log.warn("BLOG BACKFILL: could not stamp '{}': {}", blog.getSlug(), e.getMessage());
                }
            }

            if (filled > 0 || failed > 0) {
                log.info("BLOG BACKFILL: first-published stamped on {} article(s), {} failed", filled, failed);
            }
        } catch (Exception e) {
            log.error("BLOG BACKFILL: skipped entirely — {}", e.getMessage(), e);
        }
    }

    /** One row, one transaction — see BlogSeeder for why that separation is what makes the catch real. */
    @Component
    @RequiredArgsConstructor
    static class BlogFirstPublishedBackfill {

        private final BlogRepository blogRepository;

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void stamp(Long id) {
            Blog blog = blogRepository.findById(id).orElse(null);
            if (blog == null || blog.getFirstPublishedAt() != null) return;
            /*
             * The publish date the article itself carries, not now: that is when readers could
             * first reach it, and stamping today's date would misdate six articles at once.
             */
            blog.setFirstPublishedAt(
                blog.getPublishDate() != null
                    ? blog.getPublishDate().atStartOfDay()
                    : (blog.getCreatedAt() != null ? blog.getCreatedAt() : LocalDateTime.now())
            );
            blogRepository.save(blog);
        }
    }
}
