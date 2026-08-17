package com.itineraryledger.kabengosafaris.Blog.Services.BlogServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Blog.Entity.Blog;
import com.itineraryledger.kabengosafaris.Blog.Entity.BlogImage;
import com.itineraryledger.kabengosafaris.Blog.Repository.BlogRepository;
import com.itineraryledger.kabengosafaris.Blog.Services.BlogImageServices.BlogImageStorageService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Deleting articles, with a per-id report.
 *
 * A post owns its images, so deleting it deletes them — and the files with them, otherwise the
 * disk fills with pictures nothing can ever reach. Nothing else references a post, so there is
 * no reference check to make here; unpublishing remains the reversible option and the panel
 * offers it first.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BlogDeleteService {

    private final BlogRepository blogRepository;
    private final BlogImageStorageService imageStorageService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(action = "DELETE_BLOGS", description = "Deleting blog posts", entityType = "Blog")
    public ResponseEntity<ApiResponse<?>> deleteBlogs(List<String> obfuscatedIds) {
        if (obfuscatedIds == null || obfuscatedIds.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "No blog IDs provided", "NO_IDS_PROVIDED")
            );
        }

        int deletedCount = 0;
        List<String> deletedIds = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();

        for (String obfuscatedId : obfuscatedIds) {
            try {
                Long id = idObfuscator.decodeId(obfuscatedId);
                Blog blog = blogRepository.findById(id).orElse(null);
                if (blog == null) {
                    skipped.add(skip(obfuscatedId, "No such post — it may already have been deleted"));
                    continue;
                }

                List<String> fileNames = blog.getImages() == null ? List.of() : blog.getImages().stream()
                    .map(BlogImage::getFileName)
                    .filter(name -> name != null)
                    .toList();

                blogRepository.delete(blog);
                // rows first, then the files they pointed at
                fileNames.forEach(imageStorageService::deleteImage);

                deletedCount++;
                deletedIds.add(obfuscatedId);
            } catch (Exception e) {
                skipped.add(skip(obfuscatedId, e.getMessage() != null ? e.getMessage() : "Could not be deleted"));
            }
        }

        Map<String, Object> report = new HashMap<>();
        report.put("deletedCount", deletedCount);
        report.put("deletedIds", deletedIds);
        report.put("skipped", skipped);

        return ResponseEntity.ok(ApiResponse.success(200,
            deletedCount == 0 ? "No posts were deleted" : deletedCount + " post(s) deleted successfully",
            report));
    }

    private Map<String, Object> skip(String id, String reason) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("id", id);
        entry.put("reason", reason);
        return entry;
    }
}
