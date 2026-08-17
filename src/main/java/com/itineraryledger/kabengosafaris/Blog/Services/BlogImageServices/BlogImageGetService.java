package com.itineraryledger.kabengosafaris.Blog.Services.BlogImageServices;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Blog.DTOs.BlogImageDTOs.BlogImageDTO;
import com.itineraryledger.kabengosafaris.Blog.Entity.BlogImage;
import com.itineraryledger.kabengosafaris.Blog.Repository.BlogImageRepository;
import com.itineraryledger.kabengosafaris.Blog.Specifications.BlogImageSpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.ListStats;
import com.itineraryledger.kabengosafaris.Response.RecordNavigation;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Reading blog images: the gallery, one image, and the file itself. */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlogImageGetService {

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "displayOrder", "isPrimary", "isActive", "fileSize", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "displayOrder";

    private final BlogImageRepository blogImageRepository;
    private final BlogImageStorageService storageService;
    private final IdObfuscator idObfuscator;
    private final ListStats listStats;
    private final RecordNavigation recordNavigation;

    /**
     * The ONE description of the filtered set, shared by the rows, the counters and the
     * record arrows.
     */
    private Specification<BlogImage> buildSpec(String blogIdObfuscated, Boolean isPrimary, Boolean isActive, String keyword) {
        Specification<BlogImage> spec = Specification.unrestricted();
        if (blogIdObfuscated != null && !blogIdObfuscated.isBlank()) {
            try {
                spec = spec.and(BlogImageSpecification.byBlogId(idObfuscator.decodeId(blogIdObfuscated)));
            } catch (Exception e) {
                log.warn("Invalid blog id on image list: {}", blogIdObfuscated);
            }
        }
        if (isPrimary != null) spec = spec.and(BlogImageSpecification.byIsPrimary(isPrimary));
        if (isActive != null) spec = spec.and(BlogImageSpecification.byIsActive(isActive));
        if (keyword != null && !keyword.isBlank()) spec = spec.and(BlogImageSpecification.searchKeyword(keyword));
        return spec;
    }

    public ResponseEntity<?> getAllImages(
        String blogId,
        Boolean isPrimary,
        Boolean isActive,
        String keyword,
        int page,
        int size,
        String sortBy,
        String sortDirection
    ) {
        try {
            Specification<BlogImage> spec = buildSpec(blogId, isPrimary, isActive, keyword);

            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(direction, validatedSortBy));
            Page<BlogImage> imagePage = blogImageRepository.findAll(spec, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("images", imagePage.getContent().stream().map(this::toDTO).collect(Collectors.toList()));
            response.put("currentPage", imagePage.getNumber());
            response.put("totalItems", imagePage.getTotalElements());
            response.put("totalPages", imagePage.getTotalPages());
            response.put("pageSize", imagePage.getSize());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", direction.name().toLowerCase());
            /* counters over the SAME specification as the rows, so they cannot disagree */
            response.put("stats", listStats.of(BlogImage.class, spec)
                .total()
                .count("active", BlogImageSpecification.byIsActive(true))
                .complement("inactive", "active")
                .count("primary", BlogImageSpecification.byIsPrimary(true))
                .count("missingAlt", BlogImageSpecification.missingAlt())
                .count("missingCaption", BlogImageSpecification.missingCaption())
                .build());

            return ResponseEntity.ok(ApiResponse.success(200, "Blog images retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error listing blog images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch blog images", "BLOG_IMAGES_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<?> getImageById(
        String obfuscatedId,
        String scopeParentId,
        String blogId,
        Boolean isPrimary,
        Boolean isActive,
        String keyword,
        String sortBy,
        String sortDirection
    ) {
        try {
            Long id = idObfuscator.decodeId(obfuscatedId);
            BlogImage image = blogImageRepository.findById(id).orElse(null);
            if (image == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Blog image not found", "IMAGE_NOT_FOUND")
                );
            }

            Long decodedParentId = null;
            if (scopeParentId != null && !scopeParentId.isEmpty()) {
                try {
                    decodedParentId = idObfuscator.decodeId(scopeParentId);
                } catch (Exception ex) {
                    log.warn("Invalid scopeParentId: {}, falling back to global navigation", scopeParentId);
                }
            }

            /* the arrows walk the caller's filtered set, scoped to the post when one is given */
            String validatedSortBy = validateSortField(sortBy);
            Map<String, Object> nav = recordNavigation.navigate(
                BlogImage.class,
                buildSpec(decodedParentId != null ? scopeParentId : blogId, isPrimary, isActive, keyword),
                validatedSortBy != null ? validatedSortBy : DEFAULT_SORT_FIELD,
                !"desc".equalsIgnoreCase(sortDirection),
                id
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("image", toDTO(image));
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));
            response.put("scopeParentId", scopeParentId);

            return ResponseEntity.ok(ApiResponse.success(200, "Blog image retrieved successfully", response));
        } catch (Exception e) {
            log.warn("Failed to read blog image id: {}", obfuscatedId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid image ID", "INVALID_IMAGE_ID")
            );
        }
    }

    /** Every image of one post, in display order — what the editor's gallery reads. */
    public ResponseEntity<ApiResponse<?>> getImagesForBlog(String blogIdObfuscated) {
        try {
            Long blogId = idObfuscator.decodeId(blogIdObfuscated);
            List<BlogImageDTO> dtos = blogImageRepository.findByBlogIdOrderByDisplayOrderAscIdAsc(blogId)
                .stream().map(this::toDTO).collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(200, "Blog images retrieved successfully", dtos));
        } catch (Exception e) {
            log.warn("Invalid blog id: {}", blogIdObfuscated);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid blog ID", "INVALID_BLOG_ID")
            );
        }
    }

    public BlogImage findEntity(Long id) {
        return blogImageRepository.findById(id).orElse(null);
    }

    public BlogImage findEntityByFileName(String fileName) {
        return blogImageRepository.findByFileName(fileName).orElse(null);
    }

    public BlogImageDTO toDTO(BlogImage image) {
        if (image == null) return null;
        String obfuscatedId = idObfuscator.encodeId(image.getId());
        return BlogImageDTO.builder()
            .id(obfuscatedId)
            .blogId(image.getBlog() != null ? idObfuscator.encodeId(image.getBlog().getId()) : null)
            .blogTitle(image.getBlog() != null ? image.getBlog().getTitle() : null)
            .fileName(image.getFileName())
            .originalFileName(image.getOriginalFileName())
            .altText(image.getAltText())
            .caption(image.getCaption())
            .description(image.getDescription())
            .isPrimary(image.getIsPrimary())
            .isActive(image.getIsActive())
            .displayOrder(image.getDisplayOrder())
            .fileSize(image.getFileSize())
            .fileSizeFormatted(storageService.formatFileSize(image.getFileSize()))
            .mimeType(image.getMimeType())
            .width(image.getWidth())
            .height(image.getHeight())
            .imageUrl(storageService.constructImageUrl(obfuscatedId))
            .fileImageUrl(storageService.constructFileUrl(image.getFileName()))
            .createdAt(image.getCreatedAt())
            .updatedAt(image.getUpdatedAt())
            .build();
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }
}
