package com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Services;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.DTOs.ItineraryImageDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.DTOs.UpdateItineraryImageDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Entity.ItineraryImage;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Repository.ItineraryImageRepository;
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
 * Service for updating itinerary image metadata.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ItineraryImageUpdateService {

    private final ItineraryImageRepository itineraryImageRepository;
    private final ItineraryImageGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "UPDATE_ITINERARY_IMAGE",
        description = "Updating itinerary image metadata",
        entityType = "ItineraryImage"
    )
    public ResponseEntity<ApiResponse<?>> updateImage(String obfuscatedId, UpdateItineraryImageDTO updateDTO) {
        log.info("Updating itinerary image with ID: {}", obfuscatedId);
        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                log.warn("Failed to decode itinerary image ID: {}", obfuscatedId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid image ID", "INVALID_IMAGE_ID")
                );
            }

            ItineraryImage image = itineraryImageRepository.findById(id).orElse(null);
            if (image == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary image not found", "IMAGE_NOT_FOUND")
                );
            }

            if (updateDTO.getImageType() != null) image.setImageType(updateDTO.getImageType());
            if (updateDTO.getAltText() != null) image.setAltText(updateDTO.getAltText());
            if (updateDTO.getCaption() != null) image.setCaption(updateDTO.getCaption());
            if (updateDTO.getDescription() != null) image.setDescription(updateDTO.getDescription());
            if (updateDTO.getIsPrimary() != null) {
                if (updateDTO.getIsPrimary()) {
                    itineraryImageRepository.unsetPrimaryForItinerary(image.getItinerary().getId());
                }
                image.setIsPrimary(updateDTO.getIsPrimary());
            }
            if (updateDTO.getIsActive() != null) image.setIsActive(updateDTO.getIsActive());
            if (updateDTO.getIsWebActive() != null) image.setIsWebActive(updateDTO.getIsWebActive());

            image = itineraryImageRepository.save(image);
            ItineraryImageDTO imageDTO = getService.toDTO(image);

            log.info("Itinerary image updated successfully: {}", id);
            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Itinerary image updated successfully", imageDTO)
            );
        } catch (Exception e) {
            log.error("Error updating itinerary image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update itinerary image", "IMAGE_UPDATE_FAILED")
            );
        }
    }
}
