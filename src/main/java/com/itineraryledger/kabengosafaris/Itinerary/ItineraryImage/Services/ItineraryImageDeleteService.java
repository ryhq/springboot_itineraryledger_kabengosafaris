package com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Entity.ItineraryImage;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Repository.ItineraryImageRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for deleting itinerary images.
 */
@Service
@Slf4j
@Transactional
public class ItineraryImageDeleteService {

    private final ItineraryImageRepository itineraryImageRepository;
    private final ItineraryImageStorageService storageService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryImageDeleteService(
        ItineraryImageRepository itineraryImageRepository,
        ItineraryImageStorageService storageService,
        IdObfuscator idObfuscator
    ) {
        this.itineraryImageRepository = itineraryImageRepository;
        this.storageService = storageService;
        this.idObfuscator = idObfuscator;
    }

    public ResponseEntity<ApiResponse<?>> bulkDeleteImages(List<String> obfuscatedIds) {
        log.info("Deleting {} itinerary images", obfuscatedIds != null ? obfuscatedIds.size() : 0);
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
                    imageIds.add(idObfuscator.decodeId(obfuscatedId));
                } catch (Exception e) {
                    log.warn("Failed to decode itinerary image ID: {}", obfuscatedId, e);
                    notFoundIds.add(obfuscatedId);
                }
            }

            for (Long imageId : imageIds) {
                ItineraryImage image = itineraryImageRepository.findById(imageId).orElse(null);
                if (image != null) {
                    ItineraryImageDeleteService proxy = (ItineraryImageDeleteService) AopContext.currentProxy();
                    proxy.deleteSingleImage(imageId, image.getFileName());
                    deletedCount++;
                } else {
                    notFoundIds.add(idObfuscator.encodeId(imageId));
                }
            }

            if (deletedCount > 0) {
                return ResponseEntity.ok().body(
                    ApiResponse.success(200, deletedCount + " itinerary image(s) deleted successfully", null)
                );
            } else {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No itinerary images were deleted. " + notFoundIds.size() + " image(s) not found", "NO_IMAGES_DELETED")
                );
            }
        } catch (Exception e) {
            log.error("Error deleting itinerary images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete itinerary images", "ITINERARY_IMAGE_DELETE_FAILED")
            );
        }
    }

    @AuditLogAnnotation(
        action = "DELETE_ITINERARY_IMAGE",
        description = "Deleting itinerary image",
        entityType = "ItineraryImage",
        entityIdParamName = "id"
    )
    public void deleteSingleImage(Long id, String fileName) {
        log.info("Deleting itinerary image with ID: {}", id);
        itineraryImageRepository.deleteById(id);
        boolean fileDeleted = storageService.deleteImage(fileName);
        if (!fileDeleted) {
            log.warn("Image record deleted but file not found on disk: {}", fileName);
        }
    }
}
