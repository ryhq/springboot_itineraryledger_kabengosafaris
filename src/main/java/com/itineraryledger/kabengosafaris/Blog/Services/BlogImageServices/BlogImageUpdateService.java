package com.itineraryledger.kabengosafaris.Blog.Services.BlogImageServices;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Blog.DTOs.BlogImageDTOs.UpdateBlogImageDTO;
import com.itineraryledger.kabengosafaris.Blog.Entity.BlogImage;
import com.itineraryledger.kabengosafaris.Blog.Repository.BlogImageRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Editing an image's words, its cover flag and whether it is in use. */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BlogImageUpdateService {

    private final BlogImageRepository blogImageRepository;
    private final BlogImageGetService getService;
    private final BlogImageCreateService createService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(action = "UPDATE_BLOG_IMAGE", description = "Updating a blog image", entityType = "BlogImage")
    public ResponseEntity<ApiResponse<?>> updateImage(String obfuscatedId, UpdateBlogImageDTO updateDTO) {
        try {
            Long id = idObfuscator.decodeId(obfuscatedId);
            BlogImage image = blogImageRepository.findById(id).orElse(null);
            if (image == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Blog image not found", "IMAGE_NOT_FOUND")
                );
            }

            /* null means "leave it alone", so a single field can be saved on its own */
            if (updateDTO.getAltText() != null) image.setAltText(updateDTO.getAltText());
            if (updateDTO.getCaption() != null) image.setCaption(updateDTO.getCaption());
            if (updateDTO.getDescription() != null) image.setDescription(updateDTO.getDescription());
            if (updateDTO.getIsActive() != null) image.setIsActive(updateDTO.getIsActive());

            boolean becameCover = false;
            if (updateDTO.getIsPrimary() != null) {
                image.setIsPrimary(updateDTO.getIsPrimary());
                becameCover = updateDTO.getIsPrimary();
            }

            image = blogImageRepository.save(image);
            if (becameCover && image.getBlog() != null) {
                createService.demoteOtherCovers(image.getBlog().getId(), image.getId());
            }

            return ResponseEntity.ok(ApiResponse.success(200, "Blog image updated successfully", getService.toDTO(image)));
        } catch (Exception e) {
            log.error("Error updating blog image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update blog image", "IMAGE_UPDATE_FAILED")
            );
        }
    }
}
