package com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyImageServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Testimony.DTOs.TestimonyImageDTOs.CreateTestimonyImageDTO;
import com.itineraryledger.kabengosafaris.Testimony.DTOs.TestimonyImageDTOs.TestimonyImageDTO;
import com.itineraryledger.kabengosafaris.Testimony.Entity.Testimony;
import com.itineraryledger.kabengosafaris.Testimony.Entity.TestimonyImage;
import com.itineraryledger.kabengosafaris.Testimony.Repository.TestimonyImageRepository;
import com.itineraryledger.kabengosafaris.Testimony.Repository.TestimonyRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@Transactional
public class TestimonyImageCreateService {

    private final TestimonyImageRepository testimonyImageRepository;
    private final TestimonyRepository testimonyRepository;
    private final TestimonyImageStorageService storageService;
    private final TestimonyImageGetService getService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public TestimonyImageCreateService(
        TestimonyImageRepository testimonyImageRepository,
        TestimonyRepository testimonyRepository,
        TestimonyImageStorageService storageService,
        TestimonyImageGetService getService,
        IdObfuscator idObfuscator
    ) {
        this.testimonyImageRepository = testimonyImageRepository;
        this.testimonyRepository = testimonyRepository;
        this.storageService = storageService;
        this.getService = getService;
        this.idObfuscator = idObfuscator;
    }

    @AuditLogAnnotation(
        action = "CREATE_TESTIMONY_IMAGES",
        description = "Uploading testimony images",
        entityType = "TestimonyImage"
    )
    public ResponseEntity<ApiResponse<?>> uploadImages(List<CreateTestimonyImageDTO> imageDTOs) {
        log.info("Uploading {} testimony images", imageDTOs != null ? imageDTOs.size() : 0);

        try {
            if (imageDTOs == null || imageDTOs.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No images provided", "NO_IMAGES_PROVIDED")
                );
            }

            // Calculate total request size
            long totalSize = 0;
            for (CreateTestimonyImageDTO dto : imageDTOs) {
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

            // Validate all images first
            List<String> validationErrors = new ArrayList<>();
            for (int i = 0; i < imageDTOs.size(); i++) {
                CreateTestimonyImageDTO dto = imageDTOs.get(i);

                if (dto.getTestimonyId() == null || dto.getTestimonyId().isBlank()) {
                    validationErrors.add(String.format("Image %d: Testimony ID is required", i + 1));
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
                    Long testimonyId = idObfuscator.decodeId(dto.getTestimonyId());
                    if (!testimonyRepository.existsById(testimonyId)) {
                        validationErrors.add(String.format("Image %d: Testimony not found", i + 1));
                    }
                } catch (Exception e) {
                    validationErrors.add(String.format("Image %d: Invalid Testimony ID", i + 1));
                }
            }

            if (!validationErrors.isEmpty()) {
                String errorMessage = "Validation failed: " + String.join("; ", validationErrors);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, errorMessage, "VALIDATION_ERROR")
                );
            }

            // Process each image
            List<TestimonyImageDTO> createdImages = new ArrayList<>();
            List<String> savedFileNames = new ArrayList<>();
            Map<Long, Integer> displayOrderMap = new HashMap<>();

            try {
                for (CreateTestimonyImageDTO dto : imageDTOs) {
                    Long testimonyId = idObfuscator.decodeId(dto.getTestimonyId());
                    Testimony testimony = testimonyRepository.findById(testimonyId).orElse(null);

                    if (testimony == null) continue;

                    int displayOrder;
                    if (displayOrderMap.containsKey(testimonyId)) {
                        displayOrder = displayOrderMap.get(testimonyId) + 1;
                    } else {
                        List<TestimonyImage> existingImages = testimonyImageRepository.findByTestimonyId(testimonyId);
                        displayOrder = existingImages.isEmpty() ? 1 :
                            existingImages.stream()
                                .mapToInt(TestimonyImage::getDisplayOrder)
                                .max()
                                .orElse(0) + 1;
                    }
                    displayOrderMap.put(testimonyId, displayOrder);

                    MultipartFile file = dto.getImage();
                    String savedFileName = storageService.saveImage(file);
                    if (savedFileName == null) {
                        rollbackSavedFiles(savedFileNames);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            ApiResponse.error(500, "Failed to save image file: " + file.getOriginalFilename(), "STORAGE_ERROR")
                        );
                    }
                    savedFileNames.add(savedFileName);

                    TestimonyImage image = TestimonyImage.builder()
                        .testimony(testimony)
                        .fileName(savedFileName)
                        .originalFileName(file.getOriginalFilename())
                        .altText(dto.getAltText())
                        .caption(dto.getCaption())
                        .description(dto.getDescription())
                        .isPrimary(false)
                        .isActive(true)
                        .displayOrder(displayOrder)
                        .fileSize(file.getSize())
                        .mimeType(storageService.getMimeType(savedFileName))
                        .build();

                    image = testimonyImageRepository.save(image);
                    createdImages.add(getService.toDTO(image));
                }

                log.info("{} testimony images created successfully", createdImages.size());

                return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(201, createdImages.size() + " image(s) uploaded successfully", createdImages)
                );

            } catch (Exception e) {
                rollbackSavedFiles(savedFileNames);
                log.error("Failed to create testimony image records, rolled back {} files", savedFileNames.size(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to create image records", "DATABASE_ERROR")
                );
            }

        } catch (Exception e) {
            log.error("Error uploading testimony images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to upload testimony images", "TESTIMONY_IMAGE_UPLOAD_FAILED")
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
