package com.itineraryledger.kabengosafaris.Hero.Services.HeroImageServices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Hero.DTOs.HeroImageDTOs.ReorderHeroImagesDTO;
import com.itineraryledger.kabengosafaris.Hero.DTOs.HeroImageDTOs.HeroImageDTO;
import com.itineraryledger.kabengosafaris.Hero.DTOs.HeroImageDTOs.ReorderHeroImagesDTO.ImageOrderItem;
import com.itineraryledger.kabengosafaris.Hero.Entity.HeroImage;
import com.itineraryledger.kabengosafaris.Hero.Repository.HeroImageRepository;
import com.itineraryledger.kabengosafaris.Hero.Repository.HeroRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * HeroImageReorderService - Service for reordering hero images
 *
 * Handles drag-and-drop reordering from the UI with comprehensive validations:
 * - Validates all image IDs exist and belong to the hero
 * - Validates no duplicate image IDs
 * - Validates all images are included (no missing images)
 * - Validates expected display orders if provided
 * - Updates display orders
 */
@Service
@Slf4j
@Transactional
public class HeroImageReorderService {

    private final HeroRepository heroRepository;
    private final HeroImageRepository imageRepository;
    private final HeroImageGetService getService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public HeroImageReorderService(
        HeroRepository heroRepository,
        HeroImageRepository imageRepository,
        HeroImageGetService getService,
        IdObfuscator idObfuscator
    ) {
        this.heroRepository = heroRepository;
        this.imageRepository = imageRepository;
        this.getService = getService;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Reorder hero images based on the new order provided
     *
     * @param reorderDTO The reorder data containing the hero ID and new image order
     * @return ResponseEntity with ApiResponse containing the reordered images
     */
    @AuditLogAnnotation(
        action = "REORDER_HERO_IMAGES",
        description = "Reordering hero images",
        entityType = "HeroImage"
    )
    public ResponseEntity<ApiResponse<?>> reorderImages(ReorderHeroImagesDTO reorderDTO) {
        log.info("Reordering images for hero: {}", reorderDTO.getHeroId());

        try {
            // ========================
            // DECODE HERO ID
            // ========================
            Long heroId;
            try {
                heroId = idObfuscator.decodeId(reorderDTO.getHeroId());
            } catch (Exception e) {
                log.warn("Invalid hero ID format: {}", reorderDTO.getHeroId());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid hero ID format", "INVALID_HERO_ID")
                );
            }

            // ========================
            // VERIFY HERO EXISTS
            // ========================
            if (!heroRepository.existsById(heroId)) {
                log.warn("Hero not found: {}", heroId);
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Hero not found", "HERO_NOT_FOUND")
                );
            }

            // ========================
            // FETCH EXISTING IMAGES
            // ========================
            List<HeroImage> existingImages = imageRepository.findByHeroId(heroId);

            if (existingImages.isEmpty()) {
                log.warn("No images found for hero: {}", heroId);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Hero has no images to reorder", "NO_IMAGES_TO_REORDER")
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
            // VALIDATION: All images belong to this hero
            // ========================
            Set<Long> existingImageIds = existingImages.stream()
                .map(HeroImage::getId)
                .collect(Collectors.toSet());

            Set<Long> providedImageIds = decodedImageIds.keySet();

            // Check for images that don't belong to this hero
            Set<Long> foreignImages = new HashSet<>(providedImageIds);
            foreignImages.removeAll(existingImageIds);

            if (!foreignImages.isEmpty()) {
                List<String> foreignImageObfuscated = foreignImages.stream()
                    .map(id -> idObfuscator.encodeId(id))
                    .collect(Collectors.toList());
                log.warn("Image IDs not belonging to hero: {}", foreignImageObfuscated);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Image ID(s) do not belong to this hero: " + String.join(", ", foreignImageObfuscated),
                        "IMAGE_HERO_MISMATCH"
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
            Map<Long, HeroImage> imageLookup = existingImages.stream()
                .collect(Collectors.toMap(HeroImage::getId, image -> image));

            // ========================
            // CHECK IF REORDER IS ACTUALLY NEEDED
            // ========================
            boolean orderChanged = false;
            int newDisplayOrder = 1;

            for (Long imageId : decodedImageIds.keySet()) {
                HeroImage image = imageLookup.get(imageId);
                if (!image.getDisplayOrder().equals(newDisplayOrder)) {
                    orderChanged = true;
                    break;
                }
                newDisplayOrder++;
            }

            if (!orderChanged) {
                log.info("Image order unchanged for hero: {}", heroId);
                List<HeroImageDTO> resultDTOs = existingImages.stream()
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
            for (HeroImage image : existingImages) {
                image.setDisplayOrder(tempOrder--);
            }
            imageRepository.saveAll(existingImages);
            imageRepository.flush();

            // Pass 2: Set final display orders based on new order
            List<HeroImage> reorderedImages = new ArrayList<>();
            newDisplayOrder = 1;

            for (Long imageId : decodedImageIds.keySet()) {
                HeroImage image = imageLookup.get(imageId);
                image.setDisplayOrder(newDisplayOrder);
                reorderedImages.add(image);
                newDisplayOrder++;
            }

            // Save all reordered images
            reorderedImages = imageRepository.saveAll(reorderedImages);

            // ========================
            // CONVERT TO DTOs AND RETURN
            // ========================
            List<HeroImageDTO> resultDTOs = reorderedImages.stream()
                .sorted(Comparator.comparing(HeroImage::getDisplayOrder))
                .map(getService::toDTO)
                .collect(Collectors.toList());

            log.info("Successfully reordered {} images for hero: {}", reorderedImages.size(), heroId);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Images reordered successfully", resultDTOs)
            );

        } catch (Exception e) {
            log.error("Error reordering hero images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to reorder hero images", "HERO_IMAGES_REORDER_FAILED")
            );
        }
    }
}
