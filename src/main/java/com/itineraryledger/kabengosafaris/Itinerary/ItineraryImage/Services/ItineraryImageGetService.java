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
    private final com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "imageType", "isPrimary", "isActive", "displayOrder", "fileSize", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public ItineraryImageGetService(
        ItineraryImageRepository itineraryImageRepository,
        ItineraryImageStorageService storageService,
        IdObfuscator idObfuscator,
        com.itineraryledger.kabengosafaris.Response.ListStats listStats,
        com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation
    ) {
        this.itineraryImageRepository = itineraryImageRepository;
        this.storageService = storageService;
        this.idObfuscator = idObfuscator;
        this.listStats = listStats;
        this.recordNavigation = recordNavigation;
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
            List<ImageType> imageTypes,
            List<String> statuses,
            List<String> visibilities,
            List<String> qualities,
            java.time.LocalDateTime createdAfter,
            String keyword,
            Boolean includeStats,
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

        /*
         * The multi-value facets. Each one is also a stat card, and a card that
         * cannot be clicked is decoration. Contradictory pairs (active plus
         * inactive) cancel to no constraint, because that is what they mean.
         */
        spec = spec.and(ItineraryImageSpecification.byImageTypes(imageTypes));

        if (statuses != null && !statuses.isEmpty()) {
            List<Boolean> states = new java.util.ArrayList<>();
            if (statuses.contains("active")) states.add(true);
            if (statuses.contains("inactive")) states.add(false);
            if (states.size() == 1) spec = spec.and(ItineraryImageSpecification.byIsActive(states.get(0)));
        }
        if (visibilities != null && !visibilities.isEmpty()) {
            List<Boolean> states = new java.util.ArrayList<>();
            if (visibilities.contains("live")) states.add(true);
            if (visibilities.contains("hidden")) states.add(false);
            if (states.size() == 1) spec = spec.and(ItineraryImageSpecification.isWebActive(states.get(0)));
        }
        if (qualities != null && !qualities.isEmpty()) {
            List<Specification<ItineraryImage>> any = new java.util.ArrayList<>();
            if (qualities.contains("no-caption")) any.add(ItineraryImageSpecification.missingCaption());
            if (qualities.contains("no-alt")) any.add(ItineraryImageSpecification.missingAltText());
            if (!any.isEmpty()) {
                Specification<ItineraryImage> combined = any.get(0);
                for (int i = 1; i < any.size(); i++) combined = combined.or(any.get(i));
                spec = spec.and(combined);
            }
        }
        if (createdAfter != null) {
            spec = spec.and(ItineraryImageSpecification.createdAfter(createdAfter));
        }
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(ItineraryImageSpecification.searchKeyword(keyword));
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
        /*
         * Counters for the WHOLE filtered set, from the same specification as the
         * rows. Without them the page can only summarise what it loaded, and the
         * "All filtered / This page" toggle has to stay hidden.
         */
        if (includeStats == null || includeStats) {
            response.put("stats", computeStats(spec));
        }

        return ResponseEntity.ok(ApiResponse.success(200, "Itinerary images retrieved successfully", response));
    }

    /** Dashboard counters for the current filter set. */
    private Map<String, Object> computeStats(Specification<ItineraryImage> base) {
        return listStats.of(ItineraryImage.class, base)
            .total()
            .count("active", ItineraryImageSpecification.byIsActive(true))
            .complement("inactive", "active")
            .count("primary", ItineraryImageSpecification.byIsPrimary(true))
            .count("webActive", ItineraryImageSpecification.isWebActive(true))
            .complement("webHidden", "webActive")
            .count("missingCaption", ItineraryImageSpecification.missingCaption())
            .count("missingAltText", ItineraryImageSpecification.missingAltText())
            .recency(ItineraryImageSpecification::createdAfter)
            .build();
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

            /*
             * Prev/next walks the SAME set the caller came from — this itinerary's
             * images when scoped, everything otherwise — and returns the position,
             * so the record page can show 'N of M' and the wrap is visible. The id
             * walk it replaced could not say where you were.
             */
            Specification<ItineraryImage> navSpec = decodedParentId != null
                ? ItineraryImageSpecification.byItineraryId(decodedParentId)
                : Specification.unrestricted();

            Map<String, Object> nav = recordNavigation.navigate(
                ItineraryImage.class, navSpec, "displayOrder", true, id);
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("image", imageDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));
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
