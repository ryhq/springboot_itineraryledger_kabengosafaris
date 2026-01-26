package com.itineraryledger.kabengosafaris.Park.Services.ParkImageServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Park.DTOs.ParkImageDTOs.ParkImageDTO;
import com.itineraryledger.kabengosafaris.Park.DTOs.ParkImageDTOs.ReorderParkImagesDTO;
import com.itineraryledger.kabengosafaris.Park.Entities.ParkImage;
import com.itineraryledger.kabengosafaris.Park.Repositories.ParkImageRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.*;

/**
 * Service for reordering park images.
 */
@Service
@Slf4j
@Transactional
public class ParkImageReorderService {

    private final ParkImageRepository parkImageRepository;
    private final ParkImageGetService getService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ParkImageReorderService(
        ParkImageRepository parkImageRepository,
        ParkImageGetService getService,
        IdObfuscator idObfuscator
    ) {
        this.parkImageRepository = parkImageRepository;
        this.getService = getService;
        this.idObfuscator = idObfuscator;
    }

    @AuditLogAnnotation(
        action = "REORDER_PARK_IMAGES",
        description = "Reordering park images",
        entityType = "ParkImage"
    )
    public ResponseEntity<ApiResponse<?>> reorderImages(ReorderParkImagesDTO reorderDTO) {
        log.info("Reordering park images for park: {}", reorderDTO.getParkId());

        try {
            Long parkId;
            try {
                parkId = idObfuscator.decodeId(reorderDTO.getParkId());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid park ID", "INVALID_PARK_ID")
                );
            }

            List<ParkImage> existingImages = parkImageRepository.findByParkIdOrderByDisplayOrderAsc(parkId);
            if (existingImages.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No images found for this park", "NO_IMAGES_TO_REORDER")
                );
            }

            List<ReorderParkImagesDTO.ImageOrderItem> orderItems = reorderDTO.getImageOrder();
            if (orderItems.size() != existingImages.size()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("Image order list must contain exactly %d images. Received: %d",
                            existingImages.size(), orderItems.size()),
                        "IMAGE_COUNT_MISMATCH")
                );
            }

            Map<Long, ParkImage> imageMap = new HashMap<>();
            for (ParkImage img : existingImages) {
                imageMap.put(img.getId(), img);
            }

            Set<Long> providedIds = new HashSet<>();
            List<String> errors = new ArrayList<>();

            for (int i = 0; i < orderItems.size(); i++) {
                ReorderParkImagesDTO.ImageOrderItem item = orderItems.get(i);
                try {
                    Long imageId = idObfuscator.decodeId(item.getImageId());

                    if (!imageMap.containsKey(imageId)) {
                        errors.add(String.format("Image %s does not belong to this park", item.getImageId()));
                        continue;
                    }

                    if (providedIds.contains(imageId)) {
                        errors.add(String.format("Duplicate image ID: %s", item.getImageId()));
                        continue;
                    }

                    providedIds.add(imageId);

                    if (item.getExpectedDisplayOrder() != null && item.getExpectedDisplayOrder() != (i + 1)) {
                        errors.add(String.format("Image %s: expected order %d but position is %d",
                            item.getImageId(), item.getExpectedDisplayOrder(), i + 1));
                    }
                } catch (Exception e) {
                    errors.add(String.format("Invalid image ID format: %s", item.getImageId()));
                }
            }

            Set<Long> existingIds = imageMap.keySet();
            Set<Long> missingIds = new HashSet<>(existingIds);
            missingIds.removeAll(providedIds);
            if (!missingIds.isEmpty()) {
                List<String> missingObfuscatedIds = missingIds.stream()
                    .map(idObfuscator::encodeId)
                    .toList();
                errors.add("Missing image ID(s) in reorder list: " + String.join(", ", missingObfuscatedIds));
            }

            if (!errors.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, String.join("; ", errors), "VALIDATION_ERROR")
                );
            }

            List<ParkImage> reorderedImages = new ArrayList<>();
            for (int i = 0; i < orderItems.size(); i++) {
                Long imageId = idObfuscator.decodeId(orderItems.get(i).getImageId());
                ParkImage image = imageMap.get(imageId);
                image.setDisplayOrder(i + 1);
                reorderedImages.add(image);
            }

            parkImageRepository.saveAll(reorderedImages);

            List<ParkImageDTO> result = reorderedImages.stream()
                .map(getService::toDTO)
                .toList();

            log.info("Successfully reordered {} images for park {}", result.size(), parkId);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Images reordered successfully", result)
            );

        } catch (Exception e) {
            log.error("Error reordering park images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to reorder park images", "PARK_IMAGES_REORDER_FAILED")
            );
        }
    }
}
