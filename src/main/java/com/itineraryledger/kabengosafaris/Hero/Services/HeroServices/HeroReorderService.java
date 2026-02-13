package com.itineraryledger.kabengosafaris.Hero.Services.HeroServices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Hero.DTOs.HeroDTO;
import com.itineraryledger.kabengosafaris.Hero.DTOs.ReorderHeroDTO;
import com.itineraryledger.kabengosafaris.Hero.DTOs.ReorderHeroDTO.HeroOrderItem;
import com.itineraryledger.kabengosafaris.Hero.Entity.Hero;
import com.itineraryledger.kabengosafaris.Hero.Repository.HeroRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * HeroReorderService - Service for reordering heroes
 *
 * Handles drag-and-drop reordering from the UI with comprehensive validations:
 * - Validates all hero IDs exist and belong to the page
 * - Validates no duplicate hero IDs
 * - Validates all heroes are included (no missing heroes)
 * - Validates expected display orders if provided
 * - Updates display orders
 */
@Service
@Slf4j
@Transactional
public class HeroReorderService {

    private final HeroRepository heroRepository;
    private final HeroGetService getService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public HeroReorderService(
        HeroRepository heroRepository,
        HeroGetService getService,
        IdObfuscator idObfuscator
    ) {
        this.heroRepository = heroRepository;
        this.getService = getService;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Reorder heroes based on the new order provided
     *
     * @param reorderDTO The reorder data containing the page and new hero order
     * @return ResponseEntity with ApiResponse containing the reordered heroes
     */
    @AuditLogAnnotation(
        action = "REORDER_HEROES",
        description = "Reordering heroes",
        entityType = "Hero"
    )
    public ResponseEntity<ApiResponse<?>> reorderHeroes(ReorderHeroDTO reorderDTO) {
        log.info("Reordering heroes for page: {}", reorderDTO.getPage());

        try {
            // ========================
            // FETCH EXISTING HEROES FOR PAGE
            // ========================
            List<Hero> existingHeroes = heroRepository.findByPageOrderByDisplayOrderAsc(reorderDTO.getPage());

            if (existingHeroes.isEmpty()) {
                log.warn("No heroes found for page: {}", reorderDTO.getPage());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Page has no heroes to reorder", "NO_HEROES_TO_REORDER")
                );
            }

            // ========================
            // VALIDATION: Check hero order list size matches existing heroes
            // ========================
            List<HeroOrderItem> heroOrder = reorderDTO.getHeroOrder();

            if (heroOrder.size() != existingHeroes.size()) {
                log.warn("Hero order count mismatch. Expected: {}, Received: {}", existingHeroes.size(), heroOrder.size());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Hero order list must contain exactly " + existingHeroes.size() + " heroes. Received: " + heroOrder.size(),
                        "HERO_COUNT_MISMATCH"
                    )
                );
            }

            // ========================
            // DECODE ALL HERO IDs AND VALIDATE FORMAT
            // ========================
            Map<Long, HeroOrderItem> decodedHeroIds = new LinkedHashMap<>();
            List<String> invalidIds = new ArrayList<>();
            List<String> duplicateIds = new ArrayList<>();

            for (HeroOrderItem item : heroOrder) {
                if (item.getHeroId() == null || item.getHeroId().isBlank()) {
                    invalidIds.add("null/empty");
                    continue;
                }

                try {
                    Long decodedId = idObfuscator.decodeId(item.getHeroId());

                    // Check for duplicates
                    if (decodedHeroIds.containsKey(decodedId)) {
                        duplicateIds.add(item.getHeroId());
                    } else {
                        decodedHeroIds.put(decodedId, item);
                    }
                } catch (Exception e) {
                    invalidIds.add(item.getHeroId());
                }
            }

