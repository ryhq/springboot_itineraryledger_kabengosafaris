package com.itineraryledger.kabengosafaris.Blog.Services.BlogImageServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Blog.DTOs.BlogImageDTOs.BlogImageDTO;
import com.itineraryledger.kabengosafaris.Blog.DTOs.BlogImageDTOs.CreateBlogImageDTO;
import com.itineraryledger.kabengosafaris.Blog.Entity.Blog;
import com.itineraryledger.kabengosafaris.Blog.Entity.BlogImage;
import com.itineraryledger.kabengosafaris.Blog.Repository.BlogImageRepository;
import com.itineraryledger.kabengosafaris.Blog.Repository.BlogRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Uploading images to a post.
 *
 * Everything is validated BEFORE anything is written, and if a later file fails the files
 * already saved are removed again — a half-finished upload leaves rows pointing at nothing,
 * which is worse than a clean failure.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BlogImageCreateService {

    private final BlogImageRepository blogImageRepository;
    private final BlogRepository blogRepository;
    private final BlogImageStorageService storageService;
    private final BlogImageGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(action = "CREATE_BLOG_IMAGES", description = "Uploading blog images", entityType = "BlogImage")
    public ResponseEntity<ApiResponse<?>> uploadImages(List<CreateBlogImageDTO> imageDTOs) {
        log.info("Uploading {} blog images", imageDTOs != null ? imageDTOs.size() : 0);

        if (imageDTOs == null || imageDTOs.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "No images provided", "NO_IMAGES_PROVIDED")
            );
        }

        long totalSize = imageDTOs.stream()
            .filter(dto -> dto.getImage() != null)
            .mapToLong(dto -> dto.getImage().getSize())
            .sum();
        String requestSizeError = storageService.validateRequestSize(totalSize);
        if (requestSizeError != null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, requestSizeError, "REQUEST_SIZE_EXCEEDED")
            );
        }

        List<String> validationErrors = new ArrayList<>();
        for (int i = 0; i < imageDTOs.size(); i++) {
            CreateBlogImageDTO dto = imageDTOs.get(i);
            if (dto.getBlogId() == null || dto.getBlogId().isBlank()) {
                validationErrors.add(String.format("Image %d: Blog ID is required", i + 1));
                continue;
            }
            if (dto.getImage() == null || dto.getImage().isEmpty()) {
                validationErrors.add(String.format("Image %d: Image file is required", i + 1));
                continue;
            }
            String imageError = storageService.validateImage(dto.getImage());
            if (imageError != null) {
                String filename = dto.getImage().getOriginalFilename() != null ? dto.getImage().getOriginalFilename() : "unknown";
                validationErrors.add(String.format("Image %d (%s): %s", i + 1, filename, imageError));
            }
            try {
                if (!blogRepository.existsById(idObfuscator.decodeId(dto.getBlogId()))) {
                    validationErrors.add(String.format("Image %d: Blog post not found", i + 1));
                }
            } catch (Exception e) {
                validationErrors.add(String.format("Image %d: Invalid blog ID", i + 1));
            }
        }

        if (!validationErrors.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Validation failed: " + String.join("; ", validationErrors), "VALIDATION_ERROR")
            );
        }

        List<BlogImageDTO> created = new ArrayList<>();
        List<String> savedFileNames = new ArrayList<>();
        Map<Long, Integer> nextOrder = new HashMap<>();

        try {
            for (CreateBlogImageDTO dto : imageDTOs) {
                Long blogId = idObfuscator.decodeId(dto.getBlogId());
                Blog blog = blogRepository.findById(blogId).orElse(null);
                if (blog == null) continue;

                int displayOrder = nextOrder.containsKey(blogId)
                    ? nextOrder.get(blogId) + 1
                    : safeOrder(blogImageRepository.findMaxDisplayOrderInBlog(blogId)) + 1;
                nextOrder.put(blogId, displayOrder);

                String storedName = storageService.saveImage(dto.getImage());
                if (storedName == null) {
                    throw new IllegalStateException("Could not store " + dto.getImage().getOriginalFilename());
                }
                savedFileNames.add(storedName);

                /* the first image of a post is its cover unless somebody says otherwise */
                boolean isFirstEver = blogImageRepository.countByBlogId(blogId) == 0 && created.isEmpty();
                boolean wantsPrimary = Boolean.TRUE.equals(dto.getIsPrimary()) || isFirstEver;

                BlogImage image = BlogImage.builder()
                    .blog(blog)
                    .fileName(storedName)
                    .originalFileName(dto.getImage().getOriginalFilename())
                    .altText(dto.getAltText())
                    .caption(dto.getCaption())
                    .description(dto.getDescription())
                    .isPrimary(wantsPrimary)
                    .isActive(true)
                    .displayOrder(displayOrder)
                    .fileSize(dto.getImage().getSize())
                    .mimeType(dto.getImage().getContentType() != null
                        ? dto.getImage().getContentType()
                        : storageService.detectMimeType(dto.getImage().getOriginalFilename()))
                    .build();

                image = blogImageRepository.save(image);

                // one cover per post
                if (wantsPrimary) demoteOtherCovers(blogId, image.getId());

                created.add(getService.toDTO(image));
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, created.size() + " image(s) uploaded successfully", created)
            );
        } catch (Exception e) {
            log.error("Blog image upload failed; removing the files already written", e);
            for (String fileName : savedFileNames) storageService.deleteImage(fileName);
            throw new RuntimeException("Blog image upload failed: " + e.getMessage(), e);
        }
    }

    /** Exactly one cover: setting a new one clears the old, in the same transaction. */
    void demoteOtherCovers(Long blogId, Long keepId) {
        blogImageRepository.findByBlogIdOrderByDisplayOrderAscIdAsc(blogId).stream()
            .filter(other -> !other.getId().equals(keepId) && Boolean.TRUE.equals(other.getIsPrimary()))
            .forEach(other -> {
                other.setIsPrimary(false);
                blogImageRepository.save(other);
            });
    }

    private int safeOrder(Integer value) {
        return value != null ? value : 0;
    }
}
