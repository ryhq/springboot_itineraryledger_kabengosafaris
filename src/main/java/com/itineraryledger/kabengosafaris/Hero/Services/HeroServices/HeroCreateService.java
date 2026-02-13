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
import com.itineraryledger.kabengosafaris.Hero.DTOs.CreateHeroDTO;
import com.itineraryledger.kabengosafaris.Hero.DTOs.HeroDTO;
import com.itineraryledger.kabengosafaris.Hero.Services.HeroImageServices.HeroImageStorageService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;

import lombok.extern.slf4j.Slf4j;

/**
 * HeroCreateService - Service for creating heroes
 */
@Service
@Slf4j
@Transactional
public class HeroCreateService {

    private final HeroRepository heroRepository;
    private final IdObfuscator idObfuscator;
    private final HeroImageStorageService storageService;

    @Autowired
    public HeroCreateService(
        HeroRepository heroRepository,
        IdObfuscator idObfuscator,
        HeroImageStorageService storageService
    ) {
        this.heroRepository = heroRepository;
        this.idObfuscator = idObfuscator;
        this.storageService = storageService;
    }

    /**
     * Create a new hero
     *
     * @param createHeroDTO The hero data
     * @return ResponseEntity with ApiResponse containing the created hero
     */
    @AuditLogAnnotation(action = "CREATE_HERO", description = "Creating a new hero", entityType = "Hero")
    public ResponseEntity<ApiResponse<?>> createHero(CreateHeroDTO createHeroDTO) {
        log.info("Creating new hero: {} for page: {}", createHeroDTO.getTitle(), createHeroDTO.getPage());

        try {
            // Get current user from security context
            User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            // Create hero entity
            Hero hero = Hero.builder()
                .title(createHeroDTO.getTitle())
                .subtitle(createHeroDTO.getSubtitle())
                .description(createHeroDTO.getDescription())
                .page(createHeroDTO.getPage())
                .ctaText(createHeroDTO.getCtaText())
                .ctaLink(createHeroDTO.getCtaLink())
                .displayOrder(createHeroDTO.getDisplayOrder() != null ? createHeroDTO.getDisplayOrder() : 0)
                .isActive(createHeroDTO.getIsActive() != null ? createHeroDTO.getIsActive() : true)
                .overlayColor(createHeroDTO.getOverlayColor())
                .overlayOpacity(createHeroDTO.getOverlayOpacity())
                .textAlignment(createHeroDTO.getTextAlignment())
                .cssClasses(createHeroDTO.getCssClasses())
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

            // Save hero
            hero = heroRepository.save(hero);

            // Convert to DTO
            HeroDTO heroDTO = convertToDTO(hero);

            log.info("Hero created successfully: {}", hero.getTitle());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                    201,
                    "Hero created successfully",
                    heroDTO
                )
            );

        } catch (Exception e) {
            log.error("Error creating hero", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to create hero",
                    "HERO_CREATE_FAILED"
                )
            );
        }
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
