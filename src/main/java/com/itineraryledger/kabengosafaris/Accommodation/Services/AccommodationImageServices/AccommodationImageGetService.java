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

import java.util.Arrays;
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

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.ListStats listStats;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "imageType", "isPrimary", "isActive", "displayOrder", "fileSize", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

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
            .fileImageUrl(storageService.constructFileImageUrl(image.getFileName()))
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
            .isWebActive(image.getIsWebActive())
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
            Boolean isWebActive,
            Integer displayOrder,
            java.util.List<String> statuses,
            java.util.List<String> visibilities,
            java.util.List<String> qualities,
            java.time.LocalDateTime createdAfter,
            String keyword,
            Boolean includeStats,
            int page,
            int size,
            String sortBy,
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
        if (isWebActive != null) {
            spec = spec.and(AccommodationImageSpecification.isWebActive(isWebActive));
        }
        if (displayOrder != null) {
            spec = spec.and(AccommodationImageSpecification.byDisplayOrder(displayOrder));
        }

        // Sorting with validation
        // multi-value facets: every stat card must also work as a filter
        if (statuses != null && !statuses.isEmpty()) {
            java.util.List<Boolean> states = new java.util.ArrayList<>();
            if (statuses.contains("active")) states.add(true);
            if (statuses.contains("inactive")) states.add(false);
            if (states.size() == 1) spec = spec.and(AccommodationImageSpecification.byIsActive(states.get(0)));
        }
        if (visibilities != null && !visibilities.isEmpty()) {
            java.util.List<Boolean> states = new java.util.ArrayList<>();
            if (visibilities.contains("live")) states.add(true);
            if (visibilities.contains("hidden")) states.add(false);
            if (states.size() == 1) spec = spec.and(AccommodationImageSpecification.isWebActive(states.get(0)));
        }
        if (qualities != null && !qualities.isEmpty()) {
            java.util.List<org.springframework.data.jpa.domain.Specification<AccommodationImage>> any = new java.util.ArrayList<>();
            if (qualities.contains("no-caption")) any.add(AccommodationImageSpecification.missingCaption());
            if (qualities.contains("no-alt")) any.add(AccommodationImageSpecification.missingAltText());
            if (!any.isEmpty()) {
                org.springframework.data.jpa.domain.Specification<AccommodationImage> combined = any.get(0);
                for (int i = 1; i < any.size(); i++) combined = combined.or(any.get(i));
                spec = spec.and(combined);
            }
        }
        if (createdAfter != null) spec = spec.and(AccommodationImageSpecification.createdAfter(createdAfter));
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(AccommodationImageSpecification.searchKeyword(keyword));
        }

        String validatedSortBy = validateSortField(sortBy);
        if (validatedSortBy == null) {
            log.warn("Invalid sort field: {}", sortBy);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
            );
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validatedSortBy));

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
        response.put("validSortFields", VALID_SORT_FIELDS);
        response.put("currentSortBy", validatedSortBy);
        response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
        // counters from the SAME spec as the rows, so cards and table cannot disagree
        if (includeStats == null || includeStats) {
            response.put("stats", computeStats(spec));
        }

        return ResponseEntity.ok(ApiResponse.success(200, "Accommodation images retrieved successfully.", response));
    }

    /** Dashboard counters for the CURRENT filter set (see CLAUDE.md: stats on every list). */
    private Map<String, Object> computeStats(Specification<AccommodationImage> base) {
        return listStats.of(AccommodationImage.class, base)
            .total()
            .count("active", AccommodationImageSpecification.byIsActive(true))
            .complement("inactive", "active")
            .count("primary", AccommodationImageSpecification.byIsPrimary(true))
            .count("webActive", AccommodationImageSpecification.isWebActive(true))
            .complement("webHidden", "webActive")
            .count("missingCaption", AccommodationImageSpecification.missingCaption())
            .count("missingAltText", AccommodationImageSpecification.missingAltText())
            .recency(AccommodationImageSpecification::createdAfter)
            .build();
    }

    /**
     * Get image by ID
     */
    public ResponseEntity<?> getImageById(String obfuscatedId, String scopeParentId) {
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

        AccommodationImageDTO imageDTO = toDTO(image);

        // Decode optional scope parent ID for scoped navigation
        Long decodedParentId = null;
        if (scopeParentId != null && !scopeParentId.isEmpty()) {
            try {
                decodedParentId = idObfuscator.decodeId(scopeParentId);
            } catch (Exception ex) {
                log.warn("Invalid scopeParentId: {}, falling back to global navigation", scopeParentId);
            }
        }

        // Circular navigation (scoped if parent provided, global otherwise)
        Long nextId, previousId;
        if (decodedParentId != null) {
            nextId = accommodationImageRepository.findNextIdByParent(id, decodedParentId).orElse(null);
            previousId = accommodationImageRepository.findPreviousIdByParent(id, decodedParentId).orElse(null);
            if (nextId == null) nextId = accommodationImageRepository.findFirstIdByParent(decodedParentId).orElse(null);
            if (previousId == null) previousId = accommodationImageRepository.findLastIdByParent(decodedParentId).orElse(null);
        } else {
            nextId = accommodationImageRepository.findNextId(id).orElse(null);
            previousId = accommodationImageRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = accommodationImageRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = accommodationImageRepository.findLastId().orElse(null);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("image", imageDTO);
        response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
        response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
        response.put("scopeParentId", scopeParentId);

        return ResponseEntity.ok(ApiResponse.success(200, "Image retrieved successfully.", response));
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

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }
}
