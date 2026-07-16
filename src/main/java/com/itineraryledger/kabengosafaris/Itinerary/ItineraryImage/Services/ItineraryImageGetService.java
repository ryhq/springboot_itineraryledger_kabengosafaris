package com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Services;

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

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.DTOs.ItineraryImageDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Entity.ItineraryImage;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Entity.ItineraryImage.ImageType;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Repository.ItineraryImageRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for retrieving itinerary images.
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class ItineraryImageGetService {

    private final ItineraryImageRepository itineraryImageRepository;
    private final ItineraryImageStorageService storageService;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "imageType", "isPrimary", "isActive", "displayOrder", "fileSize", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public ItineraryImageGetService(
        ItineraryImageRepository itineraryImageRepository,
        ItineraryImageStorageService storageService,
        IdObfuscator idObfuscator
    ) {
        this.itineraryImageRepository = itineraryImageRepository;
        this.storageService = storageService;
        this.idObfuscator = idObfuscator;
    }

    public ItineraryImageDTO toDTO(ItineraryImage image) {
        String obfuscatedId = idObfuscator.encodeId(image.getId());
        return ItineraryImageDTO.builder()
            .id(obfuscatedId)
            .itineraryId(idObfuscator.encodeId(image.getItinerary().getId()))
            .itineraryName(image.getItinerary().getName())
            .imageUrl(storageService.constructImageUrl(obfuscatedId))
            .fileImageUrl(storageService.constructFileImageUrl(image.getFileName()))
            .fileName(image.getFileName())
            .originalFileName(image.getOriginalFileName())
            .imageType(image.getImageType())
            .imageTypeDisplayName(image.getImageType() != null ? image.getImageType().getDisplayName() : null)
            .imageTypeDescription(image.getImageType() != null ? image.getImageType().getDescription() : null)
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
            String obfuscatedItineraryId,
            String itineraryName,
            ImageType imageType,
            Boolean isPrimary,
            Boolean isActive,
            Boolean isWebActive,
            Integer displayOrder,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        Specification<ItineraryImage> spec = Specification.unrestricted();

        if (obfuscatedItineraryId != null && !obfuscatedItineraryId.isBlank()) {
            try {
                Long itineraryId = idObfuscator.decodeId(obfuscatedItineraryId);
                spec = spec.and(ItineraryImageSpecification.byItineraryId(itineraryId));
            } catch (Exception e) {
                log.warn("Failed to decode itinerary ID: {}", obfuscatedItineraryId);
            }
        }
        if (itineraryName != null && !itineraryName.isBlank()) {
            spec = spec.and(ItineraryImageSpecification.byItineraryName(itineraryName));
        }
        if (imageType != null) {
            spec = spec.and(ItineraryImageSpecification.byImageType(imageType));
        }
        if (isPrimary != null) {
            spec = spec.and(ItineraryImageSpecification.byIsPrimary(isPrimary));
        }
        if (isActive != null) {
            spec = spec.and(ItineraryImageSpecification.byIsActive(isActive));
        }
        if (isWebActive != null) {
            spec = spec.and(ItineraryImageSpecification.isWebActive(isWebActive));
        }
        if (displayOrder != null) {
            spec = spec.and(ItineraryImageSpecification.byDisplayOrder(displayOrder));
        }

        String validatedSortBy = validateSortField(sortBy);
        if (validatedSortBy == null) {
            log.warn("Invalid sort field: {}", sortBy);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
            );
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validatedSortBy));

        Page<ItineraryImage> imagePage = itineraryImageRepository.findAll(spec, pageable);

        List<ItineraryImageDTO> imageDTOs = imagePage.getContent().stream().map(this::toDTO).toList();

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

        return ResponseEntity.ok(ApiResponse.success(200, "Itinerary images retrieved successfully", response));
    }

    public ResponseEntity<?> getImageById(String obfuscatedId, String scopeParentId) {
        log.info("Getting itinerary image with ID: {}", obfuscatedId);
        try {
            Long id = idObfuscator.decodeId(obfuscatedId);
            ItineraryImage image = itineraryImageRepository.findById(id).orElse(null);
            if (image == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary image not found", "IMAGE_NOT_FOUND")
                );
            }

            ItineraryImageDTO imageDTO = toDTO(image);

            Long decodedParentId = null;
            if (scopeParentId != null && !scopeParentId.isEmpty()) {
                try {
                    decodedParentId = idObfuscator.decodeId(scopeParentId);
                } catch (Exception ex) {
                    log.warn("Invalid scopeParentId: {}, falling back to global navigation", scopeParentId);
                }
            }

            Long nextId, previousId;
            if (decodedParentId != null) {
                nextId = itineraryImageRepository.findNextIdByParent(id, decodedParentId).orElse(null);
                previousId = itineraryImageRepository.findPreviousIdByParent(id, decodedParentId).orElse(null);
                if (nextId == null) nextId = itineraryImageRepository.findFirstIdByParent(decodedParentId).orElse(null);
                if (previousId == null) previousId = itineraryImageRepository.findLastIdByParent(decodedParentId).orElse(null);
            } else {
                nextId = itineraryImageRepository.findNextId(id).orElse(null);
                previousId = itineraryImageRepository.findPreviousId(id).orElse(null);
                if (nextId == null) nextId = itineraryImageRepository.findFirstId().orElse(null);
                if (previousId == null) previousId = itineraryImageRepository.findLastId().orElse(null);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("image", imageDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("scopeParentId", scopeParentId);

            return ResponseEntity.ok(ApiResponse.success(200, "Itinerary image retrieved successfully", response));
        } catch (Exception e) {
            log.warn("Failed to decode itinerary image ID: {}", obfuscatedId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid image ID", "INVALID_IMAGE_ID")
            );
        }
    }

    public ResponseEntity<?> getImagesByItineraryId(String obfuscatedItineraryId) {
        log.info("Getting images for itinerary: {}", obfuscatedItineraryId);
        try {
            Long itineraryId = idObfuscator.decodeId(obfuscatedItineraryId);
            List<ItineraryImage> images = itineraryImageRepository.findByItineraryIdOrderByDisplayOrderAsc(itineraryId);
            List<ItineraryImageDTO> imageDTOs = images.stream().map(this::toDTO).toList();
            return ResponseEntity.ok(ApiResponse.success(200, "Itinerary images retrieved successfully", imageDTOs));
        } catch (Exception e) {
            log.warn("Failed to decode itinerary ID: {}", obfuscatedItineraryId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid itinerary ID", "INVALID_ITINERARY_ID")
            );
        }
    }

    public ItineraryImage getImageByFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) return null;
        return itineraryImageRepository.findByFileName(fileName).orElse(null);
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }
}
