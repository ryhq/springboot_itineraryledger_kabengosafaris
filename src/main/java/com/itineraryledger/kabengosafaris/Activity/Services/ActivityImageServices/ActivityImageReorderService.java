package com.itineraryledger.kabengosafaris.Activity.Services.ActivityImageServices;

import com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityImageDTOs.ActivityImageDTO;
import com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityImageDTOs.ReorderActivityImagesDTO;
import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityImage;
import com.itineraryledger.kabengosafaris.Activity.Repositories.ActivityImageRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for reordering activity images.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ActivityImageReorderService {

    private final ActivityImageRepository activityImageRepository;
    private final ActivityImageGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "REORDER_ACTIVITY_IMAGES",
        description = "Reordering activity images",
        entityType = "ActivityImage"
    )
    public ResponseEntity<ApiResponse<?>> reorderImages(ReorderActivityImagesDTO reorderDTO) {
        log.info("Reordering images for activity: {}", reorderDTO.getActivityId());

        try {
            Long activityId;
            try {
                activityId = idObfuscator.decodeId(reorderDTO.getActivityId());
            } catch (Exception e) {
                log.warn("Failed to decode activity ID: {}", reorderDTO.getActivityId(), e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid activity ID", "INVALID_ACTIVITY_ID")
                );
            }

            List<ActivityImage> existingImages = activityImageRepository.findByActivityIdOrderByDisplayOrderAsc(activityId);

            if (existingImages.isEmpty()) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "No images found for this activity", "NO_IMAGES_FOUND")
                );
            }

            Map<Long, ActivityImage> imageMap = existingImages.stream()
                .collect(Collectors.toMap(ActivityImage::getId, img -> img));

            List<Long> newOrderIds = new ArrayList<>();
            for (String obfuscatedId : reorderDTO.getImageOrder()) {
                try {
                    Long imageId = idObfuscator.decodeId(obfuscatedId);
                    if (!imageMap.containsKey(imageId)) {
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(400, "Image ID " + obfuscatedId + " does not belong to this activity", "INVALID_IMAGE_ORDER")
                        );
                    }
                    newOrderIds.add(imageId);
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid image ID in order list: " + obfuscatedId, "INVALID_IMAGE_ID")
                    );
                }
            }

            if (newOrderIds.size() != existingImages.size()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("Image order list must contain all %d images for this activity", existingImages.size()),
                        "INCOMPLETE_IMAGE_ORDER")
                );
            }

            List<ActivityImage> updatedImages = new ArrayList<>();
            for (int i = 0; i < newOrderIds.size(); i++) {
                ActivityImage image = imageMap.get(newOrderIds.get(i));
                image.setDisplayOrder(i + 1);
                updatedImages.add(activityImageRepository.save(image));
            }

            List<ActivityImageDTO> imageDTOs = updatedImages.stream()
                .map(getService::toDTO)
                .toList();

            log.info("Activity images reordered successfully for activity: {}", activityId);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Images reordered successfully", imageDTOs)
            );

        } catch (Exception e) {
            log.error("Error reordering activity images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to reorder activity images", "REORDER_FAILED")
            );
        }
    }
}