            if (!invalidIds.isEmpty()) {
                log.warn("Invalid hero ID formats: {}", invalidIds);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid hero ID format(s): " + String.join(", ", invalidIds),
                        "INVALID_HERO_ID_FORMAT"
                    )
                );
            }

            if (!duplicateIds.isEmpty()) {
                log.warn("Duplicate hero IDs in reorder list: {}", duplicateIds);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Duplicate hero ID(s) in reorder list: " + String.join(", ", duplicateIds),
                        "DUPLICATE_HERO_IDS"
                    )
                );
            }

            // ========================
            // VALIDATION: All heroes belong to this page
            // ========================
            Set<Long> existingHeroIds = existingHeroes.stream()
                .map(Hero::getId)
                .collect(Collectors.toSet());

            Set<Long> providedHeroIds = decodedHeroIds.keySet();

            // Check for heroes that don't belong to this page
            Set<Long> foreignHeroes = new HashSet<>(providedHeroIds);
            foreignHeroes.removeAll(existingHeroIds);

            if (!foreignHeroes.isEmpty()) {
                List<String> foreignHeroObfuscated = foreignHeroes.stream()
                    .map(id -> idObfuscator.encodeId(id))
                    .collect(Collectors.toList());
                log.warn("Hero IDs not belonging to page: {}", foreignHeroObfuscated);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Hero ID(s) do not belong to this page: " + String.join(", ", foreignHeroObfuscated),
                        "HERO_PAGE_MISMATCH"
                    )
                );
            }

            // Check for missing heroes
            Set<Long> missingHeroes = new HashSet<>(existingHeroIds);
            missingHeroes.removeAll(providedHeroIds);

            if (!missingHeroes.isEmpty()) {
                List<String> missingHeroObfuscated = missingHeroes.stream()
                    .map(id -> idObfuscator.encodeId(id))
                    .collect(Collectors.toList());
                log.warn("Missing hero IDs in reorder list: {}", missingHeroObfuscated);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Missing hero ID(s) in reorder list: " + String.join(", ", missingHeroObfuscated),
                        "MISSING_HERO_IDS"
                    )
                );
            }

            // ========================
            // VALIDATION: Expected display orders (if provided)
            // ========================
            List<String> expectedOrderMismatches = new ArrayList<>();
            int position = 1;

            for (HeroOrderItem item : heroOrder) {
                if (item.getExpectedDisplayOrder() != null && !item.getExpectedDisplayOrder().equals(position)) {
                    expectedOrderMismatches.add(
                        String.format("Hero %s: expected %d, but position is %d",
                            item.getHeroId(), item.getExpectedDisplayOrder(), position)
                    );
                }
                position++;
            }

            if (!expectedOrderMismatches.isEmpty()) {
                log.warn("Expected display order mismatches: {}", expectedOrderMismatches);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Expected display order mismatches: " + String.join("; ", expectedOrderMismatches),
                        "EXPECTED_ORDER_MISMATCH"
                    )
                );
            }

            // ========================
            // CREATE HERO LOOKUP MAP
            // ========================
            Map<Long, Hero> heroLookup = existingHeroes.stream()
                .collect(Collectors.toMap(Hero::getId, hero -> hero));

            // ========================
            // CHECK IF REORDER IS ACTUALLY NEEDED
            // ========================
            boolean orderChanged = false;
            int newDisplayOrder = 1;

            for (Long heroId : decodedHeroIds.keySet()) {
                Hero hero = heroLookup.get(heroId);
                if (!hero.getDisplayOrder().equals(newDisplayOrder)) {
                    orderChanged = true;
                    break;
                }
                newDisplayOrder++;
            }

            if (!orderChanged) {
                log.info("Hero order unchanged for page: {}", reorderDTO.getPage());
                List<HeroDTO> resultDTOs = existingHeroes.stream()
                    .map(getService::convertToDTO)
                    .collect(Collectors.toList());

                return ResponseEntity.ok().body(
                    ApiResponse.success(200, "Hero order unchanged", resultDTOs)
                );
            }

            // ========================
            // PERFORM REORDER
            // ========================
            log.info("Performing reorder for {} heroes", existingHeroes.size());

            // Pass 1: Set temporary negative display orders to avoid unique constraint violations (if any)
            int tempOrder = -1;
            for (Hero hero : existingHeroes) {
                hero.setDisplayOrder(tempOrder--);
            }
            heroRepository.saveAll(existingHeroes);
            heroRepository.flush();

            // Pass 2: Set final display orders based on new order
            List<Hero> reorderedHeroes = new ArrayList<>();
            newDisplayOrder = 1;

            for (Long heroId : decodedHeroIds.keySet()) {
                Hero hero = heroLookup.get(heroId);
                hero.setDisplayOrder(newDisplayOrder);
                reorderedHeroes.add(hero);
                newDisplayOrder++;
            }

            // Save all reordered heroes
            reorderedHeroes = heroRepository.saveAll(reorderedHeroes);

            // ========================
            // CONVERT TO DTOs AND RETURN
            // ========================
            List<HeroDTO> resultDTOs = reorderedHeroes.stream()
                .sorted(Comparator.comparing(Hero::getDisplayOrder))
                .map(getService::convertToDTO)
                .collect(Collectors.toList());

            log.info("Successfully reordered {} heroes for page: {}", reorderedHeroes.size(), reorderDTO.getPage());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Heroes reordered successfully", resultDTOs)
            );

        } catch (Exception e) {
            log.error("Error reordering heroes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to reorder heroes", "HEROES_REORDER_FAILED")
            );
        }
    }
}
