package com.itineraryledger.kabengosafaris.Hero.Services.HeroImageServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Hero.Entity.HeroImage;
import com.itineraryledger.kabengosafaris.Hero.Repository.HeroImageRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for deleting hero images.
 *
 * Provides:
 * - Bulk delete images by list of IDs (permanently removes from database and filesystem)
 */
@Service
@Slf4j
@Transactional
public class HeroImageDeleteService {

    private final HeroImageRepository heroImageRepository;
    private final HeroImageStorageService storageService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public HeroImageDeleteService(
        HeroImageRepository heroImageRepository,
        HeroImageStorageService storageService,
        IdObfuscator idObfuscator
    ) {
        this.heroImageRepository = heroImageRepository;
        this.storageService = storageService;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Bulk delete hero images by their IDs
     * Permanently removes from both database and filesystem
     *
     * @param obfuscatedIds List of obfuscated image IDs to delete
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> bulkDeleteImages(List<String> obfuscatedIds) {
        log.info("Deleting {} hero images", obfuscatedIds != null ? obfuscatedIds.size() : 0);

        try {
            if (obfuscatedIds == null || obfuscatedIds.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "No image IDs provided",
                        "NO_IDS_PROVIDED"
                    )
                );
            }

            List<Long> imageIds = new ArrayList<>();
            List<String> notFoundIds = new ArrayList<>();
            int deletedCount = 0;

            // Decode all IDs first
            for (String obfuscatedId : obfuscatedIds) {
                try {
                    Long imageId = idObfuscator.decodeId(obfuscatedId);
                    imageIds.add(imageId);
                } catch (Exception e) {
                    log.warn("Failed to decode hero image ID: {}", obfuscatedId, e);
                    notFoundIds.add(obfuscatedId);
                }
            }

            // Delete each image
            for (Long imageId : imageIds) {
                HeroImage image = heroImageRepository.findById(imageId).orElse(null);
                if (image != null) {
                    // Use proxy to ensure audit logging works
                    HeroImageDeleteService proxy = (HeroImageDeleteService) AopContext.currentProxy();
                    proxy.deleteSingleImage(imageId, image.getFileName());
                    deletedCount++;
                } else {
                    notFoundIds.add(idObfuscator.encodeId(imageId));
                }
            }

            // Prepare response
            if (deletedCount > 0) {
                return ResponseEntity.ok().body(
                    ApiResponse.success(
                        200,
                        deletedCount + " hero image(s) deleted successfully",
                        null
                    )
                );
            } else {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "No hero images were deleted. " + notFoundIds.size() + " image(s) not found",
                        "NO_IMAGES_DELETED"
                    )
                );
            }

        } catch (Exception e) {
            log.error("Error deleting hero images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete hero images",
                    "HERO_IMAGE_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete a single hero image by ID (with audit logging)
     * Also removes the file from storage
     *
     * @param id The image ID
     * @param fileName The filename to delete from storage
     */
    @AuditLogAnnotation(
        action = "DELETE_HERO_IMAGE",
        description = "Deleting hero image",
        entityType = "HeroImage",
        entityIdParamName = "id"
    )
    public void deleteSingleImage(Long id, String fileName) {
        log.info("Deleting hero image with ID: {}", id);

        // Delete from database
        heroImageRepository.deleteById(id);

        // Delete file from storage
        boolean fileDeleted = storageService.deleteImage(fileName);
        if (!fileDeleted) {
            log.warn("Image record deleted but file not found on disk: {}", fileName);
        }
    }
}
