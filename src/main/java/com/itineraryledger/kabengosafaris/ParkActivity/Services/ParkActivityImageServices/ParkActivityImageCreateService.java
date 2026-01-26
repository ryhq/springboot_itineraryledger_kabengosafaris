package com.itineraryledger.kabengosafaris.ParkActivity.Services.ParkActivityImageServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivity;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivityId;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivityRepository;
import com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityImageDTOs.CreateParkActivityImageDTO;
import com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityImageDTOs.ParkActivityImageDTO;
import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityImage;
import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityImage.ImageType;
import com.itineraryledger.kabengosafaris.ParkActivity.Repositories.ParkActivityImageRepository;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for creating park activity images.
 */
@Service
@Slf4j
@Transactional
public class ParkActivityImageCreateService {

    private final ParkActivityImageRepository parkActivityImageRepository;
    private final ParkActivityRepository parkActivityRepository;
    private final ParkRepository parkRepository;
    private final ActivityRepository activityRepository;
    private final ParkActivityImageStorageService storageService;
    private final ParkActivityImageGetService getService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ParkActivityImageCreateService(
        ParkActivityImageRepository parkActivityImageRepository,
        ParkActivityRepository parkActivityRepository,
        ParkRepository parkRepository,
        ActivityRepository activityRepository,
        ParkActivityImageStorageService storageService,
        ParkActivityImageGetService getService,
        IdObfuscator idObfuscator
    ) {
        this.parkActivityImageRepository = parkActivityImageRepository;
        this.parkActivityRepository = parkActivityRepository;
        this.parkRepository = parkRepository;
        this.activityRepository = activityRepository;
        this.storageService = storageService;
        this.getService = getService;
        this.idObfuscator = idObfuscator;
    }

