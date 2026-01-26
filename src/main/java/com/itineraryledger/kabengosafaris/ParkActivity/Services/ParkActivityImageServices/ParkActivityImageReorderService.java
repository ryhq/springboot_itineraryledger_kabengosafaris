package com.itineraryledger.kabengosafaris.ParkActivity.Services.ParkActivityImageServices;

import com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityImageDTOs.ParkActivityImageDTO;
import com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityImageDTOs.ReorderParkActivityImagesDTO;
import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityImage;
import com.itineraryledger.kabengosafaris.ParkActivity.Repositories.ParkActivityImageRepository;
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
 * Service for reordering park activity images.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ParkActivityImageReorderService {

    private final ParkActivityImageRepository parkActivityImageRepository;
    private final ParkActivityImageGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "REORDER_PARK_ACTIVITY_IMAGES",
        description = "Reordering park activity images",
        entityType = "ParkActivityImage"
    )
    public ResponseEntity<ApiResponse<?>> reorderImages(ReorderParkActivityImagesDTO reorderDTO) {
        log.info("Reordering images for park-activity: parkId={}, activityId={}",
            reorderDTO.getParkId(), reorderDTO.getActivityId());

        try {
            Long parkId;
            Long activityId;
            try {
                parkId = idObfuscator.decodeId(reorderDTO.getParkId());
                activityId = idObfuscator.decodeId(reorderDTO.getActivityId());
            } catch (Exception e) {
                log.warn("Failed to decode park or activity ID", e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid park or activity ID", "INVALID_ID")
                );
            }

            // Get existing images for this park-activity
            List<ParkActivityImage> existingImages = parkActivityImageRepository.findByParkActivityOrderByDisplayOrderAsc(parkId, activityId);

            if (existingImages.isEmpty()) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "No images found for this park-activity", "NO_IMAGES_FOUND")
                );
            }

            // Create a map of existing images by ID
            Map<Long, ParkActivityImage> imageMap = existingImages.stream()
                .collect(Collectors.toMap(ParkActivityImage::getId, img -> img));

            // Validate and decode the new order
            List<Long> newOrderIds = new ArrayList<>();
            for (String obfuscatedId : reorderDTO.getImageOrder()) {
                try {
                    Long imageId = idObfuscator.decodeId(obfuscatedId);
                    if (!imageMap.containsKey(imageId)) {
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(400, "Image ID " + obfuscatedId + " does not belong to this park-activity", "INVALID_IMAGE_ORDER")
                        );
                    }
                    newOrderIds.add(imageId);
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid image ID in order list: " + obfuscatedId, "INVALID_IMAGE_ID")
                    );
                }
            }

            // Ensure all images are included in the order
            if (newOrderIds.size() != existingImages.size()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("Image order list must contain all %d images for this park-activity", existingImages.size()),
                        "INCOMPLETE_IMAGE_ORDER")
                );
            }

            // Update display order
            List<ParkActivityImage> updatedImages = new ArrayList<>();
            for (int i = 0; i < newOrderIds.size(); i++) {
                ParkActivityImage image = imageMap.get(newOrderIds.get(i));
                image.setDisplayOrder(i + 1);
                updatedImages.add(parkActivityImageRepository.save(image));
            }

            List<ParkActivityImageDTO> imageDTOs = updatedImages.stream()
                .map(getService::toDTO)
                .toList();

            log.info("Park activity images reordered successfully for park-activity: parkId={}, activityId={}", parkId, activityId);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Images reordered successfully", imageDTOs)
            );

        } catch (Exception e) {
            log.error("Error reordering park activity images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to reorder park activity images", "REORDER_FAILED")
            );
        }
    }
}
