package com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyImageServices;

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

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Testimony.DTOs.TestimonyImageDTOs.TestimonyImageDTO;
import com.itineraryledger.kabengosafaris.Testimony.Entity.TestimonyImage;
import com.itineraryledger.kabengosafaris.Testimony.Repository.TestimonyImageRepository;
import com.itineraryledger.kabengosafaris.Testimony.Specifications.TestimonyImageSpecification;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
public class TestimonyImageGetService {

    private final TestimonyImageRepository testimonyImageRepository;
    private final TestimonyImageStorageService storageService;
    private final IdObfuscator idObfuscator;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "isPrimary", "isActive", "displayOrder", "fileSize", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public TestimonyImageGetService(
        TestimonyImageRepository testimonyImageRepository,
        TestimonyImageStorageService storageService,
        IdObfuscator idObfuscator,
        com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation
    ) {
        this.testimonyImageRepository = testimonyImageRepository;
        this.storageService = storageService;
        this.idObfuscator = idObfuscator;
        this.recordNavigation = recordNavigation;
    }

    public ResponseEntity<ApiResponse<?>> getTestimonyImages(String testimonyId) {
        log.info("Fetching images for testimony: {}", testimonyId);

        try {
            Long decodedTestimonyId;
            try {
                decodedTestimonyId = idObfuscator.decodeId(testimonyId);
            } catch (Exception e) {
                log.warn("Invalid testimony ID format: {}", testimonyId);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid testimony ID format", "INVALID_TESTIMONY_ID")
                );
            }

            List<TestimonyImage> images = testimonyImageRepository.findByTestimonyIdAndIsActiveTrueOrderByDisplayOrderAsc(decodedTestimonyId);

            List<TestimonyImageDTO> imageDTOs = images.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Testimony images retrieved successfully", imageDTOs)
            );

        } catch (Exception e) {
            log.error("Error fetching testimony images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch testimony images", "TESTIMONY_IMAGES_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<?> getAllImages(
            String obfuscatedTestimonyId,
            Boolean isPrimary,
            Boolean isActive,
            String keyword,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        Specification<TestimonyImage> spec = buildSpec(obfuscatedTestimonyId, isPrimary, isActive, keyword);

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

        Page<TestimonyImage> imagePage = testimonyImageRepository.findAll(spec, pageable);

        List<TestimonyImageDTO> imageDTOs = imagePage.getContent().stream()
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

        return ResponseEntity.ok(ApiResponse.success(200, "Testimony images retrieved successfully", response));
    }

    /**
     * The ONE place the filter set is described, so the rows and the record arrows cannot
     * walk different sets (see CLAUDE.md: record paging must respect the list's filters).
     */
    private Specification<TestimonyImage> buildSpec(
        String obfuscatedTestimonyId,
        Boolean isPrimary,
        Boolean isActive,
        String keyword
    ) {
        Specification<TestimonyImage> spec = Specification.unrestricted();

        if (obfuscatedTestimonyId != null && !obfuscatedTestimonyId.isBlank()) {
            try {
                Long testimonyId = idObfuscator.decodeId(obfuscatedTestimonyId);
                spec = spec.and(TestimonyImageSpecification.byTestimonyId(testimonyId));
            } catch (Exception e) {
                log.warn("Failed to decode testimony ID: {}", obfuscatedTestimonyId);
            }
        }

        if (isPrimary != null) spec = spec.and(TestimonyImageSpecification.byIsPrimary(isPrimary));
        if (isActive != null) spec = spec.and(TestimonyImageSpecification.byIsActive(isActive));
        if (keyword != null && !keyword.isBlank()) spec = spec.and(TestimonyImageSpecification.searchKeyword(keyword));

        return spec;
    }

    public ResponseEntity<?> getImageById(
        String obfuscatedId,
        String scopeParentId,
        Boolean isPrimary,
        Boolean isActive,
        String keyword,
        String sortBy,
        String sortDirection
    ) {
        log.info("Getting testimony image with ID: {}", obfuscatedId);

        try {
            Long id = idObfuscator.decodeId(obfuscatedId);
            TestimonyImage image = testimonyImageRepository.findById(id).orElse(null);

            if (image == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Testimony image not found", "IMAGE_NOT_FOUND")
                );
            }

            TestimonyImageDTO imageDTO = toDTO(image);

            // Decode optional scope parent ID for scoped navigation
            Long decodedParentId = null;
            if (scopeParentId != null && !scopeParentId.isEmpty()) {
                try {
                    decodedParentId = idObfuscator.decodeId(scopeParentId);
                } catch (Exception ex) {
                    log.warn("Invalid scopeParentId: {}, falling back to global navigation", scopeParentId);
                }
            }

            /*
             * Circular navigation over the caller's filtered, sorted set — scoped to the
             * parent when one is given. The id-ordered repository walk this replaces stepped
             * through a different set from the one on screen and could not say where you were.
             */
            String validatedSortBy = validateSortField(sortBy);
            java.util.Map<String, Object> nav = recordNavigation.navigate(
                TestimonyImage.class,
                buildSpec(
                    decodedParentId != null ? scopeParentId : null,
                    isPrimary,
                    isActive,
                    keyword
                ),
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
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));
            response.put("scopeParentId", scopeParentId);

            return ResponseEntity.ok(ApiResponse.success(200, "Testimony image retrieved successfully", response));

        } catch (Exception e) {
            log.warn("Failed to decode testimony image ID: {}", obfuscatedId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid image ID", "INVALID_IMAGE_ID")
            );
        }
    }

    public TestimonyImage getImageByFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) return null;
        return testimonyImageRepository.findByFileName(fileName).orElse(null);
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    public TestimonyImageDTO toDTO(TestimonyImage image) {
        String obfuscatedId = idObfuscator.encodeId(image.getId());

        return TestimonyImageDTO.builder()
            .id(obfuscatedId)
            .testimonyId(idObfuscator.encodeId(image.getTestimony().getId()))
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
