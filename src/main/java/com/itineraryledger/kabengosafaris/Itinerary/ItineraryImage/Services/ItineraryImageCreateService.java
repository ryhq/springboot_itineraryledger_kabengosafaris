package com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.DTOs.CreateItineraryImageDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.DTOs.ItineraryImageDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Entity.ItineraryImage;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Entity.ItineraryImage.ImageType;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Repository.ItineraryImageRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for creating itinerary images.
 */
@Service
@Slf4j
@Transactional
public class ItineraryImageCreateService {

    private final ItineraryImageRepository itineraryImageRepository;
    private final ItineraryRepository itineraryRepository;
    private final ItineraryImageStorageService storageService;
    private final ItineraryImageGetService getService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryImageCreateService(
        ItineraryImageRepository itineraryImageRepository,
        ItineraryRepository itineraryRepository,
        ItineraryImageStorageService storageService,
        ItineraryImageGetService getService,
        IdObfuscator idObfuscator
    ) {
        this.itineraryImageRepository = itineraryImageRepository;
        this.itineraryRepository = itineraryRepository;
        this.storageService = storageService;
        this.getService = getService;
        this.idObfuscator = idObfuscator;
    }

    @AuditLogAnnotation(
        action = "CREATE_ITINERARY_IMAGES",
        description = "Uploading itinerary images",
        entityType = "ItineraryImage"
    )
    public ResponseEntity<ApiResponse<?>> uploadImages(List<CreateItineraryImageDTO> imageDTOs) {
        log.info("Uploading {} itinerary images", imageDTOs != null ? imageDTOs.size() : 0);

        try {
            if (imageDTOs == null || imageDTOs.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No images provided", "NO_IMAGES_PROVIDED")
                );
            }

            long totalSize = 0;
            for (CreateItineraryImageDTO dto : imageDTOs) {
                if (dto.getImage() != null) totalSize += dto.getImage().getSize();
            }

            String requestSizeError = storageService.validateRequestSize(totalSize);
            if (requestSizeError != null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, requestSizeError, "REQUEST_SIZE_EXCEEDED")
                );
            }

            List<String> validationErrors = new ArrayList<>();
            for (int i = 0; i < imageDTOs.size(); i++) {
                CreateItineraryImageDTO dto = imageDTOs.get(i);

                if (dto.getItineraryId() == null || dto.getItineraryId().isBlank()) {
                    validationErrors.add(String.format("Image %d: Itinerary ID is required", i + 1));
                    continue;
                }
                if (dto.getImage() == null || dto.getImage().isEmpty()) {
                    validationErrors.add(String.format("Image %d: Image file is required", i + 1));
                    continue;
                }
                String imageError = storageService.validateImage(dto.getImage());
                if (imageError != null) {
                    String filename = dto.getImage().getOriginalFilename() != null ? dto.getImage().getOriginalFilename() : "unknown";
                    validationErrors.add(String.format("Image %d (%s): %s", i + 1, filename, imageError));
                }
                try {
                    Long itineraryId = idObfuscator.decodeId(dto.getItineraryId());
                    if (!itineraryRepository.existsById(itineraryId)) {
                        validationErrors.add(String.format("Image %d: Itinerary not found", i + 1));
                    }
                } catch (Exception e) {
                    validationErrors.add(String.format("Image %d: Invalid Itinerary ID", i + 1));
                }
            }

            if (!validationErrors.isEmpty()) {
                String errorMessage = "Validation failed: " + String.join("; ", validationErrors);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, errorMessage, "VALIDATION_ERROR")
                );
            }

            List<ItineraryImageDTO> createdImages = new ArrayList<>();
            List<String> savedFileNames = new ArrayList<>();
            Map<Long, Integer> displayOrderMap = new HashMap<>();

            try {
                for (CreateItineraryImageDTO dto : imageDTOs) {
                    Long itineraryId = idObfuscator.decodeId(dto.getItineraryId());
                    Itinerary itinerary = itineraryRepository.findById(itineraryId).orElse(null);
                    if (itinerary == null) continue;

                    int displayOrder;
                    if (displayOrderMap.containsKey(itineraryId)) {
                        displayOrder = displayOrderMap.get(itineraryId) + 1;
                    } else {
                        displayOrder = itineraryImageRepository.findMaxDisplayOrderByItineraryId(itineraryId) + 1;
                    }
                    displayOrderMap.put(itineraryId, displayOrder);

                    MultipartFile file = dto.getImage();
                    String savedFileName = storageService.saveImage(file);
                    if (savedFileName == null) {
                        rollbackSavedFiles(savedFileNames);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            ApiResponse.error(500, "Failed to save image file: " + file.getOriginalFilename(), "STORAGE_ERROR")
                        );
                    }
                    savedFileNames.add(savedFileName);

                    // First image for an itinerary becomes the primary hero automatically.
                    boolean makePrimary = !itineraryImageRepository.hasPrimaryImage(itineraryId);

                    ItineraryImage image = ItineraryImage.builder()
                        .itinerary(itinerary)
                        .fileName(savedFileName)
                        .originalFileName(file.getOriginalFilename())
                        .imageType(dto.getImageType() != null ? dto.getImageType() : ImageType.GALLERY)
                        .altText(dto.getAltText())
                        .caption(dto.getCaption())
                        .description(dto.getDescription())
                        .isPrimary(makePrimary)
                        .isActive(true)
                        .isWebActive(true)
                        .displayOrder(displayOrder)
                        .fileSize(file.getSize())
                        .mimeType(storageService.getMimeType(savedFileName))
                        .build();

                    image = itineraryImageRepository.save(image);
                    createdImages.add(getService.toDTO(image));
                }

                log.info("{} itinerary images created successfully", createdImages.size());
                return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(201, createdImages.size() + " image(s) uploaded successfully", createdImages)
                );
            } catch (Exception e) {
                rollbackSavedFiles(savedFileNames);
                log.error("Failed to create itinerary image records, rolled back {} files", savedFileNames.size(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to create image records", "DATABASE_ERROR")
                );
            }
        } catch (Exception e) {
            log.error("Error uploading itinerary images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to upload itinerary images", "ITINERARY_IMAGE_UPLOAD_FAILED")
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
