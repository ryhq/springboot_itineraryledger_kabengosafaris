package com.itineraryledger.kabengosafaris.Activity.Services.ActivityImageServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityImageDTOs.CreateActivityImageDTO;
import com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityImageDTOs.ActivityImageDTO;
import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityImage;
import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityImage.ImageType;
import com.itineraryledger.kabengosafaris.Activity.Repositories.ActivityImageRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for creating activity images.
 */
@Service
@Slf4j
@Transactional
public class ActivityImageCreateService {

    private final ActivityImageRepository activityImageRepository;
    private final ActivityRepository activityRepository;
    private final ActivityImageStorageService storageService;
    private final ActivityImageGetService getService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ActivityImageCreateService(
        ActivityImageRepository activityImageRepository,
        ActivityRepository activityRepository,
        ActivityImageStorageService storageService,
        ActivityImageGetService getService,
        IdObfuscator idObfuscator
    ) {
        this.activityImageRepository = activityImageRepository;
        this.activityRepository = activityRepository;
        this.storageService = storageService;
        this.getService = getService;
        this.idObfuscator = idObfuscator;
    }

    @AuditLogAnnotation(
        action = "CREATE_ACTIVITY_IMAGES",
        description = "Uploading activity images",
        entityType = "ActivityImage"
    )
    public ResponseEntity<ApiResponse<?>> uploadImages(List<CreateActivityImageDTO> imageDTOs) {
        log.info("Uploading {} activity images", imageDTOs != null ? imageDTOs.size() : 0);

        try {
            if (imageDTOs == null || imageDTOs.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No images provided", "NO_IMAGES_PROVIDED")
                );
            }

            long totalSize = 0;
            for (CreateActivityImageDTO dto : imageDTOs) {
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

            List<String> validationErrors = new ArrayList<>();
            for (int i = 0; i < imageDTOs.size(); i++) {
                CreateActivityImageDTO dto = imageDTOs.get(i);

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

                try {
                    Long activityId = idObfuscator.decodeId(dto.getActivityId());
                    if (!activityRepository.existsById(activityId)) {
                        validationErrors.add(String.format("Image %d: Activity not found", i + 1));
                    }
                } catch (Exception e) {
                    validationErrors.add(String.format("Image %d: Invalid Activity ID", i + 1));
                }
            }

            if (!validationErrors.isEmpty()) {
                String errorMessage = "Validation failed: " + String.join("; ", validationErrors);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, errorMessage, "VALIDATION_ERROR")
                );
            }

            List<ActivityImageDTO> createdImages = new ArrayList<>();
            List<String> savedFileNames = new ArrayList<>();
            Map<Long, Integer> displayOrderMap = new HashMap<>();

            try {
                for (CreateActivityImageDTO dto : imageDTOs) {
                    Long activityId = idObfuscator.decodeId(dto.getActivityId());
                    Activity activity = activityRepository.findById(activityId).orElse(null);

                    if (activity == null) {
                        continue;
                    }

                    int displayOrder;
                    if (displayOrderMap.containsKey(activityId)) {
                        displayOrder = displayOrderMap.get(activityId) + 1;
                    } else {
                        displayOrder = activityImageRepository.findMaxDisplayOrderByActivityId(activityId) + 1;
                    }
                    displayOrderMap.put(activityId, displayOrder);

                    MultipartFile file = dto.getImage();
                    String savedFileName = storageService.saveImage(file);
                    if (savedFileName == null) {
                        rollbackSavedFiles(savedFileNames);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            ApiResponse.error(500, "Failed to save image file: " + file.getOriginalFilename(), "STORAGE_ERROR")
                        );
                    }
                    savedFileNames.add(savedFileName);

                    ActivityImage image = ActivityImage.builder()
                        .activity(activity)
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

                    image = activityImageRepository.save(image);
                    createdImages.add(getService.toDTO(image));
                }

                log.info("{} activity images created successfully", createdImages.size());

                return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(201, createdImages.size() + " image(s) uploaded successfully", createdImages)
                );

            } catch (Exception e) {
                rollbackSavedFiles(savedFileNames);
                log.error("Failed to create activity image records, rolled back {} files", savedFileNames.size(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to create image records", "DATABASE_ERROR")
                );
            }

        } catch (Exception e) {
            log.error("Error uploading activity images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to upload activity images", "ACTIVITY_IMAGE_UPLOAD_FAILED")
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
