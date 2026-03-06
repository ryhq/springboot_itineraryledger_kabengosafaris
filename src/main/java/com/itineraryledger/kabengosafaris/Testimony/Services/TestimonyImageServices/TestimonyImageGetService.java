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

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "isPrimary", "isActive", "displayOrder", "fileSize", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public TestimonyImageGetService(
        TestimonyImageRepository testimonyImageRepository,
        TestimonyImageStorageService storageService,
        IdObfuscator idObfuscator
    ) {
        this.testimonyImageRepository = testimonyImageRepository;
        this.storageService = storageService;
        this.idObfuscator = idObfuscator;
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
        response.put("currentSortDir", sortDirection != null ? sortDirection : "desc");

        return ResponseEntity.ok(ApiResponse.success(200, "Testimony images retrieved successfully", response));
    }

    public ResponseEntity<?> getImageById(String obfuscatedId) {
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

            // Circular navigation
            Long nextId = testimonyImageRepository.findNextId(id).orElse(null);
            Long previousId = testimonyImageRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = testimonyImageRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = testimonyImageRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("image", imageDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

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
