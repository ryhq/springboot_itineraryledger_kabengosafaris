package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationImageServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationImageDTOs.AccommodationImageDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationImageDTOs.UpdateAccommodationImageDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage.ImageType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationImageRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for creating and updating accommodation images.
 *
 * Provides:
 * - Bulk upload images for an accommodation
 * - Update image metadata
 */
@Service
@Slf4j
public class AccommodationImageCreateService {

    @Autowired
    private AccommodationImageRepository accommodationImageRepository;

    @Autowired
    private AccommodationRepository accommodationRepository;

    @Autowired
    private AccommodationImageStorageService storageService;

    @Autowired
    private AccommodationImageGetService getService;

    @Autowired
    private IdObfuscator idObfuscator;

    /**
     * Upload multiple images for an accommodation
     *
     * @param obfuscatedAccommodationId The accommodation ID (obfuscated)
     * @param files List of image files to upload
     * @param imageType Common image type for all uploads (optional, defaults to OTHER)
     * @return Response with list of created image DTOs
     */
    @Transactional
    @AuditLogAnnotation(
        action = "CREATE_ACCOMMODATION_IMAGES",
        description = "Uploading accommodation images",
        entityType = "AccommodationImage"
    )
    public ResponseEntity<?> uploadImages(
            String obfuscatedAccommodationId,
            List<MultipartFile> files,
            ImageType imageType
    ) {
        // Validate files provided
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "No files provided.", "VALIDATION_ERROR")
            );
        }

        // Decode accommodation ID
        Long accommodationId;
        try {
            accommodationId = idObfuscator.decodeId(obfuscatedAccommodationId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid Accommodation ID provided.", "VALIDATION_ERROR")
            );
        }

        // Check accommodation exists
        Accommodation accommodation = accommodationRepository.findById(accommodationId).orElse(null);
        if (accommodation == null) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(404, "Accommodation not found.", "NOT_FOUND")
            );
        }

        // Validate all files first before saving any
        List<String> validationErrors = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String error = storageService.validateImage(file);
            if (error != null) {
                validationErrors.add(String.format("File %d (%s): %s",
                    i + 1,
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown",
                    error
                ));
            }
        }

        if (!validationErrors.isEmpty()) {
            String errorMessage = "Validation failed: " + String.join("; ", validationErrors);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, errorMessage, "VALIDATION_ERROR")
            );
        }

        // Get starting display order
        int displayOrder = accommodationImageRepository.findMaxDisplayOrderByAccommodationId(accommodationId) + 1;

        // Process each file
        List<AccommodationImageDTO> createdImages = new ArrayList<>();
        List<String> savedFileNames = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                // Save image file to storage
                String savedFileName = storageService.saveImage(file);
                if (savedFileName == null) {
                    // Rollback previously saved files
                    rollbackSavedFiles(savedFileNames);
                    return ResponseEntity.status(500).body(
                        ApiResponse.error(500, "Failed to save image file: " + file.getOriginalFilename(), "STORAGE_ERROR")
                    );
                }
                savedFileNames.add(savedFileName);

                // Create entity
                AccommodationImage image = AccommodationImage.builder()
                    .accommodation(accommodation)
                    .fileName(savedFileName)
                    .originalFileName(file.getOriginalFilename())
                    .imageType(imageType != null ? imageType : ImageType.OTHER)
                    .isPrimary(false)
                    .isActive(true)
                    .displayOrder(displayOrder++)
                    .fileSize(file.getSize())
                    .mimeType(storageService.getMimeType(savedFileName))
                    .build();

                image = accommodationImageRepository.save(image);
                createdImages.add(getService.toDTO(image));
            }

            log.info("{} accommodation images created for accommodation {}",
                createdImages.size(), accommodation.getName());

            return ResponseEntity.status(201).body(
                ApiResponse.success(201,
                    String.format("%d image(s) uploaded successfully.", createdImages.size()),
                    createdImages)
            );

        } catch (Exception e) {
            // Rollback: delete saved files if database operations fail
            rollbackSavedFiles(savedFileNames);
            log.error("Failed to create accommodation image records, rolled back {} files",
                savedFileNames.size(), e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to create image records.", "DATABASE_ERROR")
            );
        }
    }

    /**
     * Update accommodation image metadata
     */
    @Transactional
    @AuditLogAnnotation(
        action = "UPDATE_ACCOMMODATION_IMAGE",
        description = "Updating accommodation image metadata",
        entityIdParamName = "obfuscatedId",
        entityType = "AccommodationImage"
    )
    public ResponseEntity<?> updateImage(String obfuscatedId, UpdateAccommodationImageDTO updateDTO) {
        // Decode image ID
        Long id;
        try {
            id = idObfuscator.decodeId(obfuscatedId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid Image ID provided.", "VALIDATION_ERROR")
            );
        }

        // Find image
        AccommodationImage image = accommodationImageRepository.findById(id).orElse(null);
        if (image == null) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(404, "Image not found.", "NOT_FOUND")
            );
        }

        // Track if any changes made
        boolean changed = false;

        // Update fields if provided
        if (updateDTO.getImageType() != null && !updateDTO.getImageType().equals(image.getImageType())) {
            image.setImageType(updateDTO.getImageType());
            changed = true;
        }

        if (updateDTO.getAltText() != null) {
            image.setAltText(updateDTO.getAltText());
            changed = true;
        }

        if (updateDTO.getCaption() != null) {
            image.setCaption(updateDTO.getCaption());
            changed = true;
        }

        if (updateDTO.getDescription() != null) {
            image.setDescription(updateDTO.getDescription());
            changed = true;
        }

        if (updateDTO.getIsActive() != null && !updateDTO.getIsActive().equals(image.getIsActive())) {
            image.setIsActive(updateDTO.getIsActive());
            changed = true;
        }

        if (updateDTO.getDisplayOrder() != null && !updateDTO.getDisplayOrder().equals(image.getDisplayOrder())) {
            image.setDisplayOrder(updateDTO.getDisplayOrder());
            changed = true;
        }

        // Handle primary flag change
        if (updateDTO.getIsPrimary() != null && !updateDTO.getIsPrimary().equals(image.getIsPrimary())) {
            if (Boolean.TRUE.equals(updateDTO.getIsPrimary())) {
                // Unset other primary images for this accommodation
                accommodationImageRepository.unsetPrimaryForAccommodation(image.getAccommodation().getId());
            }
            image.setIsPrimary(updateDTO.getIsPrimary());
            changed = true;
        }

        if (!changed) {
            return ResponseEntity.ok(
                ApiResponse.success(200, "No changes detected. Image remains unchanged.", getService.toDTO(image))
            );
        }

        image = accommodationImageRepository.save(image);
        log.info("Accommodation image updated: {}", image.getFileName());

        return ResponseEntity.ok(
            ApiResponse.success(200, "Accommodation image updated successfully.", getService.toDTO(image))
        );
    }

    /**
     * Rollback saved files on error
     */
    private void rollbackSavedFiles(List<String> fileNames) {
        for (String fileName : fileNames) {
            try {
                storageService.deleteImage(fileName);
                log.debug("Rolled back file: {}", fileName);
            } catch (Exception e) {
                log.warn("Failed to rollback file: {}", fileName, e);
            }
        }
    }
}
