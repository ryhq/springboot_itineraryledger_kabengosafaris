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
import com.itineraryledger.kabengosafaris.Blog.Entity.BlogImage;
import com.itineraryledger.kabengosafaris.Blog.Repository.BlogImageRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Deleting images, with a per-id report.
 *
 * The row goes first and the file after it: an orphaned file wastes disk, but a row pointing
 * at a file that is gone breaks every page that renders it.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BlogImageDeleteService {

    private final BlogImageRepository blogImageRepository;
    private final BlogImageStorageService storageService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(action = "DELETE_BLOG_IMAGES", description = "Deleting blog images", entityType = "BlogImage")
    public ResponseEntity<ApiResponse<?>> deleteImages(List<String> obfuscatedIds) {
        if (obfuscatedIds == null || obfuscatedIds.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "No image IDs provided", "NO_IDS_PROVIDED")
            );
        }

        int deletedCount = 0;
        List<String> deletedIds = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();

        for (String obfuscatedId : obfuscatedIds) {
            try {
                Long id = idObfuscator.decodeId(obfuscatedId);
                BlogImage image = blogImageRepository.findById(id).orElse(null);
                if (image == null) {
                    skipped.add(skip(obfuscatedId, "No such image — it may already have been deleted"));
                    continue;
                }
                String fileName = image.getFileName();
                blogImageRepository.delete(image);
                if (fileName != null) storageService.deleteImage(fileName);
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
            deletedCount == 0 ? "No images were deleted" : deletedCount + " image(s) deleted successfully",
            report));
    }

    private Map<String, Object> skip(String id, String reason) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("id", id);
        entry.put("reason", reason);
        return entry;
    }
}
