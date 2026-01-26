package com.itineraryledger.kabengosafaris.Activity.Services.ActivityImageServices;

import com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityImageDTOs.ActivityImageDTO;
import com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityImageDTOs.UpdateActivityImageDTO;
import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityImage;
import com.itineraryledger.kabengosafaris.Activity.Repositories.ActivityImageRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for updating activity image metadata.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ActivityImageUpdateService {

    private final ActivityImageRepository activityImageRepository;
    private final ActivityImageGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "UPDATE_ACTIVITY_IMAGE",
        description = "Updating activity image metadata",
        entityType = "ActivityImage"
    )
    public ResponseEntity<ApiResponse<?>> updateImage(String obfuscatedId, UpdateActivityImageDTO updateDTO) {
        log.info("Updating activity image with ID: {}", obfuscatedId);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                log.warn("Failed to decode activity image ID: {}", obfuscatedId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid image ID", "INVALID_IMAGE_ID")
                );
            }

            ActivityImage image = activityImageRepository.findById(id).orElse(null);
            if (image == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Activity image not found", "IMAGE_NOT_FOUND")
                );
            }

            if (updateDTO.getImageType() != null) {
                image.setImageType(updateDTO.getImageType());
            }
            if (updateDTO.getAltText() != null) {
                image.setAltText(updateDTO.getAltText());
            }
            if (updateDTO.getCaption() != null) {
                image.setCaption(updateDTO.getCaption());
            }
            if (updateDTO.getDescription() != null) {
                image.setDescription(updateDTO.getDescription());
            }
            if (updateDTO.getIsPrimary() != null) {
                if (updateDTO.getIsPrimary()) {
                    activityImageRepository.unsetPrimaryForActivity(image.getActivity().getId());
                }
                image.setIsPrimary(updateDTO.getIsPrimary());
            }
            if (updateDTO.getIsActive() != null) {
                image.setIsActive(updateDTO.getIsActive());
            }

            image = activityImageRepository.save(image);

            ActivityImageDTO imageDTO = getService.toDTO(image);

            log.info("Activity image updated successfully: {}", id);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Activity image updated successfully", imageDTO)
            );

        } catch (Exception e) {
            log.error("Error updating activity image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update activity image", "IMAGE_UPDATE_FAILED")
            );
        }
    }
}
