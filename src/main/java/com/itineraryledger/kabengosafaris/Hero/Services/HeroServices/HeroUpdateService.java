package com.itineraryledger.kabengosafaris.Hero.Services.HeroServices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Hero.Entity.Hero;
import com.itineraryledger.kabengosafaris.Hero.Entity.HeroImage;
import com.itineraryledger.kabengosafaris.Hero.Repository.HeroRepository;
import com.itineraryledger.kabengosafaris.Hero.DTOs.HeroDTO;
import com.itineraryledger.kabengosafaris.Hero.DTOs.UpdateHeroDTO;
import com.itineraryledger.kabengosafaris.Hero.Services.HeroImageServices.HeroImageStorageService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;

import lombok.extern.slf4j.Slf4j;

/**
 * HeroUpdateService - Service for updating heroes
 */
@Service
@Slf4j
@Transactional
public class HeroUpdateService {

    private final HeroRepository heroRepository;
    private final IdObfuscator idObfuscator;
    private final HeroImageStorageService storageService;

    @Autowired
    public HeroUpdateService(
        HeroRepository heroRepository,
        IdObfuscator idObfuscator,
        HeroImageStorageService storageService
    ) {
        this.heroRepository = heroRepository;
        this.idObfuscator = idObfuscator;
        this.storageService = storageService;
    }

    /**
     * Update a hero by obfuscated ID
     *
     * @param idObfuscated The obfuscated hero ID
     * @param updateHeroDTO The updated hero data
     * @return ResponseEntity with ApiResponse containing the updated hero
     */
    @AuditLogAnnotation(action = "UPDATE_HERO", description = "Updating hero", entityType = "Hero", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> updateHero(String idObfuscated, UpdateHeroDTO updateHeroDTO) {
        log.info("Updating hero with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode hero ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid hero ID",
                        "INVALID_HERO_ID"
                    )
                );
            }

            return updateHeroById(id, updateHeroDTO);

        } catch (Exception e) {
            log.error("Error updating hero", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to update hero",
                    "HERO_UPDATE_FAILED"
                )
            );
        }
    }

    /**
     * Update a hero by ID (internal method)
     */
    private ResponseEntity<ApiResponse<?>> updateHeroById(Long id, UpdateHeroDTO updateHeroDTO) {
        // Find hero
        Hero hero = heroRepository.findById(id).orElse(null);
        if (hero == null) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(
                    404,
                    "Hero not found",
                    "HERO_NOT_FOUND"
                )
            );
        }

        // Get current user from security context
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Update fields if provided
        if (updateHeroDTO.getTitle() != null) {
            hero.setTitle(updateHeroDTO.getTitle());
        }
        if (updateHeroDTO.getSubtitle() != null) {
            hero.setSubtitle(updateHeroDTO.getSubtitle());
        }
        if (updateHeroDTO.getDescription() != null) {
            hero.setDescription(updateHeroDTO.getDescription());
        }
        if (updateHeroDTO.getPage() != null) {
            hero.setPage(updateHeroDTO.getPage());
        }
        if (updateHeroDTO.getCtaText() != null) {
            hero.setCtaText(updateHeroDTO.getCtaText());
        }
        if (updateHeroDTO.getCtaLink() != null) {
            hero.setCtaLink(updateHeroDTO.getCtaLink());
        }
        if (updateHeroDTO.getIsActive() != null) {
            hero.setIsActive(updateHeroDTO.getIsActive());
        }
        if (updateHeroDTO.getOverlayColor() != null) {
            hero.setOverlayColor(updateHeroDTO.getOverlayColor());
        }
        if (updateHeroDTO.getOverlayOpacity() != null) {
            hero.setOverlayOpacity(updateHeroDTO.getOverlayOpacity());
        }
        if (updateHeroDTO.getTextAlignment() != null) {
            hero.setTextAlignment(updateHeroDTO.getTextAlignment());
        }
        if (updateHeroDTO.getCssClasses() != null) {
            hero.setCssClasses(updateHeroDTO.getCssClasses());
        }

        // Update audit fields
        hero.setUpdatedBy(currentUser);

        // Save hero
        hero = heroRepository.save(hero);

        // Convert to DTO
        HeroDTO heroDTO = convertToDTO(hero);

        log.info("Hero updated successfully: {}", hero.getTitle());

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                "Hero updated successfully",
                heroDTO
            )
        );
    }

    /**
     * Convert Hero entity to HeroDTO
     */
    private HeroDTO convertToDTO(Hero hero) {
        HeroDTO dto = HeroDTO.builder()
            .id(idObfuscator.encodeId(hero.getId()))
            .title(hero.getTitle())
            .subtitle(hero.getSubtitle())
            .description(hero.getDescription())
            .page(hero.getPage())
            .pageDisplayName(hero.getPage() != null ? hero.getPage().getDisplayName() : null)
            .ctaText(hero.getCtaText())
            .ctaLink(hero.getCtaLink())
            .displayOrder(hero.getDisplayOrder())
            .isActive(hero.getIsActive())
            .overlayColor(hero.getOverlayColor())
            .overlayOpacity(hero.getOverlayOpacity())
            .textAlignment(hero.getTextAlignment())
            .cssClasses(hero.getCssClasses())
            .imageCount((long) hero.getImages().size())
            .createdAt(hero.getCreatedAt())
            .updatedAt(hero.getUpdatedAt())
            .build();

        // Set audit fields
        if (hero.getCreatedBy() != null) {
            dto.setCreatedById(idObfuscator.encodeId(hero.getCreatedBy().getId()));
            dto.setCreatedByName(hero.getCreatedBy().getFirstName() + " " + hero.getCreatedBy().getLastName());
        }
        if (hero.getUpdatedBy() != null) {
            dto.setUpdatedById(idObfuscator.encodeId(hero.getUpdatedBy().getId()));
            dto.setUpdatedByName(hero.getUpdatedBy().getFirstName() + " " + hero.getUpdatedBy().getLastName());
        }

        // Set primary image URL
        HeroImage primaryImage = hero.getPrimaryImage();
        if (primaryImage != null && primaryImage.getFileName() != null) {
            dto.setPrimaryImageUrl(storageService.constructImageUrl(idObfuscator.encodeId(primaryImage.getId())));
        }

        return dto;
    }
}
