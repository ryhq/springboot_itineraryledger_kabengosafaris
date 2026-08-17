package com.itineraryledger.kabengosafaris.Blog.Services.BlogImageServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Blog.DTOs.BlogImageDTOs.ReorderBlogImagesDTO;
import com.itineraryledger.kabengosafaris.Blog.Entity.BlogImage;
import com.itineraryledger.kabengosafaris.Blog.Repository.BlogImageRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The gallery's order, set as a whole list.
 *
 * Position in the list IS the display order, and every id must belong to the post being
 * reordered — a stray id would otherwise renumber some other post's gallery.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BlogImageReorderService {

    private final BlogImageRepository blogImageRepository;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(action = "REORDER_BLOG_IMAGES", description = "Reordering blog images", entityType = "BlogImage")
    public ResponseEntity<ApiResponse<?>> reorder(ReorderBlogImagesDTO dto) {
        try {
            Long blogId = idObfuscator.decodeId(dto.getBlogId());
            List<BlogImage> existing = blogImageRepository.findByBlogIdOrderByDisplayOrderAscIdAsc(blogId);
            Map<Long, BlogImage> byId = new HashMap<>();
            existing.forEach(image -> byId.put(image.getId(), image));

            List<String> skipped = new ArrayList<>();
            int position = 1;
            for (ReorderBlogImagesDTO.ImageOrderItem item : dto.getImageOrder()) {
                Long imageId;
                try {
                    imageId = idObfuscator.decodeId(item.getImageId());
                } catch (Exception e) {
                    skipped.add(item.getImageId() + ": unreadable id");
                    continue;
                }
                BlogImage image = byId.get(imageId);
                if (image == null) {
                    skipped.add(item.getImageId() + ": not an image of this post");
                    continue;
                }
                image.setDisplayOrder(position++);
                blogImageRepository.save(image);
            }

            Map<String, Object> report = new HashMap<>();
            report.put("reorderedCount", position - 1);
            report.put("skipped", skipped);
            if (!skipped.isEmpty()) log.warn("Blog image reorder skipped: {}", skipped);

            return ResponseEntity.ok(ApiResponse.success(200, "Blog images reordered successfully", report));
        } catch (Exception e) {
            log.error("Error reordering blog images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to reorder blog images", "IMAGE_REORDER_FAILED")
            );
        }
    }
}
