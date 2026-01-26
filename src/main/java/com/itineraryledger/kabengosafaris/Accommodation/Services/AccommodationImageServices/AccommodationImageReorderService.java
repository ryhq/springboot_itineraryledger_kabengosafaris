package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationImageServices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationImageDTOs.AccommodationImageDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationImageDTOs.ReorderAccommodationImagesDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationImageDTOs.ReorderAccommodationImagesDTO.ImageOrderItem;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationImageRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AccommodationImageReorderService - Service for reordering accommodation images
 *
 * Handles drag-and-drop reordering from the UI with comprehensive validations:
 * - Validates all image IDs exist and belong to the accommodation
 * - Validates no duplicate image IDs
 * - Validates all images are included (no missing images)
 * - Validates expected display orders if provided
 * - Updates display orders
 */
@Service
@Slf4j
@Transactional
public class AccommodationImageReorderService {

    private final AccommodationRepository accommodationRepository;
    private final AccommodationImageRepository imageRepository;
    private final AccommodationImageGetService getService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public AccommodationImageReorderService(
        AccommodationRepository accommodationRepository,
        AccommodationImageRepository imageRepository,
        AccommodationImageGetService getService,
        IdObfuscator idObfuscator
    ) {
        this.accommodationRepository = accommodationRepository;
        this.imageRepository = imageRepository;
        this.getService = getService;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Reorder accommodation images based on the new order provided
     *
     * @param reorderDTO The reorder data containing the accommodation ID and new image order
     * @return ResponseEntity with ApiResponse containing the reordered images
     */
    @AuditLogAnnotation(
        action = "REORDER_ACCOMMODATION_IMAGES",
        description = "Reordering accommodation images",
        entityType = "AccommodationImage"
    )
    public ResponseEntity<ApiResponse<?>> reorderImages(ReorderAccommodationImagesDTO reorderDTO) {
        log.info("Reordering images for accommodation: {}", reorderDTO.getAccommodationId());

        try {
            // ========================
            // DECODE ACCOMMODATION ID
            // ========================
            Long accommodationId;
            try {
                accommodationId = idObfuscator.decodeId(reorderDTO.getAccommodationId());
            } catch (Exception e) {
                log.warn("Invalid accommodation ID format: {}", reorderDTO.getAccommodationId());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid accommodation ID format", "INVALID_ACCOMMODATION_ID")
                );
            }

            // ========================
            // VERIFY ACCOMMODATION EXISTS
            // ========================
            if (!accommodationRepository.existsById(accommodationId)) {
                log.warn("Accommodation not found: {}", accommodationId);
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Accommodation not found", "ACCOMMODATION_NOT_FOUND")
                );
            }

            // ========================
            // FETCH EXISTING IMAGES
            // ========================
            List<AccommodationImage> existingImages = imageRepository.findByAccommodationIdOrderByDisplayOrderAsc(accommodationId);

            if (existingImages.isEmpty()) {
                log.warn("No images found for accommodation: {}", accommodationId);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Accommodation has no images to reorder", "NO_IMAGES_TO_REORDER")
                );
            }

            // ========================
            // VALIDATION: Check image order list size matches existing images
            // ========================
            List<ImageOrderItem> imageOrder = reorderDTO.getImageOrder();

