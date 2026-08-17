package com.itineraryledger.kabengosafaris.Blog.Services.BlogServices;

import java.util.ArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Blog.DTOs.UpdateBlogDTO;
import com.itineraryledger.kabengosafaris.Blog.Entity.Blog;
import com.itineraryledger.kabengosafaris.Blog.Repository.BlogRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Editing an article.
 *
 * Null means "leave it alone" for every scalar, which is what click-to-edit needs: saving the
 * title cannot blank the excerpt somebody else is writing. Lists (body, faqs, tags) are
 * applied whenever present, because an EMPTY list is a real edit — "this post no longer has
 * an FAQ block".
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BlogUpdateService {

    private final BlogRepository blogRepository;
    private final BlogContentService contentService;
    private final BlogCreateService createService;
    private final BlogGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(action = "UPDATE_BLOG", description = "Updating a blog post", entityType = "Blog")
    public ResponseEntity<ApiResponse<?>> updateBlog(String obfuscatedId, UpdateBlogDTO updateDTO) {
        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid blog ID", "INVALID_BLOG_ID")
                );
            }

            Blog blog = blogRepository.findById(id).orElse(null);
            if (blog == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Blog post not found", "BLOG_NOT_FOUND")
                );
            }

            /*
             * POLICY: a published article's slug is frozen. Refused rather than ignored — a
             * caller that asked for a new address deserves to be told why it did not happen,
             * and a silent no-op reads as a save that failed to stick.
             */
            if (updateDTO.getSlug() != null && !updateDTO.getSlug().isBlank() && blog.getFirstPublishedAt() != null) {
                String wanted = contentService.slugify(updateDTO.getSlug());
                if (wanted != null && !wanted.equals(blog.getSlug())) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            "This article has been published, so its address cannot change. "
                                + "/blog/" + blog.getSlug() + " is already in search results and in people's links; "
                                + "renaming it would turn every one of those into a 404.",
                            "SLUG_LOCKED"
                        )
                    );
                }
            }

            if (updateDTO.getTitle() != null) blog.setTitle(updateDTO.getTitle());
            if (updateDTO.getExcerpt() != null) blog.setExcerpt(updateDTO.getExcerpt());
            if (updateDTO.getAuthor() != null) blog.setAuthor(updateDTO.getAuthor());
            if (updateDTO.getPublishDate() != null) blog.setPublishDate(updateDTO.getPublishDate());
            if (updateDTO.getIsPublished() != null) {
                blog.setIsPublished(updateDTO.getIsPublished());
                /* the first publish is what freezes the slug, and it is recorded once */
                if (Boolean.TRUE.equals(updateDTO.getIsPublished()) && blog.getFirstPublishedAt() == null) {
                    blog.setFirstPublishedAt(java.time.LocalDateTime.now());
                    log.info("Blog '{}' published for the first time — its slug is now fixed", blog.getSlug());
                }
            }
            if (updateDTO.getDisplayOrder() != null) blog.setDisplayOrder(updateDTO.getDisplayOrder());
            if (updateDTO.getMetaTitle() != null) blog.setMetaTitle(updateDTO.getMetaTitle());
            if (updateDTO.getMetaDescription() != null) blog.setMetaDescription(updateDTO.getMetaDescription());

            /*
             * Only reachable while the article has never been published (the check above
             * refuses it otherwise). A draft's address is nobody's link yet, so renaming it is
             * free; it is still made unique, because the slug IS the address.
             */
            if (updateDTO.getSlug() != null && !updateDTO.getSlug().isBlank()) {
                String wanted = contentService.slugify(updateDTO.getSlug());
                if (wanted != null && !wanted.equals(blog.getSlug())) {
                    String free = createService.uniqueSlug(wanted, blog.getId());
                    log.info("Blog slug changing: {} -> {}", blog.getSlug(), free);
                    blog.setSlug(free);
                }
            }

            if (updateDTO.getTags() != null) {
                blog.getTags().clear();
                blog.getTags().addAll(updateDTO.getTags());
            }

            boolean bodyChanged = false;
            if (updateDTO.getBody() != null) {
                blog.setBodyJson(contentService.writeJson(updateDTO.getBody()));
                bodyChanged = true;
            }
            if (updateDTO.getFaqs() != null) {
                blog.setFaqsJson(contentService.writeJson(updateDTO.getFaqs()));
            }

            /*
             * The reading time follows the body unless somebody has typed one. An explicit
             * value always wins — the field is editable, and silently overwriting what an
             * editor set would make it look broken.
             */
            if (updateDTO.getReadMinutes() != null) {
                blog.setReadMinutes(updateDTO.getReadMinutes());
            } else if (bodyChanged) {
                blog.setReadMinutes(contentService.estimateReadMinutes(updateDTO.getBody()));
            }

            blog.setUpdatedBy(currentUser());
            blog = blogRepository.save(blog);

            return ResponseEntity.ok(ApiResponse.success(200, "Blog post updated successfully", getService.toDTO(blog)));
        } catch (Exception e) {
            log.error("Error updating blog post", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update blog post", "BLOG_UPDATE_FAILED")
            );
        }
    }

    /** Publish / unpublish on its own, for the list's bulk actions and the editor's toggle. */
    @AuditLogAnnotation(action = "PUBLISH_BLOG", description = "Publishing or unpublishing a blog post", entityType = "Blog")
    public ResponseEntity<ApiResponse<?>> setPublished(String obfuscatedId, boolean published) {
        UpdateBlogDTO patch = new UpdateBlogDTO();
        patch.setIsPublished(published);
        patch.setBody(null);
        patch.setFaqs(null);
        patch.setTags(null);
        return updateBlog(obfuscatedId, patch);
    }

    private User currentUser() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return principal instanceof User user ? user : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Kept so a caller can hand us an empty list without Lombok complaining about generics. */
    static ArrayList<Object> emptyList() {
        return new ArrayList<>();
    }
}
