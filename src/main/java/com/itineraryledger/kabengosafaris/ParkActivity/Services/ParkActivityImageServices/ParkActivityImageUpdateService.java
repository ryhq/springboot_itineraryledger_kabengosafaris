package com.itineraryledger.kabengosafaris.ParkActivity.Services.ParkActivityImageServices;

import com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityImageDTOs.ParkActivityImageDTO;
import com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityImageDTOs.UpdateParkActivityImageDTO;
import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityImage;
import com.itineraryledger.kabengosafaris.ParkActivity.Repositories.ParkActivityImageRepository;
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
 * Service for updating park activity image metadata.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ParkActivityImageUpdateService {

    private final ParkActivityImageRepository parkActivityImageRepository;
    private final ParkActivityImageGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "UPDATE_PARK_ACTIVITY_IMAGE",
        description = "Updating park activity image metadata",
        entityType = "ParkActivityImage"
    )
    public ResponseEntity<ApiResponse<?>> updateImage(String obfuscatedId, UpdateParkActivityImageDTO updateDTO) {
        log.info("Updating park activity image with ID: {}", obfuscatedId);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                log.warn("Failed to decode park activity image ID: {}", obfuscatedId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid image ID", "INVALID_IMAGE_ID")
                );
            }

            ParkActivityImage image = parkActivityImageRepository.findById(id).orElse(null);
            if (image == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Park activity image not found", "IMAGE_NOT_FOUND")
                );
            }

            // Update fields if provided
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
                    // Unset primary for all other images of this park-activity
                    Long parkId = image.getParkActivity().getPark().getId();
                    Long activityId = image.getParkActivity().getActivity().getId();
                    parkActivityImageRepository.unsetPrimaryForParkActivity(parkId, activityId);
                }
                image.setIsPrimary(updateDTO.getIsPrimary());
            }
            if (updateDTO.getIsActive() != null) {
                image.setIsActive(updateDTO.getIsActive());
            }
            if (updateDTO.getIsWebActive() != null) {
                image.setIsWebActive(updateDTO.getIsWebActive());
            }

            image = parkActivityImageRepository.save(image);

            ParkActivityImageDTO imageDTO = getService.toDTO(image);

            log.info("Park activity image updated successfully: {}", id);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Park activity image updated successfully", imageDTO)
            );

        } catch (Exception e) {
            log.error("Error updating park activity image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update park activity image", "IMAGE_UPDATE_FAILED")
            );
        }
    }
}
