package com.itineraryledger.kabengosafaris.Hero.Services.HeroServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Hero.Entity.Hero;
import com.itineraryledger.kabengosafaris.Hero.Entity.HeroImage;
import com.itineraryledger.kabengosafaris.Hero.Repository.HeroRepository;
import com.itineraryledger.kabengosafaris.Hero.Repository.HeroImageRepository;
import com.itineraryledger.kabengosafaris.Hero.Services.HeroImageServices.HeroImageStorageService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * HeroDeleteService - Service for deleting heroes
 */
@Service
@Slf4j
@Transactional
public class HeroDeleteService {

    private final HeroRepository heroRepository;
    private final HeroImageRepository heroImageRepository;
    private final HeroImageStorageService storageService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public HeroDeleteService(
        HeroRepository heroRepository,
        HeroImageRepository heroImageRepository,
        HeroImageStorageService storageService,
        IdObfuscator idObfuscator
    ) {
        this.heroRepository = heroRepository;
        this.heroImageRepository = heroImageRepository;
        this.storageService = storageService;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete heroes by list of obfuscated IDs
     *
     * @param idObfuscatedList List of obfuscated hero IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteHeroes(List<String> idObfuscatedList) {
        log.info("Deleting {} heroes", idObfuscatedList.size());

        try {
            // Decode all obfuscated IDs
            List<Long> ids = new ArrayList<>();
            for (String idObfuscated : idObfuscatedList) {
                try {
                    Long id = idObfuscator.decodeId(idObfuscated);
                    ids.add(id);
                } catch (Exception e) {
                    log.warn("Failed to decode ID: {}", idObfuscated, e);
                }
            }

            return deleteHeroesInternal(ids);

        } catch (Exception e) {
            log.error("Error deleting heroes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete heroes",
                    "HEROES_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete heroes by list of IDs (internal method)
     */
    private ResponseEntity<ApiResponse<?>> deleteHeroesInternal(List<Long> ids) {
        int deletedCount = 0;
        /*
         * Per-row outcomes, because this used to answer "200, none deleted" in the same
         * words as "200, all deleted" — a row that was missing or that threw was logged
         * here and never mentioned to the caller.
         */
        List<String> deletedIds = new java.util.ArrayList<>();
        List<Map<String, Object>> skipped = new java.util.ArrayList<>();

        for (Long id : ids) {
            try {
                Hero hero = heroRepository.findById(id).orElse(null);

                if (hero == null) {
                    log.warn("Hero not found: {}", id);
                    skipped.add(skip(id, null, "No such banner — it may already have been deleted"));
                    continue;
                }

                // Delete all associated images from filesystem
                List<HeroImage> images = heroImageRepository.findByHeroId(id);
                for (HeroImage image : images) {
                    if (image.getFileName() != null) {
                        storageService.deleteImage(image.getFileName());
                    }
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((HeroDeleteService) AopContext.currentProxy()).deleteHero(id);
                deletedCount++;
                deletedIds.add(idObfuscator.encodeId(id));
                log.info("Hero deleted successfully: {}", id);

            } catch (Exception e) {
                log.error("Error deleting hero: {}", id, e);
                skipped.add(skip(id, null, e.getMessage() != null ? e.getMessage() : "Could not be deleted"));
            }
        }

        Map<String, Object> report = new java.util.HashMap<>();
        report.put("deletedCount", deletedCount);
        report.put("deletedIds", deletedIds);
        report.put("skipped", skipped);

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                deletedCount + " hero(es) deleted successfully",
                report
            )
        );
    }

    /** One skipped row, named the way the caller can show it. */
    private Map<String, Object> skip(Long id, String title, String reason) {
        Map<String, Object> row = new java.util.HashMap<>();
        row.put("id", idObfuscator.encodeId(id));
        row.put("code", title);
        row.put("reason", reason);
        return row;
    }

    @AuditLogAnnotation(action = "DELETE_HERO", description = "Deleting hero", entityType = "Hero", entityIdParamName = "id")
    public void deleteHero(Long id) {
        heroRepository.deleteById(id);
    }
}
