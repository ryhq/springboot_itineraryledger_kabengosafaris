package com.itineraryledger.kabengosafaris.Blog.Services.BlogServices;

import java.time.LocalDate;
import java.util.ArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Blog.DTOs.CreateBlogDTO;
import com.itineraryledger.kabengosafaris.Blog.Entity.Blog;
import com.itineraryledger.kabengosafaris.Blog.Repository.BlogRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.User.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Writing a new article. */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BlogCreateService {

    private final BlogRepository blogRepository;
    private final BlogContentService contentService;
    private final BlogGetService getService;

    @AuditLogAnnotation(action = "CREATE_BLOG", description = "Creating a blog post", entityType = "Blog")
    public ResponseEntity<ApiResponse<?>> createBlog(CreateBlogDTO createDTO) {
        log.info("Creating blog post: {}", createDTO.getTitle());
        try {
            User currentUser = currentUser();

            String slug = uniqueSlug(
                createDTO.getSlug() != null && !createDTO.getSlug().isBlank()
                    ? contentService.slugify(createDTO.getSlug())
                    : contentService.slugify(createDTO.getTitle()),
                null
            );
            if (slug == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "A title with at least one letter or number is required to build a slug", "INVALID_SLUG")
                );
            }

            var blocks = createDTO.getBody() != null ? createDTO.getBody() : new ArrayList<com.itineraryledger.kabengosafaris.Blog.DTOs.BlogBlockDTO>();

            Blog blog = Blog.builder()
                .slug(slug)
                .title(createDTO.getTitle())
                .excerpt(createDTO.getExcerpt())
                .author(createDTO.getAuthor())
                .publishDate(createDTO.getPublishDate() != null ? createDTO.getPublishDate() : LocalDate.now())
                /* blank means "work it out from the body", and it stays editable afterwards */
                .readMinutes(createDTO.getReadMinutes() != null
                    ? createDTO.getReadMinutes()
                    : contentService.estimateReadMinutes(blocks))
                .bodyJson(contentService.writeJson(blocks))
                .faqsJson(contentService.writeJson(createDTO.getFaqs() != null ? createDTO.getFaqs() : new ArrayList<>()))
                .tags(createDTO.getTags() != null ? new ArrayList<>(createDTO.getTags()) : new ArrayList<>())
                .isPublished(createDTO.getIsPublished() != null ? createDTO.getIsPublished() : false)
                .displayOrder(createDTO.getDisplayOrder() != null
                    ? createDTO.getDisplayOrder()
                    : safeOrder(blogRepository.findMaxDisplayOrder()) + 1)
                .metaTitle(createDTO.getMetaTitle())
                .metaDescription(createDTO.getMetaDescription())
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

            /* published on the way in? then it has been live since now, and the slug is fixed */
            if (Boolean.TRUE.equals(blog.getIsPublished())) {
                blog.setFirstPublishedAt(java.time.LocalDateTime.now());
            }

            blog = blogRepository.save(blog);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Blog post created successfully", getService.toDTO(blog))
            );
        } catch (Exception e) {
            log.error("Error creating blog post", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create blog post", "BLOG_CREATE_FAILED")
            );
        }
    }

    /**
     * A free slug.
     *
     * The slug IS the article's public address, so a collision cannot be allowed to 500 on
     * save; a numeric suffix is added until it is free.
     */
    String uniqueSlug(String base, Long selfId) {
        if (base == null || base.isBlank()) return null;
        String candidate = base;
        int suffix = 2;
        while (selfId == null
            ? blogRepository.existsBySlug(candidate)
            : blogRepository.existsBySlugAndIdNot(candidate, selfId)) {
            candidate = base + "-" + suffix++;
            if (suffix > 200) return base + "-" + System.currentTimeMillis();
        }
        return candidate;
    }

    private int safeOrder(Integer value) {
        return value != null ? value : 0;
    }

    private User currentUser() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return principal instanceof User user ? user : null;
        } catch (Exception e) {
            // the seeder runs without a session; a post with no author recorded is still valid
            return null;
        }
    }
}
