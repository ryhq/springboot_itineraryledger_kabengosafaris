package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationImageServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationImageDTOs.AccommodationImageDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationCategory;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationType;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage.ImageType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationImageRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for retrieving accommodation images.
 *
 * Provides:
 * - Paginated listing with filters based on image and accommodation attributes
 * - Get image by ID
 * - Get image by filename (for file serving)
 */
@Service
@Slf4j
public class AccommodationImageGetService {

    @Autowired
    private AccommodationImageRepository accommodationImageRepository;

    @Autowired
    private AccommodationImageStorageService storageService;

    @Autowired
    private IdObfuscator idObfuscator;

    /**
     * Convert entity to DTO with full image URL
     */
    public AccommodationImageDTO toDTO(AccommodationImage image) {
        String obfuscatedId = idObfuscator.encodeId(image.getId());
        return AccommodationImageDTO.builder()
            .id(obfuscatedId)
            .accommodationId(idObfuscator.encodeId(image.getAccommodation().getId()))
            .accommodationName(image.getAccommodation().getName())
            .imageUrl(storageService.constructImageUrl(obfuscatedId))
            .fileName(image.getFileName())
            .originalFileName(image.getOriginalFileName())
            .imageType(image.getImageType())
            .imageTypeDisplayName(image.getImageType().getDisplayName())
            .imageTypeDescription(image.getImageType().getDescription())
            .altText(image.getAltText())
            .caption(image.getCaption())
            .description(image.getDescription())
            .isPrimary(image.getIsPrimary())
            .isActive(image.getIsActive())
            .displayOrder(image.getDisplayOrder())
            .fileSize(image.getFileSize())
            .fileSizeFormatted(image.getFileSize() != null ? storageService.formatFileSize(image.getFileSize()) : null)
            .mimeType(image.getMimeType())
            .width(image.getWidth())
            .height(image.getHeight())
            .createdAt(image.getCreatedAt())
            .updatedAt(image.getUpdatedAt())
            .build();
    }

    /**
     * Convert list of entities to DTOs
     */
    private List<AccommodationImageDTO> toDTOList(List<AccommodationImage> images) {
        return images.stream().map(this::toDTO).toList();
    }

    /**
     * Get all images with filters, pagination, and sorting
     * Always sort by createdAt descending by default
     */
    public ResponseEntity<?> getAllImages(
            String obfuscatedAccommodationId,
            String accommodationName,
            AccommodationType accommodationType,
            AccommodationCategory accommodationCategory,
            ImageType imageType,
            Boolean isPrimary,
            Boolean isActive,
            int page,
            int size,
            String sortDirection
    ) {
        // Build specification
        Specification<AccommodationImage> spec = Specification.unrestricted();

        // Accommodation ID filter
        if (obfuscatedAccommodationId != null && !obfuscatedAccommodationId.isBlank()) {
            try {
                Long accommodationId = idObfuscator.decodeId(obfuscatedAccommodationId);
                spec = spec.and(AccommodationImageSpecification.byAccommodationId(accommodationId));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid Accommodation ID provided.", "VALIDATION_ERROR")
                );
            }
        }

        // Accommodation attribute filters
        if (accommodationName != null && !accommodationName.isBlank()) {
            spec = spec.and(AccommodationImageSpecification.byAccommodationName(accommodationName));
        }
        if (accommodationType != null) {
            spec = spec.and(AccommodationImageSpecification.byAccommodationType(accommodationType));
        }
        if (accommodationCategory != null) {
            spec = spec.and(AccommodationImageSpecification.byAccommodationCategory(accommodationCategory));
        }

        // Image attribute filters
        if (imageType != null) {
            spec = spec.and(AccommodationImageSpecification.byImageType(imageType));
        }
        if (isPrimary != null) {
            spec = spec.and(AccommodationImageSpecification.byIsPrimary(isPrimary));
        }
        if (isActive != null) {
            spec = spec.and(AccommodationImageSpecification.byIsActive(isActive));
        }

        // Sort direction - default DESC for createdAt
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        // Always sort by createdAt
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"));

        // Execute query
        Page<AccommodationImage> imagePage = accommodationImageRepository.findAll(spec, pageable);

        // Build response
        List<AccommodationImageDTO> dtos = toDTOList(imagePage.getContent());

        Map<String, Object> response = new HashMap<>();
        response.put("content", dtos);
        response.put("currentPage", imagePage.getNumber());
        response.put("totalItems", imagePage.getTotalElements());
        response.put("totalPages", imagePage.getTotalPages());
        response.put("pageSize", imagePage.getSize());
        response.put("hasNext", imagePage.hasNext());
        response.put("hasPrevious", imagePage.hasPrevious());

        return ResponseEntity.ok(ApiResponse.success(200, "Accommodation images retrieved successfully.", response));
    }

    /**
     * Get image by ID
     */
    public ResponseEntity<?> getImageById(String obfuscatedId) {
        Long id;
        try {
            id = idObfuscator.decodeId(obfuscatedId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid Image ID provided.", "VALIDATION_ERROR")
            );
        }

        AccommodationImage image = accommodationImageRepository.findById(id).orElse(null);

        if (image == null) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(404, "Image not found.", "NOT_FOUND")
            );
        }

        return ResponseEntity.ok(ApiResponse.success(200, "Image retrieved successfully.", toDTO(image)));
    }

    /**
     * Get image by filename (for file serving endpoint)
     * Returns the entity for the controller to serve the file
     */
    public AccommodationImage getImageByFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        return accommodationImageRepository.findByFileName(fileName).orElse(null);
    }
}
