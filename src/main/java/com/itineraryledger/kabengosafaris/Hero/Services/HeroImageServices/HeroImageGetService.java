package com.itineraryledger.kabengosafaris.Hero.Services.HeroImageServices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Hero.DTOs.HeroImageDTOs.HeroImageDTO;
import com.itineraryledger.kabengosafaris.Hero.Entity.HeroImage;
import com.itineraryledger.kabengosafaris.Hero.Enums.HeroPage;
import com.itineraryledger.kabengosafaris.Hero.Repository.HeroImageRepository;
import com.itineraryledger.kabengosafaris.Hero.Specifications.HeroImageSpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HeroImageGetService - Service for retrieving hero images
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class HeroImageGetService {

    private final HeroImageRepository heroImageRepository;
    private final HeroImageStorageService storageService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public HeroImageGetService(
        HeroImageRepository heroImageRepository,
        HeroImageStorageService storageService,
        IdObfuscator idObfuscator
    ) {
        this.heroImageRepository = heroImageRepository;
        this.storageService = storageService;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get all images for a hero
     *
     * @param heroId The obfuscated hero ID
     * @return ResponseEntity with ApiResponse containing the images
     */
    public ResponseEntity<ApiResponse<?>> getHeroImages(String heroId) {
        log.info("Fetching images for hero: {}", heroId);

        try {
            // Decode hero ID
            Long decodedHeroId;
            try {
                decodedHeroId = idObfuscator.decodeId(heroId);
            } catch (Exception e) {
                log.warn("Invalid hero ID format: {}", heroId);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid hero ID format", "INVALID_HERO_ID")
                );
            }

            // Fetch images
            List<HeroImage> images = heroImageRepository.findByHeroIdAndIsActiveTrueOrderByDisplayOrderAsc(decodedHeroId);

            // Convert to DTOs
            List<HeroImageDTO> imageDTOs = images.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Hero images retrieved successfully",
                    imageDTOs
                )
            );

        } catch (Exception e) {
            log.error("Error fetching hero images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch hero images",
                    "HERO_IMAGES_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all images with filters, pagination, and sorting
     *
     * @param obfuscatedHeroId Filter by hero ID (obfuscated)
     * @param heroTitle Filter by hero title (partial match)
     * @param heroPage Filter by hero page
     * @param isPrimary Filter by primary status
     * @param isActive Filter by active status
     * @param displayOrder Filter by display order
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated images
     */
    public ResponseEntity<?> getAllImages(
            String obfuscatedHeroId,
            String heroTitle,
            HeroPage heroPage,
            Boolean isPrimary,
            Boolean isActive,
            Integer displayOrder,
            int page,
            int size,
            String sortDirection
    ) {
        Specification<HeroImage> spec = Specification.unrestricted();

        if (obfuscatedHeroId != null && !obfuscatedHeroId.isBlank()) {
            try {
                Long heroId = idObfuscator.decodeId(obfuscatedHeroId);
                spec = spec.and(HeroImageSpecification.byHeroId(heroId));
            } catch (Exception e) {
                log.warn("Failed to decode hero ID: {}", obfuscatedHeroId);
            }
        }

        if (heroTitle != null && !heroTitle.isBlank()) {
            spec = spec.and(HeroImageSpecification.byHeroTitle(heroTitle));
        }
        if (heroPage != null) {
            spec = spec.and(HeroImageSpecification.byHeroPage(heroPage));
        }
        if (isPrimary != null) {
            spec = spec.and(HeroImageSpecification.byIsPrimary(isPrimary));
        }
        if (isActive != null) {
            spec = spec.and(HeroImageSpecification.byIsActive(isActive));
        }
        if (displayOrder != null) {
            spec = spec.and(HeroImageSpecification.byDisplayOrder(displayOrder));
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"));

        Page<HeroImage> imagePage = heroImageRepository.findAll(spec, pageable);

        List<HeroImageDTO> imageDTOs = imagePage.getContent().stream()
            .map(this::toDTO)
            .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("images", imageDTOs);
        response.put("currentPage", imagePage.getNumber());
        response.put("totalItems", imagePage.getTotalElements());
        response.put("totalPages", imagePage.getTotalPages());
        response.put("pageSize", imagePage.getSize());
        response.put("hasNext", imagePage.hasNext());
        response.put("hasPrevious", imagePage.hasPrevious());

        return ResponseEntity.ok(ApiResponse.success(200, "Hero images retrieved successfully", response));
    }

    /**
     * Get image by ID
     *
     * @param obfuscatedId The obfuscated image ID
     * @return ResponseEntity with ApiResponse containing the image
     */
    public ResponseEntity<?> getImageById(String obfuscatedId) {
        log.info("Getting hero image with ID: {}", obfuscatedId);

        try {
            Long id = idObfuscator.decodeId(obfuscatedId);
            HeroImage image = heroImageRepository.findById(id).orElse(null);

            if (image == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Hero image not found", "IMAGE_NOT_FOUND")
                );
            }

            return ResponseEntity.ok(ApiResponse.success(200, "Hero image retrieved successfully", toDTO(image)));

        } catch (Exception e) {
            log.warn("Failed to decode hero image ID: {}", obfuscatedId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid image ID", "INVALID_IMAGE_ID")
            );
        }
    }

    /**
     * Get image by filename (helper method for controllers)
     *
     * @param fileName The image filename
     * @return HeroImage entity or null if not found
     */
    public HeroImage getImageByFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        return heroImageRepository.findByFileName(fileName).orElse(null);
    }

    /**
     * Convert HeroImage entity to HeroImageDTO
     */
    public HeroImageDTO toDTO(HeroImage image) {
        String obfuscatedId = idObfuscator.encodeId(image.getId());

        return HeroImageDTO.builder()
            .id(obfuscatedId)
            .heroId(idObfuscator.encodeId(image.getHero().getId()))
            .imageUrl(storageService.constructImageUrl(obfuscatedId))
            .fileImageUrl(storageService.constructFileImageUrl(image.getFileName()))
            .fileName(image.getFileName())
            .originalFileName(image.getOriginalFileName())
            .altText(image.getAltText())
            .caption(image.getCaption())
            .description(image.getDescription())
            .isPrimary(image.getIsPrimary())
            .isActive(image.getIsActive())
            .displayOrder(image.getDisplayOrder())
            .fileSize(image.getFileSize())
            .fileSizeFormatted(storageService.formatFileSize(image.getFileSize() != null ? image.getFileSize() : 0L))
            .mimeType(image.getMimeType())
            .width(image.getWidth())
            .height(image.getHeight())
            .createdAt(image.getCreatedAt())
            .updatedAt(image.getUpdatedAt())
            .build();
    }
}
