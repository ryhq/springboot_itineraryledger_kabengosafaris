package com.itineraryledger.kabengosafaris.Park.Services.ParkImageServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.Park.DTOs.ParkImageDTOs.CreateParkImageDTO;
import com.itineraryledger.kabengosafaris.Park.DTOs.ParkImageDTOs.ParkImageDTO;
import com.itineraryledger.kabengosafaris.Park.Entities.ParkImage;
import com.itineraryledger.kabengosafaris.Park.Entities.ParkImage.ImageType;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.Park.Repositories.ParkImageRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for creating park images.
 */
@Service
@Slf4j
@Transactional
public class ParkImageCreateService {

    private final ParkImageRepository parkImageRepository;
    private final ParkRepository parkRepository;
    private final ParkImageStorageService storageService;
    private final ParkImageGetService getService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ParkImageCreateService(
        ParkImageRepository parkImageRepository,
        ParkRepository parkRepository,
        ParkImageStorageService storageService,
        ParkImageGetService getService,
        IdObfuscator idObfuscator
    ) {
        this.parkImageRepository = parkImageRepository;
        this.parkRepository = parkRepository;
        this.storageService = storageService;
        this.getService = getService;
        this.idObfuscator = idObfuscator;
    }

    @AuditLogAnnotation(
        action = "CREATE_PARK_IMAGES",
        description = "Uploading park images",
        entityType = "ParkImage"
    )
    public ResponseEntity<ApiResponse<?>> uploadImages(List<CreateParkImageDTO> imageDTOs) {
        log.info("Uploading {} park images", imageDTOs != null ? imageDTOs.size() : 0);

        try {
            if (imageDTOs == null || imageDTOs.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No images provided", "NO_IMAGES_PROVIDED")
                );
            }

            long totalSize = 0;
            for (CreateParkImageDTO dto : imageDTOs) {
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
                CreateParkImageDTO dto = imageDTOs.get(i);

                if (dto.getParkId() == null || dto.getParkId().isBlank()) {
                    validationErrors.add(String.format("Image %d: Park ID is required", i + 1));
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
                    Long parkId = idObfuscator.decodeId(dto.getParkId());
                    if (!parkRepository.existsById(parkId)) {
                        validationErrors.add(String.format("Image %d: Park not found", i + 1));
                    }
                } catch (Exception e) {
                    validationErrors.add(String.format("Image %d: Invalid Park ID", i + 1));
                }
            }

            if (!validationErrors.isEmpty()) {
                String errorMessage = "Validation failed: " + String.join("; ", validationErrors);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, errorMessage, "VALIDATION_ERROR")
                );
            }

            List<ParkImageDTO> createdImages = new ArrayList<>();
            List<String> savedFileNames = new ArrayList<>();
            Map<Long, Integer> displayOrderMap = new HashMap<>();

            try {
                for (CreateParkImageDTO dto : imageDTOs) {
                    Long parkId = idObfuscator.decodeId(dto.getParkId());
                    Park park = parkRepository.findById(parkId).orElse(null);

                    if (park == null) {
                        continue;
                    }

                    int displayOrder;
                    if (displayOrderMap.containsKey(parkId)) {
                        displayOrder = displayOrderMap.get(parkId) + 1;
                    } else {
                        displayOrder = parkImageRepository.findMaxDisplayOrderByParkId(parkId) + 1;
                    }
                    displayOrderMap.put(parkId, displayOrder);

                    MultipartFile file = dto.getImage();
                    String savedFileName = storageService.saveImage(file);
                    if (savedFileName == null) {
                        rollbackSavedFiles(savedFileNames);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            ApiResponse.error(500, "Failed to save image file: " + file.getOriginalFilename(), "STORAGE_ERROR")
                        );
                    }
                    savedFileNames.add(savedFileName);

                    ParkImage image = ParkImage.builder()
                        .park(park)
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

                    image = parkImageRepository.save(image);
                    createdImages.add(getService.toDTO(image));
                }

                log.info("{} park images created successfully", createdImages.size());

                return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(201, createdImages.size() + " image(s) uploaded successfully", createdImages)
                );

            } catch (Exception e) {
                rollbackSavedFiles(savedFileNames);
                log.error("Failed to create park image records, rolled back {} files", savedFileNames.size(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to create image records", "DATABASE_ERROR")
                );
            }

        } catch (Exception e) {
            log.error("Error uploading park images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to upload park images", "PARK_IMAGE_UPLOAD_FAILED")
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
