package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationImageServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationImageDTOs.AccommodationImageDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationImageDTOs.CreateAccommodationImageDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage.ImageType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationImageRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for creating accommodation images.
 *
 * Provides:
 * - Bulk upload images using list of CreateAccommodationImageDTO
 */
@Service
@Slf4j
@Transactional
public class AccommodationImageCreateService {

    private final AccommodationImageRepository accommodationImageRepository;
    private final AccommodationRepository accommodationRepository;
    private final AccommodationImageStorageService storageService;
    private final AccommodationImageGetService getService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public AccommodationImageCreateService(
        AccommodationImageRepository accommodationImageRepository,
        AccommodationRepository accommodationRepository,
        AccommodationImageStorageService storageService,
        AccommodationImageGetService getService,
        IdObfuscator idObfuscator
    ) {
        this.accommodationImageRepository = accommodationImageRepository;
        this.accommodationRepository = accommodationRepository;
        this.storageService = storageService;
        this.getService = getService;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Upload multiple images for accommodations
     *
     * @param imageDTOs List of CreateAccommodationImageDTO containing image files and metadata
     * @return ResponseEntity with ApiResponse containing success or error
     */
    @AuditLogAnnotation(
        action = "CREATE_ACCOMMODATION_IMAGES",
        description = "Uploading accommodation images",
        entityType = "AccommodationImage"
    )
    public ResponseEntity<ApiResponse<?>> uploadImages(List<CreateAccommodationImageDTO> imageDTOs) {
        log.info("Uploading {} accommodation images", imageDTOs != null ? imageDTOs.size() : 0);

        try {
            // Validate input
            if (imageDTOs == null || imageDTOs.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "No images provided",
                        "NO_IMAGES_PROVIDED"
                    )
                );
            }

            // Calculate total request size
            long totalSize = 0;
            for (CreateAccommodationImageDTO dto : imageDTOs) {
                if (dto.getImage() != null) {
                    totalSize += dto.getImage().getSize();
                }
            }

            // Validate total request size
            String requestSizeError = storageService.validateRequestSize(totalSize);
            if (requestSizeError != null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        requestSizeError,
                        "REQUEST_SIZE_EXCEEDED"
                    )
                );
            }

            // Validate all images first
            List<String> validationErrors = new ArrayList<>();
            for (int i = 0; i < imageDTOs.size(); i++) {
                CreateAccommodationImageDTO dto = imageDTOs.get(i);

                // Validate accommodation ID
                if (dto.getAccommodationId() == null || dto.getAccommodationId().isBlank()) {
                    validationErrors.add(String.format("Image %d: Accommodation ID is required", i + 1));
                    continue;
                }

                // Validate image file
                if (dto.getImage() == null || dto.getImage().isEmpty()) {
                    validationErrors.add(String.format("Image %d: Image file is required", i + 1));
                    continue;
                }

                // Validate image file (size and format)
                String imageError = storageService.validateImage(dto.getImage());
                if (imageError != null) {
                    String filename = dto.getImage().getOriginalFilename() != null
                        ? dto.getImage().getOriginalFilename()
                        : "unknown";
                    validationErrors.add(String.format("Image %d (%s): %s", i + 1, filename, imageError));
                }

                // Validate accommodation exists
                try {
                    Long accommodationId = idObfuscator.decodeId(dto.getAccommodationId());
                    if (!accommodationRepository.existsById(accommodationId)) {
                        validationErrors.add(String.format("Image %d: Accommodation not found", i + 1));
                    }
                } catch (Exception e) {
                    validationErrors.add(String.format("Image %d: Invalid Accommodation ID", i + 1));
                }
            }

            if (!validationErrors.isEmpty()) {
                String errorMessage = "Validation failed: " + String.join("; ", validationErrors);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        errorMessage,
                        "VALIDATION_ERROR"
                    )
                );
            }

            // Process each image
            List<AccommodationImageDTO> createdImages = new ArrayList<>();
            List<String> savedFileNames = new ArrayList<>();
            Map<Long, Integer> displayOrderMap = new HashMap<>();

            try {
                for (CreateAccommodationImageDTO dto : imageDTOs) {
                    Long accommodationId = idObfuscator.decodeId(dto.getAccommodationId());
                    Accommodation accommodation = accommodationRepository.findById(accommodationId).orElse(null);

                    if (accommodation == null) {
                        continue; // Already validated, skip if somehow null
                    }

                    // Get or calculate display order for this accommodation
                    int displayOrder;
                    if (displayOrderMap.containsKey(accommodationId)) {
                        displayOrder = displayOrderMap.get(accommodationId) + 1;
                    } else {
                        displayOrder = accommodationImageRepository.findMaxDisplayOrderByAccommodationId(accommodationId) + 1;
                    }
                    displayOrderMap.put(accommodationId, displayOrder);

                    // Save image file to storage (with SHA-256 hashed filename)
                    MultipartFile file = dto.getImage();
                    String savedFileName = storageService.saveImage(file);
                    if (savedFileName == null) {
                        // Rollback previously saved files
                        rollbackSavedFiles(savedFileNames);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            ApiResponse.error(
                                500,
                                "Failed to save image file: " + file.getOriginalFilename(),
                                "STORAGE_ERROR"
                            )
                        );
                    }
                    savedFileNames.add(savedFileName);

                    // Create entity
                    AccommodationImage image = AccommodationImage.builder()
                        .accommodation(accommodation)
                        .fileName(savedFileName)
                        .originalFileName(file.getOriginalFilename())
                        .imageType(dto.getImageType() != null ? dto.getImageType() : ImageType.OTHER)
                        .altText(dto.getAltText())
                        .caption(dto.getCaption())
                        .description(dto.getDescription())
                        .isPrimary(false)
                        .isActive(true)
                        .isWebActive(true)
                        .displayOrder(displayOrder)
                        .fileSize(file.getSize())
                        .mimeType(storageService.getMimeType(savedFileName))
                        .build();

                    image = accommodationImageRepository.save(image);
                    createdImages.add(getService.toDTO(image));
                }

                log.info("{} accommodation images created successfully", createdImages.size());

                return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(
                        201,
                        createdImages.size() + " image(s) uploaded successfully",
                        createdImages
                    )
                );

            } catch (Exception e) {
                // Rollback: delete saved files if database operations fail
                rollbackSavedFiles(savedFileNames);
                log.error("Failed to create accommodation image records, rolled back {} files",
                    savedFileNames.size(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(
                        500,
                        "Failed to create image records",
                        "DATABASE_ERROR"
                    )
                );
            }

        } catch (Exception e) {
            log.error("Error uploading accommodation images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to upload accommodation images",
                    "ACCOMMODATION_IMAGE_UPLOAD_FAILED"
                )
            );
        }
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
