package com.itineraryledger.kabengosafaris.ParkActivity.Services.ParkActivityImageServices;

import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityImage;
import com.itineraryledger.kabengosafaris.ParkActivity.Repositories.ParkActivityImageRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for deleting park activity images.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ParkActivityImageDeleteService {

    private final ParkActivityImageRepository parkActivityImageRepository;
    private final ParkActivityImageStorageService storageService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "DELETE_PARK_ACTIVITY_IMAGE",
        description = "Deleting park activity image",
        entityType = "ParkActivityImage"
    )
    public ResponseEntity<ApiResponse<?>> deleteImage(String obfuscatedId) {
        log.info("Deleting park activity image with ID: {}", obfuscatedId);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                log.warn("Failed to decode park activity image ID: {}", obfuscatedId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid image ID", "INVALID_IMAGE_ID")
                );
            }

            ParkActivityImage image = parkActivityImageRepository.findById(id).orElse(null);
            if (image == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Park activity image not found", "IMAGE_NOT_FOUND")
                );
            }

            String fileName = image.getFileName();

            // Delete database record
            parkActivityImageRepository.delete(image);

            // Delete file from storage
            if (fileName != null && !fileName.isBlank()) {
                boolean fileDeleted = storageService.deleteImage(fileName);
                if (!fileDeleted) {
                    log.warn("Failed to delete image file: {} - record already deleted", fileName);
                }
            }

            log.info("Park activity image deleted successfully: {}", id);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Park activity image deleted successfully", null)
            );

        } catch (Exception e) {
            log.error("Error deleting park activity image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete park activity image", "IMAGE_DELETE_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> bulkDeleteImages(List<String> obfuscatedIds) {
        log.info("Bulk deleting {} park activity images", obfuscatedIds != null ? obfuscatedIds.size() : 0);

        try {
            if (obfuscatedIds == null || obfuscatedIds.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No image IDs provided", "NO_IDS_PROVIDED")
                );
            }

            List<String> deletedIds = new ArrayList<>();
            List<String> failedIds = new ArrayList<>();

            ParkActivityImageDeleteService proxy = (ParkActivityImageDeleteService) AopContext.currentProxy();

            for (String obfuscatedId : obfuscatedIds) {
                try {
                    ResponseEntity<ApiResponse<?>> response = proxy.deleteImage(obfuscatedId);
                    if (response.getStatusCode().is2xxSuccessful()) {
                        deletedIds.add(obfuscatedId);
                    } else {
                        failedIds.add(obfuscatedId);
                    }
                } catch (Exception e) {
                    log.warn("Failed to delete image: {}", obfuscatedId, e);
                    failedIds.add(obfuscatedId);
                }
            }

            if (deletedIds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.error(400, "Failed to delete any images", "DELETE_FAILED")
                );
            }

            String message = String.format("%d image(s) deleted successfully", deletedIds.size());
            if (!failedIds.isEmpty()) {
                message += String.format(", %d failed", failedIds.size());
            }

            log.info("Bulk delete completed: {} deleted, {} failed", deletedIds.size(), failedIds.size());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, message, Map.of(
                    "deletedIds", deletedIds,
                    "failedIds", failedIds
                ))
            );

        } catch (Exception e) {
            log.error("Error bulk deleting park activity images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete park activity images", "BULK_DELETE_FAILED")
            );
        }
    }
}
