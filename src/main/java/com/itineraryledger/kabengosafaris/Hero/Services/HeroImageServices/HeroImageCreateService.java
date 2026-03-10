package com.itineraryledger.kabengosafaris.Hero.Services.HeroImageServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.Hero.DTOs.HeroImageDTOs.CreateHeroImageDTO;
import com.itineraryledger.kabengosafaris.Hero.DTOs.HeroImageDTOs.HeroImageDTO;
import com.itineraryledger.kabengosafaris.Hero.Entity.Hero;
import com.itineraryledger.kabengosafaris.Hero.Entity.HeroImage;
import com.itineraryledger.kabengosafaris.Hero.Repository.HeroImageRepository;
import com.itineraryledger.kabengosafaris.Hero.Repository.HeroRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for creating hero images.
 *
 * Provides:
 * - Bulk upload images using list of CreateHeroImageDTO
 */
@Service
@Slf4j
@Transactional
public class HeroImageCreateService {

    private final HeroImageRepository heroImageRepository;
    private final HeroRepository heroRepository;
    private final HeroImageStorageService storageService;
    private final HeroImageGetService getService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public HeroImageCreateService(
        HeroImageRepository heroImageRepository,
        HeroRepository heroRepository,
        HeroImageStorageService storageService,
        HeroImageGetService getService,
        IdObfuscator idObfuscator
    ) {
        this.heroImageRepository = heroImageRepository;
        this.heroRepository = heroRepository;
        this.storageService = storageService;
        this.getService = getService;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Upload multiple images for heroes
     *
     * @param imageDTOs List of CreateHeroImageDTO containing image files and metadata
     * @return ResponseEntity with ApiResponse containing success or error
     */
    @AuditLogAnnotation(
        action = "CREATE_HERO_IMAGES",
        description = "Uploading hero images",
        entityType = "HeroImage"
    )
    public ResponseEntity<ApiResponse<?>> uploadImages(List<CreateHeroImageDTO> imageDTOs) {
        log.info("Uploading {} hero images", imageDTOs != null ? imageDTOs.size() : 0);

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
            for (CreateHeroImageDTO dto : imageDTOs) {
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
                CreateHeroImageDTO dto = imageDTOs.get(i);

                // Validate hero ID
                if (dto.getHeroId() == null || dto.getHeroId().isBlank()) {
                    validationErrors.add(String.format("Image %d: Hero ID is required", i + 1));
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

                // Validate hero exists
                try {
                    Long heroId = idObfuscator.decodeId(dto.getHeroId());
                    if (!heroRepository.existsById(heroId)) {
                        validationErrors.add(String.format("Image %d: Hero not found", i + 1));
                    }
                } catch (Exception e) {
                    validationErrors.add(String.format("Image %d: Invalid Hero ID", i + 1));
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
            List<HeroImageDTO> createdImages = new ArrayList<>();
            List<String> savedFileNames = new ArrayList<>();

            try {
                for (CreateHeroImageDTO dto : imageDTOs) {
                    Long heroId = idObfuscator.decodeId(dto.getHeroId());
                    Hero hero = heroRepository.findById(heroId).orElse(null);

                    if (hero == null) {
                        continue; // Already validated, skip if somehow null
                    }

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
                    HeroImage image = HeroImage.builder()
                        .hero(hero)
                        .fileName(savedFileName)
                        .originalFileName(file.getOriginalFilename())
                        .altText(dto.getAltText())
                        .caption(dto.getCaption())
                        .description(dto.getDescription())
                        .isPrimary(false)
                        .isActive(true)
                        .fileSize(file.getSize())
                        .mimeType(storageService.getMimeType(savedFileName))
                        .build();

                    image = heroImageRepository.save(image);
                    createdImages.add(getService.toDTO(image));
                }

                log.info("{} hero images created successfully", createdImages.size());

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
                log.error("Failed to create hero image records, rolled back {} files",
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
            log.error("Error uploading hero images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to upload hero images",
                    "HERO_IMAGE_UPLOAD_FAILED"
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
