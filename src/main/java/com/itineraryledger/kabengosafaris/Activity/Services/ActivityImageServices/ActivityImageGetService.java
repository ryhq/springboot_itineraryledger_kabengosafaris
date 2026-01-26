package com.itineraryledger.kabengosafaris.Activity.Services.ActivityImageServices;

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

import com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityImageDTOs.ActivityImageDTO;
import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityImage;
import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityImage.ImageType;
import com.itineraryledger.kabengosafaris.Activity.Repositories.ActivityImageRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for retrieving activity images.
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class ActivityImageGetService {

    private final ActivityImageRepository activityImageRepository;
    private final ActivityImageStorageService storageService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ActivityImageGetService(
        ActivityImageRepository activityImageRepository,
        ActivityImageStorageService storageService,
        IdObfuscator idObfuscator
    ) {
        this.activityImageRepository = activityImageRepository;
        this.storageService = storageService;
        this.idObfuscator = idObfuscator;
    }

    public ActivityImageDTO toDTO(ActivityImage image) {
        String obfuscatedId = idObfuscator.encodeId(image.getId());
        return ActivityImageDTO.builder()
            .id(obfuscatedId)
            .activityId(idObfuscator.encodeId(image.getActivity().getId()))
            .activityName(image.getActivity().getName())
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
            String obfuscatedActivityId,
            String activityName,
            Boolean activityIsActive,
            Boolean hasTariff,
            ImageType imageType,
            Boolean isPrimary,
            Boolean isActive,
            Integer displayOrder,
            int page,
            int size,
            String sortDirection
    ) {
        Specification<ActivityImage> spec = Specification.unrestricted();

        if (obfuscatedActivityId != null && !obfuscatedActivityId.isBlank()) {
            try {
                Long activityId = idObfuscator.decodeId(obfuscatedActivityId);
                spec = spec.and(ActivityImageSpecification.byActivityId(activityId));
            } catch (Exception e) {
                log.warn("Failed to decode activity ID: {}", obfuscatedActivityId);
            }
        }

        if (activityName != null && !activityName.isBlank()) {
            spec = spec.and(ActivityImageSpecification.byActivityName(activityName));
        }
        if (activityIsActive != null) {
            spec = spec.and(ActivityImageSpecification.byActivityIsActive(activityIsActive));
        }
        if (hasTariff != null) {
            spec = spec.and(ActivityImageSpecification.byActivityHasTariff(hasTariff));
        }
        if (imageType != null) {
            spec = spec.and(ActivityImageSpecification.byImageType(imageType));
        }
        if (isPrimary != null) {
            spec = spec.and(ActivityImageSpecification.byIsPrimary(isPrimary));
        }
        if (isActive != null) {
            spec = spec.and(ActivityImageSpecification.byIsActive(isActive));
        }
        if (displayOrder != null) {
            spec = spec.and(ActivityImageSpecification.byDisplayOrder(displayOrder));
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"));

        Page<ActivityImage> imagePage = activityImageRepository.findAll(spec, pageable);

        List<ActivityImageDTO> imageDTOs = imagePage.getContent().stream()
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

        return ResponseEntity.ok(ApiResponse.success(200, "Activity images retrieved successfully", response));
    }

    public ResponseEntity<?> getImageById(String obfuscatedId) {
        log.info("Getting activity image with ID: {}", obfuscatedId);

        try {
            Long id = idObfuscator.decodeId(obfuscatedId);
            ActivityImage image = activityImageRepository.findById(id).orElse(null);

            if (image == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Activity image not found", "IMAGE_NOT_FOUND")
                );
            }

            return ResponseEntity.ok(ApiResponse.success(200, "Activity image retrieved successfully", toDTO(image)));

        } catch (Exception e) {
            log.warn("Failed to decode activity image ID: {}", obfuscatedId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid image ID", "INVALID_IMAGE_ID")
            );
        }
    }

    public ResponseEntity<?> getImagesByActivityId(String obfuscatedActivityId) {
        log.info("Getting images for activity: {}", obfuscatedActivityId);

        try {
            Long activityId = idObfuscator.decodeId(obfuscatedActivityId);
            List<ActivityImage> images = activityImageRepository.findByActivityIdOrderByDisplayOrderAsc(activityId);

            List<ActivityImageDTO> imageDTOs = images.stream()
                .map(this::toDTO)
                .toList();

            return ResponseEntity.ok(ApiResponse.success(200, "Activity images retrieved successfully", imageDTOs));

        } catch (Exception e) {
            log.warn("Failed to decode activity ID: {}", obfuscatedActivityId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid activity ID", "INVALID_ACTIVITY_ID")
            );
        }
    }

    public ActivityImage getImageByFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        return activityImageRepository.findByFileName(fileName).orElse(null);
    }
}