            if (imageOrder.size() != existingImages.size()) {
                log.warn("Image order count mismatch. Expected: {}, Received: {}", existingImages.size(), imageOrder.size());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Image order list must contain exactly " + existingImages.size() + " images. Received: " + imageOrder.size(),
                        "IMAGE_COUNT_MISMATCH"
                    )
                );
            }

            // ========================
            // DECODE ALL IMAGE IDs AND VALIDATE FORMAT
            // ========================
            Map<Long, ImageOrderItem> decodedImageIds = new LinkedHashMap<>();
            List<String> invalidIds = new ArrayList<>();
            List<String> duplicateIds = new ArrayList<>();

            for (ImageOrderItem item : imageOrder) {
                if (item.getImageId() == null || item.getImageId().isBlank()) {
                    invalidIds.add("null/empty");
                    continue;
                }

                try {
                    Long decodedId = idObfuscator.decodeId(item.getImageId());

                    // Check for duplicates
                    if (decodedImageIds.containsKey(decodedId)) {
                        duplicateIds.add(item.getImageId());
                    } else {
                        decodedImageIds.put(decodedId, item);
                    }
                } catch (Exception e) {
                    invalidIds.add(item.getImageId());
                }
            }

            if (!invalidIds.isEmpty()) {
                log.warn("Invalid image ID formats: {}", invalidIds);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid image ID format(s): " + String.join(", ", invalidIds),
                        "INVALID_IMAGE_ID_FORMAT"
                    )
                );
            }

            if (!duplicateIds.isEmpty()) {
                log.warn("Duplicate image IDs in reorder list: {}", duplicateIds);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Duplicate image ID(s) in reorder list: " + String.join(", ", duplicateIds),
                        "DUPLICATE_IMAGE_IDS"
                    )
                );
            }

            // ========================
            // VALIDATION: All images belong to this accommodation
            // ========================
            Set<Long> existingImageIds = existingImages.stream()
                .map(AccommodationImage::getId)
                .collect(Collectors.toSet());

            Set<Long> providedImageIds = decodedImageIds.keySet();

            // Check for images that don't belong to this accommodation
            Set<Long> foreignImages = new HashSet<>(providedImageIds);
            foreignImages.removeAll(existingImageIds);

            if (!foreignImages.isEmpty()) {
                List<String> foreignImageObfuscated = foreignImages.stream()
                    .map(id -> idObfuscator.encodeId(id))
                    .collect(Collectors.toList());
                log.warn("Image IDs not belonging to accommodation: {}", foreignImageObfuscated);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Image ID(s) do not belong to this accommodation: " + String.join(", ", foreignImageObfuscated),
                        "IMAGE_ACCOMMODATION_MISMATCH"
                    )
                );
            }

            // Check for missing images
            Set<Long> missingImages = new HashSet<>(existingImageIds);
            missingImages.removeAll(providedImageIds);

            if (!missingImages.isEmpty()) {
                List<String> missingImageObfuscated = missingImages.stream()
                    .map(id -> idObfuscator.encodeId(id))
                    .collect(Collectors.toList());
                log.warn("Missing image IDs in reorder list: {}", missingImageObfuscated);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Missing image ID(s) in reorder list: " + String.join(", ", missingImageObfuscated),
                        "MISSING_IMAGE_IDS"
                    )
                );
            }

            // ========================
            // VALIDATION: Expected display orders (if provided)
            // ========================
            List<String> expectedOrderMismatches = new ArrayList<>();
            int position = 1;

            for (ImageOrderItem item : imageOrder) {
                if (item.getExpectedDisplayOrder() != null && !item.getExpectedDisplayOrder().equals(position)) {
                    expectedOrderMismatches.add(
                        String.format("Image %s: expected %d, but position is %d",
                            item.getImageId(), item.getExpectedDisplayOrder(), position)
                    );
                }
                position++;
            }

            if (!expectedOrderMismatches.isEmpty()) {
                log.warn("Expected display order mismatches: {}", expectedOrderMismatches);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Expected display order mismatches: " + String.join("; ", expectedOrderMismatches),
                        "EXPECTED_ORDER_MISMATCH"
                    )
                );
            }

            // ========================
            // CREATE IMAGE LOOKUP MAP
            // ========================
            Map<Long, AccommodationImage> imageLookup = existingImages.stream()
                .collect(Collectors.toMap(AccommodationImage::getId, image -> image));

            // ========================
            // CHECK IF REORDER IS ACTUALLY NEEDED
            // ========================
            boolean orderChanged = false;
            int newDisplayOrder = 1;

            for (Long imageId : decodedImageIds.keySet()) {
                AccommodationImage image = imageLookup.get(imageId);
                if (!image.getDisplayOrder().equals(newDisplayOrder)) {
                    orderChanged = true;
                    break;
                }
                newDisplayOrder++;
            }

            if (!orderChanged) {
                log.info("Image order unchanged for accommodation: {}", accommodationId);
                List<AccommodationImageDTO> resultDTOs = existingImages.stream()
                    .map(getService::toDTO)
                    .collect(Collectors.toList());

                return ResponseEntity.ok().body(
                    ApiResponse.success(200, "Image order unchanged", resultDTOs)
                );
            }

            // ========================
            // PERFORM REORDER
            // ========================
            log.info("Performing reorder for {} images", existingImages.size());

            // Pass 1: Set temporary negative display orders to avoid unique constraint violations (if any)
            int tempOrder = -1;
            for (AccommodationImage image : existingImages) {
                image.setDisplayOrder(tempOrder--);
            }
            imageRepository.saveAll(existingImages);
            imageRepository.flush();

            // Pass 2: Set final display orders based on new order
            List<AccommodationImage> reorderedImages = new ArrayList<>();
            newDisplayOrder = 1;

            for (Long imageId : decodedImageIds.keySet()) {
                AccommodationImage image = imageLookup.get(imageId);
                image.setDisplayOrder(newDisplayOrder);
                reorderedImages.add(image);
                newDisplayOrder++;
            }

            // Save all reordered images
            reorderedImages = imageRepository.saveAll(reorderedImages);

            // ========================
            // CONVERT TO DTOs AND RETURN
            // ========================
            List<AccommodationImageDTO> resultDTOs = reorderedImages.stream()
                .sorted(Comparator.comparing(AccommodationImage::getDisplayOrder))
                .map(getService::toDTO)
                .collect(Collectors.toList());

            log.info("Successfully reordered {} images for accommodation: {}", reorderedImages.size(), accommodationId);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Images reordered successfully", resultDTOs)
            );

        } catch (Exception e) {
            log.error("Error reordering accommodation images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to reorder accommodation images", "ACCOMMODATION_IMAGES_REORDER_FAILED")
            );
        }
    }
}
