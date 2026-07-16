package com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.DTOs.ItineraryImageDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.DTOs.ReorderItineraryImagesDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Entity.ItineraryImage;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Repository.ItineraryImageRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.*;

/**
 * Service for reordering itinerary images.
 */
@Service
@Slf4j
@Transactional
public class ItineraryImageReorderService {

    private final ItineraryImageRepository itineraryImageRepository;
    private final ItineraryImageGetService getService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryImageReorderService(
        ItineraryImageRepository itineraryImageRepository,
        ItineraryImageGetService getService,
        IdObfuscator idObfuscator
    ) {
        this.itineraryImageRepository = itineraryImageRepository;
        this.getService = getService;
        this.idObfuscator = idObfuscator;
    }

    @AuditLogAnnotation(
        action = "REORDER_ITINERARY_IMAGES",
        description = "Reordering itinerary images",
        entityType = "ItineraryImage"
    )
    public ResponseEntity<ApiResponse<?>> reorderImages(ReorderItineraryImagesDTO reorderDTO) {
        log.info("Reordering itinerary images for itinerary: {}", reorderDTO.getItineraryId());
        try {
            Long itineraryId;
            try {
                itineraryId = idObfuscator.decodeId(reorderDTO.getItineraryId());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid itinerary ID", "INVALID_ITINERARY_ID")
                );
            }

            List<ItineraryImage> existingImages = itineraryImageRepository.findByItineraryIdOrderByDisplayOrderAsc(itineraryId);
            if (existingImages.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No images found for this itinerary", "NO_IMAGES_TO_REORDER")
                );
            }

            List<ReorderItineraryImagesDTO.ImageOrderItem> orderItems = reorderDTO.getImageOrder();
            if (orderItems.size() != existingImages.size()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("Image order list must contain exactly %d images. Received: %d",
                            existingImages.size(), orderItems.size()),
                        "IMAGE_COUNT_MISMATCH")
                );
            }

            Map<Long, ItineraryImage> imageMap = new HashMap<>();
            for (ItineraryImage img : existingImages) imageMap.put(img.getId(), img);

            Set<Long> providedIds = new HashSet<>();
            List<String> errors = new ArrayList<>();

            for (int i = 0; i < orderItems.size(); i++) {
                ReorderItineraryImagesDTO.ImageOrderItem item = orderItems.get(i);
                try {
                    Long imageId = idObfuscator.decodeId(item.getImageId());
                    if (!imageMap.containsKey(imageId)) {
                        errors.add(String.format("Image %s does not belong to this itinerary", item.getImageId()));
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

            Set<Long> missingIds = new HashSet<>(imageMap.keySet());
            missingIds.removeAll(providedIds);
            if (!missingIds.isEmpty()) {
                List<String> missingObfuscatedIds = missingIds.stream().map(idObfuscator::encodeId).toList();
                errors.add("Missing image ID(s) in reorder list: " + String.join(", ", missingObfuscatedIds));
            }

            if (!errors.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, String.join("; ", errors), "VALIDATION_ERROR")
                );
            }

            List<ItineraryImage> reorderedImages = new ArrayList<>();
            for (int i = 0; i < orderItems.size(); i++) {
                Long imageId = idObfuscator.decodeId(orderItems.get(i).getImageId());
                ItineraryImage image = imageMap.get(imageId);
                image.setDisplayOrder(i + 1);
                reorderedImages.add(image);
            }

            itineraryImageRepository.saveAll(reorderedImages);

            List<ItineraryImageDTO> result = reorderedImages.stream().map(getService::toDTO).toList();
            log.info("Successfully reordered {} images for itinerary {}", result.size(), itineraryId);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Images reordered successfully", result)
            );
        } catch (Exception e) {
            log.error("Error reordering itinerary images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to reorder itinerary images", "ITINERARY_IMAGES_REORDER_FAILED")
            );
        }
    }
}
