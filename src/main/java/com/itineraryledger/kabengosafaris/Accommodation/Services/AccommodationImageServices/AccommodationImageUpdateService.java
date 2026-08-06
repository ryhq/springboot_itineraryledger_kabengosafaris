package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationImageServices;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationImageDTOs.AccommodationImageDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationImageDTOs.UpdateAccommodationImageDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationImageRepository;
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
 * Service for updating accommodation image metadata.
 *
 * Note: This service updates metadata only (imageType, altText, caption, etc.)
 * To replace the actual image file, delete the old image and upload a new one.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AccommodationImageUpdateService {

    private final AccommodationImageRepository accommodationImageRepository;
    private final AccommodationImageGetService getService;
    private final IdObfuscator idObfuscator;

    /**
     * Update an existing accommodation image's metadata
     *
     * @param obfuscatedId The obfuscated image ID
     * @param updateDTO The update DTO containing fields to update
     * @return ResponseEntity with ApiResponse containing updated image or error
     */
    @AuditLogAnnotation(
        action = "UPDATE_ACCOMMODATION_IMAGE",
        description = "Updating accommodation image metadata",
        entityType = "AccommodationImage"
    )
    public ResponseEntity<ApiResponse<?>> updateImage(String obfuscatedId, UpdateAccommodationImageDTO updateDTO) {
        log.info("Updating accommodation image with ID: {}", obfuscatedId);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                log.warn("Failed to decode accommodation image ID: {}", obfuscatedId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid image ID", "INVALID_IMAGE_ID")
                );
            }

            // Find image
            AccommodationImage image = accommodationImageRepository.findById(id).orElse(null);
            if (image == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Accommodation image not found", "IMAGE_NOT_FOUND")
                );
            }

            // Update fields if provided
            if (updateDTO.getImageType() != null) {
                image.setImageType(updateDTO.getImageType().isBlank() ? null : com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage.ImageType.valueOf(updateDTO.getImageType().trim()));
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
                // If setting as primary, unset other primary images for same accommodation
                if (updateDTO.getIsPrimary()) {
                    accommodationImageRepository.unsetPrimaryForAccommodation(image.getAccommodation().getId());
                }
                image.setIsPrimary(updateDTO.getIsPrimary());
            }
            if (updateDTO.getIsActive() != null) {
                image.setIsActive(updateDTO.getIsActive());
            }
            if (updateDTO.getIsWebActive() != null) {
                image.setIsWebActive(updateDTO.getIsWebActive());
            }

            // Save image
            image = accommodationImageRepository.save(image);

            // Convert to DTO
            AccommodationImageDTO imageDTO = getService.toDTO(image);

            log.info("Accommodation image updated successfully: {}", id);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Accommodation image updated successfully", imageDTO)
            );

        } catch (Exception e) {
            log.error("Error updating accommodation image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update accommodation image", "IMAGE_UPDATE_FAILED")
            );
        }
    }
}
