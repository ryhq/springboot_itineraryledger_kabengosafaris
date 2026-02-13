package com.itineraryledger.kabengosafaris.Hero.Services.HeroImageServices;

import com.itineraryledger.kabengosafaris.Hero.DTOs.HeroImageDTOs.HeroImageDTO;
import com.itineraryledger.kabengosafaris.Hero.DTOs.HeroImageDTOs.UpdateHeroImageDTO;
import com.itineraryledger.kabengosafaris.Hero.Entity.HeroImage;
import com.itineraryledger.kabengosafaris.Hero.Repository.HeroImageRepository;
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
 * Service for updating hero image metadata.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class HeroImageUpdateService {

    private final HeroImageRepository heroImageRepository;
    private final HeroImageGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "UPDATE_HERO_IMAGE",
        description = "Updating hero image metadata",
        entityType = "HeroImage"
    )
    public ResponseEntity<ApiResponse<?>> updateImage(String obfuscatedId, UpdateHeroImageDTO updateDTO) {
        log.info("Updating hero image with ID: {}", obfuscatedId);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                log.warn("Failed to decode hero image ID: {}", obfuscatedId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid image ID", "INVALID_IMAGE_ID")
                );
            }

            HeroImage image = heroImageRepository.findById(id).orElse(null);
            if (image == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Hero image not found", "IMAGE_NOT_FOUND")
                );
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
                    // Unset other primary images for this hero
                    heroImageRepository.unsetPrimaryForHero(image.getHero().getId());
                }
                image.setIsPrimary(updateDTO.getIsPrimary());
            }
            if (updateDTO.getIsActive() != null) {
                image.setIsActive(updateDTO.getIsActive());
            }

            image = heroImageRepository.save(image);

            HeroImageDTO imageDTO = getService.toDTO(image);

            log.info("Hero image updated successfully: {}", id);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Hero image updated successfully", imageDTO)
            );

        } catch (Exception e) {
            log.error("Error updating hero image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update hero image", "IMAGE_UPDATE_FAILED")
            );
        }
    }
}
