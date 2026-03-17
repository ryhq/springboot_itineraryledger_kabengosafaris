package com.itineraryledger.kabengosafaris.Park.Services.ParkImageServices;

import com.itineraryledger.kabengosafaris.Park.DTOs.ParkImageDTOs.ParkImageDTO;
import com.itineraryledger.kabengosafaris.Park.DTOs.ParkImageDTOs.UpdateParkImageDTO;
import com.itineraryledger.kabengosafaris.Park.Entities.ParkImage;
import com.itineraryledger.kabengosafaris.Park.Repositories.ParkImageRepository;
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
 * Service for updating park image metadata.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ParkImageUpdateService {

    private final ParkImageRepository parkImageRepository;
    private final ParkImageGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "UPDATE_PARK_IMAGE",
        description = "Updating park image metadata",
        entityType = "ParkImage"
    )
    public ResponseEntity<ApiResponse<?>> updateImage(String obfuscatedId, UpdateParkImageDTO updateDTO) {
        log.info("Updating park image with ID: {}", obfuscatedId);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                log.warn("Failed to decode park image ID: {}", obfuscatedId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid image ID", "INVALID_IMAGE_ID")
                );
            }

            ParkImage image = parkImageRepository.findById(id).orElse(null);
            if (image == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Park image not found", "IMAGE_NOT_FOUND")
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
                    parkImageRepository.unsetPrimaryForPark(image.getPark().getId());
                }
                image.setIsPrimary(updateDTO.getIsPrimary());
            }
            if (updateDTO.getIsActive() != null) {
                image.setIsActive(updateDTO.getIsActive());
            }
            if (updateDTO.getIsWebActive() != null) {
                image.setIsWebActive(updateDTO.getIsWebActive());
            }

            image = parkImageRepository.save(image);

            ParkImageDTO imageDTO = getService.toDTO(image);

            log.info("Park image updated successfully: {}", id);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Park image updated successfully", imageDTO)
            );

        } catch (Exception e) {
            log.error("Error updating park image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update park image", "IMAGE_UPDATE_FAILED")
            );
        }
    }
}
