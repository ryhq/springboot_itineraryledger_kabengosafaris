package com.itineraryledger.kabengosafaris.Initializers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Blog.DTOs.BlogBlockDTO;
import com.itineraryledger.kabengosafaris.Blog.DTOs.BlogFaqDTO;
import com.itineraryledger.kabengosafaris.Blog.Entity.Blog;
import com.itineraryledger.kabengosafaris.Blog.Repository.BlogRepository;
import com.itineraryledger.kabengosafaris.Blog.Services.BlogServices.BlogContentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Seeds ONE article, in its OWN transaction.
 *
 * A separate bean with REQUIRES_NEW is what makes the initializer's catch real: a
 * self-invoked @Transactional method runs on the caller's transaction, so a failure would
 * poison the whole boot transaction and take the application down with it. One post failing
 * must cost exactly that one post.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BlogSeeder {

    private final BlogRepository blogRepository;
    private final BlogContentService contentService;

    /** @return true when a post was written, false when it was already there. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean seed(SeedBlog seed) {
        String slug = seed.slug();
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("A seeded post needs a slug");
        }
        if (blogRepository.existsBySlug(slug)) {
            log.debug("Blog '{}' already exists — leaving it alone", slug);
            return false;
        }

        List<BlogBlockDTO> body = seed.body() != null ? seed.body() : new ArrayList<>();
        List<BlogFaqDTO> faqs = seed.faqs() != null ? seed.faqs() : new ArrayList<>();

        Blog blog = Blog.builder()
            .slug(slug)
            .title(seed.title())
            .excerpt(seed.excerpt())
            .author(seed.author())
            .publishDate(seed.date() != null ? LocalDate.parse(seed.date()) : LocalDate.now())
            .readMinutes(seed.readMinutes() != null && seed.readMinutes() > 0
                ? seed.readMinutes()
                : contentService.estimateReadMinutes(body))
            .bodyJson(contentService.writeJson(body))
            .faqsJson(contentService.writeJson(faqs))
            .tags(seed.tags() != null ? new ArrayList<>(seed.tags()) : new ArrayList<>())
            /* the site already serves these, so they arrive published */
            .isPublished(true)
            /*
             * And already at these addresses — the sitemap lists them — so the slug is frozen
             * from the date the article says it was published, not from this boot.
             */
            .firstPublishedAt(seed.date() != null
                ? LocalDate.parse(seed.date()).atStartOfDay()
                : LocalDateTime.now())
            .displayOrder(seed.displayOrder() != null ? seed.displayOrder() : 0)
            .build();

        blogRepository.save(blog);
        log.info("Seeded blog: {} ({} blocks, {} FAQs)", slug, body.size(), faqs.size());
        return true;
    }

    /**
     * The shape of one entry in seed/blogs.json — the website's own BlogPost, verbatim.
     *
     * A record rather than the entity so a change to either side is a compile error here
     * rather than a silent mis-seed.
     */
    public record SeedBlog(
        String slug,
        String title,
        String excerpt,
        String date,
        String author,
        Integer readMinutes,
        List<String> tags,
        List<BlogBlockDTO> body,
        List<BlogFaqDTO> faqs,
        Integer displayOrder
    ) {}
}
