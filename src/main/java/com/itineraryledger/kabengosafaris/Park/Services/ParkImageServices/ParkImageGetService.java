package com.itineraryledger.kabengosafaris.Park.Services.ParkImageServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Park.DTOs.ParkImageDTOs.ParkImageDTO;
import com.itineraryledger.kabengosafaris.Park.Entities.ParkImage;
import com.itineraryledger.kabengosafaris.Park.Entities.ParkImage.ImageType;
import com.itineraryledger.kabengosafaris.Park.ParkType;
import com.itineraryledger.kabengosafaris.Park.Repositories.ParkImageRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for retrieving park images.
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class ParkImageGetService {

    private final ParkImageRepository parkImageRepository;
    private final com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final ParkImageStorageService storageService;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "imageType", "isPrimary", "isActive", "displayOrder", "fileSize", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public ParkImageGetService(
        ParkImageRepository parkImageRepository,
        ParkImageStorageService storageService,
        IdObfuscator idObfuscator,
        com.itineraryledger.kabengosafaris.Response.ListStats listStats
    ) {
        this.parkImageRepository = parkImageRepository;
        this.storageService = storageService;
        this.idObfuscator = idObfuscator;
        this.listStats = listStats;
    }

    public ParkImageDTO toDTO(ParkImage image) {
        String obfuscatedId = idObfuscator.encodeId(image.getId());
        return ParkImageDTO.builder()
            .id(obfuscatedId)
            .parkId(idObfuscator.encodeId(image.getPark().getId()))
            .parkName(image.getPark().getName())
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

    public ResponseEntity<?> getAllImages(
            String obfuscatedParkId,
            String parkName,
            ParkType parkType,
            String region,
            ImageType imageType,
            Boolean isPrimary,
            Boolean isActive,
            Boolean isWebActive,
            Integer displayOrder,
            java.util.List<ParkImage.ImageType> imageTypes,
            java.util.List<String> statuses,
            java.util.List<String> qualities,
            java.time.LocalDateTime createdAfter,
            java.time.LocalDateTime createdBefore,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        Specification<ParkImage> spec = Specification.unrestricted();

        if (obfuscatedParkId != null && !obfuscatedParkId.isBlank()) {
            try {
                Long parkId = idObfuscator.decodeId(obfuscatedParkId);
                spec = spec.and(ParkImageSpecification.byParkId(parkId));
            } catch (Exception e) {
                log.warn("Failed to decode park ID: {}", obfuscatedParkId);
            }
        }

        if (parkName != null && !parkName.isBlank()) {
            spec = spec.and(ParkImageSpecification.byParkName(parkName));
        }
        if (parkType != null) {
            spec = spec.and(ParkImageSpecification.byParkType(parkType));
        }
        if (region != null && !region.isBlank()) {
            spec = spec.and(ParkImageSpecification.byParkRegion(region));
        }
        if (imageType != null) {
            spec = spec.and(ParkImageSpecification.byImageType(imageType));
        }
        if (isPrimary != null) {
            spec = spec.and(ParkImageSpecification.byIsPrimary(isPrimary));
        }
        if (isActive != null) {
            spec = spec.and(ParkImageSpecification.byIsActive(isActive));
        }
        if (isWebActive != null) {
            spec = spec.and(ParkImageSpecification.isWebActive(isWebActive));
        }
        if (displayOrder != null) {
            spec = spec.and(ParkImageSpecification.byDisplayOrder(displayOrder));
        }

        // Sorting with validation

        // multi-value + data-quality facets: every stat card must be filterable
        if (imageTypes != null && !imageTypes.isEmpty()) {
            spec = spec.and(ParkImageSpecification.imageTypeIn(imageTypes));
        }
        if (statuses != null && !statuses.isEmpty()) {
            java.util.List<Boolean> states = new java.util.ArrayList<>();
            if (statuses.contains("active")) states.add(true);
            if (statuses.contains("inactive")) states.add(false);
            if (states.size() == 1) spec = spec.and(ParkImageSpecification.byIsActive(states.get(0)));
        }
        if (qualities != null && !qualities.isEmpty()) {
            spec = spec.and(ParkImageSpecification.anyQualityIssue(
                qualities.contains("no-caption"),
                qualities.contains("no-alt")
            ));
        }
        if (createdAfter != null) spec = spec.and(ParkImageSpecification.createdAfter(createdAfter));
        if (createdBefore != null) spec = spec.and(ParkImageSpecification.createdBefore(createdBefore));

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

        Page<ParkImage> imagePage = parkImageRepository.findAll(spec, pageable);

        List<ParkImageDTO> imageDTOs = imagePage.getContent().stream()
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
        response.put("validSortFields", VALID_SORT_FIELDS);
        response.put("currentSortBy", validatedSortBy);
        response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
        // counters share the SAME spec as the rows, so cards and table always agree
        response.put("stats", computeStats(spec));

        return ResponseEntity.ok(ApiResponse.success(200, "Park images retrieved successfully", response));
    }

    public ResponseEntity<?> getImageById(String obfuscatedId, String scopeParentId) {
        log.info("Getting park image with ID: {}", obfuscatedId);

        try {
            Long id = idObfuscator.decodeId(obfuscatedId);
            ParkImage image = parkImageRepository.findById(id).orElse(null);

            if (image == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Park image not found", "IMAGE_NOT_FOUND")
                );
            }

            ParkImageDTO imageDTO = toDTO(image);

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
                nextId = parkImageRepository.findNextIdByParent(id, decodedParentId).orElse(null);
                previousId = parkImageRepository.findPreviousIdByParent(id, decodedParentId).orElse(null);
                if (nextId == null) nextId = parkImageRepository.findFirstIdByParent(decodedParentId).orElse(null);
                if (previousId == null) previousId = parkImageRepository.findLastIdByParent(decodedParentId).orElse(null);
            } else {
                nextId = parkImageRepository.findNextId(id).orElse(null);
                previousId = parkImageRepository.findPreviousId(id).orElse(null);
                if (nextId == null) nextId = parkImageRepository.findFirstId().orElse(null);
                if (previousId == null) previousId = parkImageRepository.findLastId().orElse(null);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("image", imageDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("scopeParentId", scopeParentId);

            return ResponseEntity.ok(ApiResponse.success(200, "Park image retrieved successfully", response));

        } catch (Exception e) {
            log.warn("Failed to decode park image ID: {}", obfuscatedId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid image ID", "INVALID_IMAGE_ID")
            );
        }
    }

    public ResponseEntity<?> getImagesByParkId(String obfuscatedParkId) {
        log.info("Getting images for park: {}", obfuscatedParkId);

        try {
            Long parkId = idObfuscator.decodeId(obfuscatedParkId);
            List<ParkImage> images = parkImageRepository.findByParkIdOrderByDisplayOrderAsc(parkId);

            List<ParkImageDTO> imageDTOs = images.stream()
                .map(this::toDTO)
                .toList();

            return ResponseEntity.ok(ApiResponse.success(200, "Park images retrieved successfully", imageDTOs));

        } catch (Exception e) {
            log.warn("Failed to decode park ID: {}", obfuscatedParkId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid park ID", "INVALID_PARK_ID")
            );
        }
    }

    public ParkImage getImageByFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        return parkImageRepository.findByFileName(fileName).orElse(null);
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    /** Dashboard counters for the CURRENT filter set (see CLAUDE.md: stats on every list). */
    private Map<String, Object> computeStats(Specification<ParkImage> base) {
        return listStats.of(ParkImage.class, base)
            .total()
            .count("active", ParkImageSpecification.byIsActive(true))
            .complement("inactive", "active")
            .count("primary", ParkImageSpecification.byIsPrimary(true))
            .count("missingCaption", ParkImageSpecification.missingCaption())
            .count("missingAltText", ParkImageSpecification.missingAltText())
            .breakdown("byImageType", ImageType.values(), ParkImageSpecification::byImageType)
            .recency(ParkImageSpecification::createdAfter)
            .build();
    }
}
