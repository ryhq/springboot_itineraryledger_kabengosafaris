package com.itineraryledger.kabengosafaris.Park.Services.ParkImageServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Park.Entities.ParkImage;
import com.itineraryledger.kabengosafaris.Park.Repositories.ParkImageRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for deleting park images.
 */
@Service
@Slf4j
@Transactional
public class ParkImageDeleteService {

    private final ParkImageRepository parkImageRepository;
    private final ParkImageStorageService storageService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ParkImageDeleteService(
        ParkImageRepository parkImageRepository,
        ParkImageStorageService storageService,
        IdObfuscator idObfuscator
    ) {
        this.parkImageRepository = parkImageRepository;
        this.storageService = storageService;
        this.idObfuscator = idObfuscator;
    }

    public ResponseEntity<ApiResponse<?>> bulkDeleteImages(List<String> obfuscatedIds) {
        log.info("Deleting {} park images", obfuscatedIds != null ? obfuscatedIds.size() : 0);

        try {
            if (obfuscatedIds == null || obfuscatedIds.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No image IDs provided", "NO_IDS_PROVIDED")
                );
            }

            List<Long> imageIds = new ArrayList<>();
            List<String> notFoundIds = new ArrayList<>();
            int deletedCount = 0;

            for (String obfuscatedId : obfuscatedIds) {
                try {
                    Long imageId = idObfuscator.decodeId(obfuscatedId);
                    imageIds.add(imageId);
                } catch (Exception e) {
                    log.warn("Failed to decode park image ID: {}", obfuscatedId, e);
                    notFoundIds.add(obfuscatedId);
                }
            }

            for (Long imageId : imageIds) {
                ParkImage image = parkImageRepository.findById(imageId).orElse(null);
                if (image != null) {
                    ParkImageDeleteService proxy = (ParkImageDeleteService) AopContext.currentProxy();
                    proxy.deleteSingleImage(imageId, image.getFileName());
                    deletedCount++;
                } else {
                    notFoundIds.add(idObfuscator.encodeId(imageId));
                }
            }

            if (deletedCount > 0) {
                return ResponseEntity.ok().body(
                    ApiResponse.success(200, deletedCount + " park image(s) deleted successfully", null)
                );
            } else {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No park images were deleted. " + notFoundIds.size() + " image(s) not found", "NO_IMAGES_DELETED")
                );
            }

        } catch (Exception e) {
            log.error("Error deleting park images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete park images", "PARK_IMAGE_DELETE_FAILED")
            );
        }
    }

    @AuditLogAnnotation(
        action = "DELETE_PARK_IMAGE",
        description = "Deleting park image",
        entityType = "ParkImage",
        entityIdParamName = "id"
    )
    public void deleteSingleImage(Long id, String fileName) {
        log.info("Deleting park image with ID: {}", id);

        parkImageRepository.deleteById(id);

        boolean fileDeleted = storageService.deleteImage(fileName);
        if (!fileDeleted) {
            log.warn("Image record deleted but file not found on disk: {}", fileName);
        }
    }
}
