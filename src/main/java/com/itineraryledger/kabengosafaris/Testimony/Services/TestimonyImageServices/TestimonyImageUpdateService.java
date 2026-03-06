package com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyImageServices;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Testimony.DTOs.TestimonyImageDTOs.TestimonyImageDTO;
import com.itineraryledger.kabengosafaris.Testimony.DTOs.TestimonyImageDTOs.UpdateTestimonyImageDTO;
import com.itineraryledger.kabengosafaris.Testimony.Entity.TestimonyImage;
import com.itineraryledger.kabengosafaris.Testimony.Repository.TestimonyImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TestimonyImageUpdateService {

    private final TestimonyImageRepository testimonyImageRepository;
    private final TestimonyImageGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "UPDATE_TESTIMONY_IMAGE",
        description = "Updating testimony image metadata",
        entityType = "TestimonyImage"
    )
    public ResponseEntity<ApiResponse<?>> updateImage(String obfuscatedId, UpdateTestimonyImageDTO updateDTO) {
        log.info("Updating testimony image with ID: {}", obfuscatedId);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                log.warn("Failed to decode testimony image ID: {}", obfuscatedId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid image ID", "INVALID_IMAGE_ID")
                );
            }

            TestimonyImage image = testimonyImageRepository.findById(id).orElse(null);
            if (image == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Testimony image not found", "IMAGE_NOT_FOUND")
                );
            }

            if (updateDTO.getAltText() != null) image.setAltText(updateDTO.getAltText());
            if (updateDTO.getCaption() != null) image.setCaption(updateDTO.getCaption());
            if (updateDTO.getDescription() != null) image.setDescription(updateDTO.getDescription());
            if (updateDTO.getIsPrimary() != null) {
                if (updateDTO.getIsPrimary()) {
                    testimonyImageRepository.unsetPrimaryForTestimony(image.getTestimony().getId());
                }
                image.setIsPrimary(updateDTO.getIsPrimary());
            }
            if (updateDTO.getIsActive() != null) image.setIsActive(updateDTO.getIsActive());
            if (updateDTO.getDisplayOrder() != null) image.setDisplayOrder(updateDTO.getDisplayOrder());

            image = testimonyImageRepository.save(image);

            TestimonyImageDTO imageDTO = getService.toDTO(image);

            log.info("Testimony image updated successfully: {}", id);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Testimony image updated successfully", imageDTO)
            );

        } catch (Exception e) {
            log.error("Error updating testimony image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update testimony image", "IMAGE_UPDATE_FAILED")
            );
        }
    }
}