    @AuditLogAnnotation(
        action = "CREATE_PARK_ACTIVITY_IMAGES",
        description = "Uploading park activity images",
        entityType = "ParkActivityImage"
    )
    public ResponseEntity<ApiResponse<?>> uploadImages(List<CreateParkActivityImageDTO> imageDTOs) {
        log.info("Uploading {} park activity images", imageDTOs != null ? imageDTOs.size() : 0);

        try {
            if (imageDTOs == null || imageDTOs.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No images provided", "NO_IMAGES_PROVIDED")
                );
            }

            // Validate total request size
            long totalSize = 0;
            for (CreateParkActivityImageDTO dto : imageDTOs) {
                if (dto.getImage() != null) {
                    totalSize += dto.getImage().getSize();
                }
            }

            String requestSizeError = storageService.validateRequestSize(totalSize);
            if (requestSizeError != null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, requestSizeError, "REQUEST_SIZE_EXCEEDED")
                );
            }

            // Validate each image
            List<String> validationErrors = new ArrayList<>();
            for (int i = 0; i < imageDTOs.size(); i++) {
                CreateParkActivityImageDTO dto = imageDTOs.get(i);

                if (dto.getParkId() == null || dto.getParkId().isBlank()) {
                    validationErrors.add(String.format("Image %d: Park ID is required", i + 1));
                    continue;
                }

                if (dto.getActivityId() == null || dto.getActivityId().isBlank()) {
                    validationErrors.add(String.format("Image %d: Activity ID is required", i + 1));
                    continue;
                }

                if (dto.getImage() == null || dto.getImage().isEmpty()) {
                    validationErrors.add(String.format("Image %d: Image file is required", i + 1));
                    continue;
                }

                String imageError = storageService.validateImage(dto.getImage());
                if (imageError != null) {
                    String filename = dto.getImage().getOriginalFilename() != null
                        ? dto.getImage().getOriginalFilename()
                        : "unknown";
                    validationErrors.add(String.format("Image %d (%s): %s", i + 1, filename, imageError));
                }

                // Validate park-activity relationship exists
                try {
                    Long parkId = idObfuscator.decodeId(dto.getParkId());
                    Long activityId = idObfuscator.decodeId(dto.getActivityId());

                    Park park = parkRepository.findById(parkId).orElse(null);
                    Activity activity = activityRepository.findById(activityId).orElse(null);

                    if (park == null) {
                        validationErrors.add(String.format("Image %d: Park not found", i + 1));
                        continue;
                    }

                    if (activity == null) {
                        validationErrors.add(String.format("Image %d: Activity not found", i + 1));
                        continue;
                    }

                    // Check if ParkActivity relationship exists
                    ParkActivityId parkActivityId = new ParkActivityId(park, activity);
                    if (!parkActivityRepository.existsById(parkActivityId)) {
                        validationErrors.add(String.format("Image %d: Park-Activity relationship does not exist. The activity must be associated with the park first.", i + 1));
                    }
                } catch (Exception e) {
                    validationErrors.add(String.format("Image %d: Invalid Park or Activity ID", i + 1));
                }
            }

            if (!validationErrors.isEmpty()) {
                String errorMessage = "Validation failed: " + String.join("; ", validationErrors);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, errorMessage, "VALIDATION_ERROR")
                );
            }

            // Save images
            List<ParkActivityImageDTO> createdImages = new ArrayList<>();
            List<String> savedFileNames = new ArrayList<>();
            Map<String, Integer> displayOrderMap = new HashMap<>(); // key: parkId_activityId

            try {
                for (CreateParkActivityImageDTO dto : imageDTOs) {
                    Long parkId = idObfuscator.decodeId(dto.getParkId());
                    Long activityId = idObfuscator.decodeId(dto.getActivityId());

                    Park park = parkRepository.findById(parkId).orElse(null);
                    Activity activity = activityRepository.findById(activityId).orElse(null);

                    if (park == null || activity == null) {
                        continue;
                    }

                    ParkActivityId parkActivityId = new ParkActivityId(park, activity);
                    ParkActivity parkActivity = parkActivityRepository.findById(parkActivityId).orElse(null);

                    if (parkActivity == null) {
                        continue;
                    }

                    // Calculate display order
                    String orderKey = parkId + "_" + activityId;
                    int displayOrder;
                    if (displayOrderMap.containsKey(orderKey)) {
                        displayOrder = displayOrderMap.get(orderKey) + 1;
                    } else {
                        displayOrder = parkActivityImageRepository.findMaxDisplayOrderByParkActivity(parkId, activityId) + 1;
                    }
                    displayOrderMap.put(orderKey, displayOrder);

                    // Save file
                    MultipartFile file = dto.getImage();
                    String savedFileName = storageService.saveImage(file);
                    if (savedFileName == null) {
                        rollbackSavedFiles(savedFileNames);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            ApiResponse.error(500, "Failed to save image file: " + file.getOriginalFilename(), "STORAGE_ERROR")
                        );
                    }
                    savedFileNames.add(savedFileName);

                    // Create entity
                    ParkActivityImage image = ParkActivityImage.builder()
                        .parkActivity(parkActivity)
                        .fileName(savedFileName)
                        .originalFileName(file.getOriginalFilename())
                        .imageType(dto.getImageType() != null ? dto.getImageType() : ImageType.OTHER)
                        .altText(dto.getAltText())
                        .caption(dto.getCaption())
                        .description(dto.getDescription())
                        .isPrimary(false)
                        .isActive(true)
                        .displayOrder(displayOrder)
                        .fileSize(file.getSize())
                        .mimeType(storageService.getMimeType(savedFileName))
                        .build();

                    image = parkActivityImageRepository.save(image);
                    createdImages.add(getService.toDTO(image));
                }

                log.info("{} park activity images created successfully", createdImages.size());

                return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(201, createdImages.size() + " image(s) uploaded successfully", createdImages)
                );

            } catch (Exception e) {
                rollbackSavedFiles(savedFileNames);
                log.error("Failed to create park activity image records, rolled back {} files", savedFileNames.size(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to create image records", "DATABASE_ERROR")
                );
            }

        } catch (Exception e) {
            log.error("Error uploading park activity images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to upload park activity images", "PARK_ACTIVITY_IMAGE_UPLOAD_FAILED")
            );
        }
    }

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
