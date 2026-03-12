package com.itineraryledger.kabengosafaris.Public.Services;

import com.itineraryledger.kabengosafaris.Hero.Entity.Hero;
import com.itineraryledger.kabengosafaris.Hero.Entity.HeroImage;
import com.itineraryledger.kabengosafaris.Hero.Enums.HeroPage;
import com.itineraryledger.kabengosafaris.Hero.Repository.HeroRepository;
import com.itineraryledger.kabengosafaris.Hero.Services.HeroImageServices.HeroImageStorageService;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicHeroDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicHeroService {

    private final HeroRepository heroRepository;
    private final HeroImageStorageService heroImageStorageService;
    private final IdObfuscator idObfuscator;
    private final PublicTranslationService publicTranslationService;

    /**
     * Get active heroes for a specific page, ordered by displayOrder
     */
    public ResponseEntity<ApiResponse<?>> getHeroesByPage(HeroPage page, String lang) {
        try {
            List<Hero> heroes = heroRepository.findByPageAndIsActiveTrueOrderByDisplayOrderAsc(page);
            List<PublicHeroDTO> dtos = heroes.stream()
                .map(this::convertToPublicDTO)
                .collect(Collectors.toList());

            publicTranslationService.translateDtoList(dtos, lang);

            return ResponseEntity.ok(ApiResponse.success(200, "Heroes retrieved successfully", dtos));
        } catch (Exception e) {
            log.error("Error fetching heroes for page: {}", page, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch heroes", "HEROES_FETCH_FAILED"));
        }
    }

    PublicHeroDTO convertToPublicDTO(Hero hero) {
        // Resolve primary image: isPrimary image > first active image
        String primaryImageUrl = null;
        HeroImage primary = hero.getPrimaryImage();
        if (primary != null) {
            primaryImageUrl = heroImageStorageService.constructFileImageUrl(primary.getFileName());
        } else if (hero.getImages() != null && !hero.getImages().isEmpty()) {
            HeroImage first = hero.getActiveImages().stream().findFirst().orElse(null);
            if (first != null) {
                primaryImageUrl = heroImageStorageService.constructFileImageUrl(first.getFileName());
            }
        }

        return PublicHeroDTO.builder()
            .id(idObfuscator.encodeId(hero.getId()))
            .title(hero.getTitle())
            .subtitle(hero.getSubtitle())
            .description(hero.getDescription())
            .ctaText(hero.getCtaText())
            .ctaLink(hero.getCtaLink())
            .overlayColor(hero.getOverlayColor())
            .overlayOpacity(hero.getOverlayOpacity())
            .textAlignment(hero.getTextAlignment())
            .cssClasses(hero.getCssClasses())
            .primaryImageUrl(primaryImageUrl)
            .imageCount((long) hero.getImages().size())
            .build();
    }
}
