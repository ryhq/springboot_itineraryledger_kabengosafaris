package com.itineraryledger.kabengosafaris.ParkActivity.Services.ParkActivityImageServices;

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

import com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityImageDTOs.ParkActivityImageDTO;
import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityImage;
import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityImage.ImageType;
import com.itineraryledger.kabengosafaris.ParkActivity.Repositories.ParkActivityImageRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for retrieving park activity images.
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class ParkActivityImageGetService {

    private final ParkActivityImageRepository parkActivityImageRepository;
    private final ParkActivityImageStorageService storageService;
    private final IdObfuscator idObfuscator;
    private final com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "isPrimary", "isActive", "displayOrder", "fileSize", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public ParkActivityImageGetService(
        com.itineraryledger.kabengosafaris.Response.ListStats listStats,
        com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation,
        ParkActivityImageRepository parkActivityImageRepository,
        ParkActivityImageStorageService storageService,
        IdObfuscator idObfuscator
    ) {
        this.parkActivityImageRepository = parkActivityImageRepository;
        this.storageService = storageService;
        this.idObfuscator = idObfuscator;
        this.listStats = listStats;
        this.recordNavigation = recordNavigation;
    }

    public ParkActivityImageDTO toDTO(ParkActivityImage image) {
        String obfuscatedId = idObfuscator.encodeId(image.getId());
        return ParkActivityImageDTO.builder()
            .id(obfuscatedId)
            .parkId(idObfuscator.encodeId(image.getParkActivity().getPark().getId()))
            .parkName(image.getParkActivity().getPark().getName())
            .activityId(idObfuscator.encodeId(image.getParkActivity().getActivity().getId()))
            .activityName(image.getParkActivity().getActivity().getName())
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
            String obfuscatedActivityId,
            String parkName,
            String activityName,
            Boolean parkIsActive,
            Boolean activityIsActive,
            Boolean hasTariff,
            ImageType imageType,
            Boolean isPrimary,
            Boolean isActive,
            Integer displayOrder,
            java.util.List<ImageType> imageTypes,
            java.util.List<String> statuses,
            java.util.List<String> qualities,
            java.time.LocalDateTime createdAfter,
            java.time.LocalDateTime createdBefore,
            String keyword,
            Boolean includeStats,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        Specification<ParkActivityImage> spec = Specification.unrestricted();

        // Filter by park ID
        if (obfuscatedParkId != null && !obfuscatedParkId.isBlank()) {
            try {
                Long parkId = idObfuscator.decodeId(obfuscatedParkId);
                spec = spec.and(ParkActivityImageSpecification.byParkId(parkId));
            } catch (Exception e) {
                log.warn("Failed to decode park ID: {}", obfuscatedParkId);
            }
        }

        // Filter by activity ID
        if (obfuscatedActivityId != null && !obfuscatedActivityId.isBlank()) {
            try {
                Long activityId = idObfuscator.decodeId(obfuscatedActivityId);
                spec = spec.and(ParkActivityImageSpecification.byActivityId(activityId));
            } catch (Exception e) {
                log.warn("Failed to decode activity ID: {}", obfuscatedActivityId);
            }
        }

        if (parkName != null && !parkName.isBlank()) {
            spec = spec.and(ParkActivityImageSpecification.byParkName(parkName));
        }
        if (activityName != null && !activityName.isBlank()) {
            spec = spec.and(ParkActivityImageSpecification.byActivityName(activityName));
        }
        if (parkIsActive != null) {
            spec = spec.and(ParkActivityImageSpecification.byParkIsActive(parkIsActive));
        }
        if (activityIsActive != null) {
            spec = spec.and(ParkActivityImageSpecification.byActivityIsActive(activityIsActive));
        }
        if (hasTariff != null) {
            spec = spec.and(ParkActivityImageSpecification.byActivityHasTariff(hasTariff));
        }
        if (imageType != null) {
            spec = spec.and(ParkActivityImageSpecification.byImageType(imageType));
        }
        if (isPrimary != null) {
            spec = spec.and(ParkActivityImageSpecification.byIsPrimary(isPrimary));
        }
        if (isActive != null) {
            spec = spec.and(ParkActivityImageSpecification.byIsActive(isActive));
        }
        if (displayOrder != null) {
            spec = spec.and(ParkActivityImageSpecification.byDisplayOrder(displayOrder));
        }

        // multi-value facets: every stat card must be reachable as a filter
        if (imageTypes != null && !imageTypes.isEmpty()) {
            spec = spec.and(ParkActivityImageSpecification.imageTypeIn(imageTypes));
        }
        if (statuses != null && !statuses.isEmpty()) {
            java.util.List<Boolean> states = new java.util.ArrayList<>();
            if (statuses.contains("active")) states.add(true);
            if (statuses.contains("inactive")) states.add(false);
            // active+inactive is every row, so it cancels to no constraint
            if (states.size() == 1) spec = spec.and(ParkActivityImageSpecification.byIsActive(states.get(0)));
        }
        if (qualities != null && !qualities.isEmpty()) {
            spec = spec.and(ParkActivityImageSpecification.anyQualityIssue(
                qualities.contains("no-caption"),
                qualities.contains("no-alt")
            ));
        }
        if (createdAfter != null) spec = spec.and(ParkActivityImageSpecification.createdAfter(createdAfter));
        if (createdBefore != null) spec = spec.and(ParkActivityImageSpecification.createdBefore(createdBefore));
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(ParkActivityImageSpecification.searchKeyword(keyword));
        }

        // Sorting with validation
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

        Page<ParkActivityImage> imagePage = parkActivityImageRepository.findAll(spec, pageable);

        List<ParkActivityImageDTO> imageDTOs = imagePage.getContent().stream()
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
        // counters built from the SAME spec as the rows, so cards and table agree
        if (includeStats == null || includeStats) {
            response.put("stats", computeStats(spec));
        }

        return ResponseEntity.ok(ApiResponse.success(200, "Park activity images retrieved successfully", response));
    }

    /**
     * The filter chain the record pager walks — the same dimensions the list
     * offers, so paging from a filtered list stays inside those matches.
     */
    private org.springframework.data.jpa.domain.Specification<ParkActivityImage> navigationSpec(
        String obfuscatedParkId,
        String obfuscatedActivityId,
        java.util.List<ImageType> imageTypes,
        java.util.List<String> statuses,
        java.util.List<String> qualities,
        java.time.LocalDateTime createdAfter,
        String keyword
    ) {
        org.springframework.data.jpa.domain.Specification<ParkActivityImage> spec =
            org.springframework.data.jpa.domain.Specification.unrestricted();
        if (obfuscatedParkId != null && !obfuscatedParkId.isBlank()) {
            try {
                spec = spec.and(ParkActivityImageSpecification.byParkId(idObfuscator.decodeId(obfuscatedParkId)));
            } catch (Exception ignored) { /* an unreadable id just means no park filter */ }
        }
        if (obfuscatedActivityId != null && !obfuscatedActivityId.isBlank()) {
            try {
                spec = spec.and(ParkActivityImageSpecification.byActivityId(idObfuscator.decodeId(obfuscatedActivityId)));
            } catch (Exception ignored) { /* likewise */ }
        }
        if (imageTypes != null && !imageTypes.isEmpty()) {
            spec = spec.and(ParkActivityImageSpecification.imageTypeIn(imageTypes));
        }
        if (statuses != null && !statuses.isEmpty()) {
            java.util.List<Boolean> states = new java.util.ArrayList<>();
            if (statuses.contains("active")) states.add(true);
            if (statuses.contains("inactive")) states.add(false);
            if (states.size() == 1) spec = spec.and(ParkActivityImageSpecification.byIsActive(states.get(0)));
        }
        if (qualities != null && !qualities.isEmpty()) {
            spec = spec.and(ParkActivityImageSpecification.anyQualityIssue(
                qualities.contains("no-caption"), qualities.contains("no-alt")));
        }
        if (createdAfter != null) spec = spec.and(ParkActivityImageSpecification.createdAfter(createdAfter));
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(ParkActivityImageSpecification.searchKeyword(keyword));
        }
        return spec;
    }

    /** Dashboard counters for the CURRENT filter set (see CLAUDE.md: stats on every list). */
    private Map<String, Object> computeStats(org.springframework.data.jpa.domain.Specification<ParkActivityImage> base) {
        return listStats.of(ParkActivityImage.class, base)
            .total()
            .count("active", ParkActivityImageSpecification.byIsActive(true))
            .complement("inactive", "active")
            .count("primary", ParkActivityImageSpecification.byIsPrimary(true))
            .count("missingCaption", ParkActivityImageSpecification.missingCaption())
            .count("missingAltText", ParkActivityImageSpecification.missingAltText())
            .breakdown("byImageType", ParkActivityImage.ImageType.values(), ParkActivityImageSpecification::byImageType)
            .recency(ParkActivityImageSpecification::createdAfter)
            .build();
    }

    public ResponseEntity<?> getImageById(String obfuscatedId) {
        return getImageById(obfuscatedId, null, null, null, null, null, null, null, null, null);
    }

    /**
     * One image, plus where it sits in the set the caller was looking at.
     *
     * The arrows MUST walk the same filtered and sorted list the table showed —
     * paging into records the user never saw is worse than no arrows — so the
     * caller's filters come back in and rebuild that exact Specification.
     */
    public ResponseEntity<?> getImageById(
            String obfuscatedId,
            String obfuscatedParkId,
            String obfuscatedActivityId,
            java.util.List<ImageType> imageTypes,
            java.util.List<String> statuses,
            java.util.List<String> qualities,
            java.time.LocalDateTime createdAfter,
            String keyword,
            String sortBy,
            String sortDirection
    ) {
        log.info("Getting park activity image with ID: {}", obfuscatedId);

        try {
            Long id = idObfuscator.decodeId(obfuscatedId);
            ParkActivityImage image = parkActivityImageRepository.findById(id).orElse(null);

            if (image == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Park activity image not found", "IMAGE_NOT_FOUND")
                );
            }

            ParkActivityImageDTO imageDTO = toDTO(image);

            String validatedSortBy = validateSortField(sortBy);
            java.util.Map<String, Object> nav = recordNavigation.navigate(
                ParkActivityImage.class,
                navigationSpec(obfuscatedParkId, obfuscatedActivityId, imageTypes, statuses, qualities, createdAfter, keyword),
                validatedSortBy != null ? validatedSortBy : DEFAULT_SORT_FIELD,
                "asc".equalsIgnoreCase(sortDirection),
                id
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("image", imageDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            // the "N of M" readout makes the wraparound visible
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(ApiResponse.success(200, "Park activity image retrieved successfully", response));

        } catch (Exception e) {
            log.warn("Failed to decode park activity image ID: {}", obfuscatedId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid image ID", "INVALID_IMAGE_ID")
            );
        }
    }

    public ResponseEntity<?> getImagesByParkActivity(String obfuscatedParkId, String obfuscatedActivityId) {
        log.info("Getting images for park-activity: parkId={}, activityId={}", obfuscatedParkId, obfuscatedActivityId);

        try {
            Long parkId = idObfuscator.decodeId(obfuscatedParkId);
            Long activityId = idObfuscator.decodeId(obfuscatedActivityId);

            List<ParkActivityImage> images = parkActivityImageRepository.findByParkActivityOrderByDisplayOrderAsc(parkId, activityId);

            List<ParkActivityImageDTO> imageDTOs = images.stream()
                .map(this::toDTO)
                .toList();

            return ResponseEntity.ok(ApiResponse.success(200, "Park activity images retrieved successfully", imageDTOs));

        } catch (Exception e) {
            log.warn("Failed to decode park or activity ID", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid park or activity ID", "INVALID_ID")
            );
        }
    }

    public ResponseEntity<?> getImagesByParkId(String obfuscatedParkId) {
        log.info("Getting images for park: {}", obfuscatedParkId);

        try {
            Long parkId = idObfuscator.decodeId(obfuscatedParkId);
            List<ParkActivityImage> images = parkActivityImageRepository.findByParkIdOrderByDisplayOrderAsc(parkId);

            List<ParkActivityImageDTO> imageDTOs = images.stream()
                .map(this::toDTO)
                .toList();

            return ResponseEntity.ok(ApiResponse.success(200, "Park activity images retrieved successfully", imageDTOs));

        } catch (Exception e) {
            log.warn("Failed to decode park ID: {}", obfuscatedParkId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid park ID", "INVALID_PARK_ID")
            );
        }
    }

    public ResponseEntity<?> getImagesByActivityId(String obfuscatedActivityId) {
        log.info("Getting images for activity: {}", obfuscatedActivityId);

        try {
            Long activityId = idObfuscator.decodeId(obfuscatedActivityId);
            List<ParkActivityImage> images = parkActivityImageRepository.findByActivityIdOrderByDisplayOrderAsc(activityId);

            List<ParkActivityImageDTO> imageDTOs = images.stream()
                .map(this::toDTO)
                .toList();

            return ResponseEntity.ok(ApiResponse.success(200, "Park activity images retrieved successfully", imageDTOs));

        } catch (Exception e) {
            log.warn("Failed to decode activity ID: {}", obfuscatedActivityId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid activity ID", "INVALID_ACTIVITY_ID")
            );
        }
    }

    public ParkActivityImage getImageByFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        return parkActivityImageRepository.findByFileName(fileName).orElse(null);
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }
}
