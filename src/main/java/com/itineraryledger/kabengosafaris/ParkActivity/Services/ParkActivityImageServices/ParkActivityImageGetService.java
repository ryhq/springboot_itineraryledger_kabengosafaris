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

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "isPrimary", "isActive", "displayOrder", "fileSize", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public ParkActivityImageGetService(
        ParkActivityImageRepository parkActivityImageRepository,
        ParkActivityImageStorageService storageService,
        IdObfuscator idObfuscator
    ) {
        this.parkActivityImageRepository = parkActivityImageRepository;
        this.storageService = storageService;
        this.idObfuscator = idObfuscator;
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

        return ResponseEntity.ok(ApiResponse.success(200, "Park activity images retrieved successfully", response));
    }

    public ResponseEntity<?> getImageById(String obfuscatedId) {
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

            // Circular navigation
            Long nextId = parkActivityImageRepository.findNextId(id).orElse(null);
            Long previousId = parkActivityImageRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = parkActivityImageRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = parkActivityImageRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("image", imageDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

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
