package com.itineraryledger.kabengosafaris.Translation.Services;

import com.itineraryledger.kabengosafaris.Translation.Cache.TranslationCache;
import com.itineraryledger.kabengosafaris.Translation.Cache.TranslationCacheRepository;
import com.itineraryledger.kabengosafaris.Translation.Settings.TranslationSettingGetterServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Async service for translation cache operations.
 * Cache writes run in a separate thread to avoid blocking the main translation flow
 * and to avoid read-only transaction issues.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationCacheAsyncService {

    private final TranslationCacheRepository cacheRepository;
    private final TranslationSettingGetterServices settingsService;

    /**
     * Save translation to cache asynchronously.
     * Runs in a separate thread to avoid blocking PDF generation.
     */
    @Async
    @Transactional
    public void saveToCacheAsync(String originalContent, String translatedContent, String sourceLanguage, String targetLanguage) {
        try {
            String contentHash = generateHash(originalContent, sourceLanguage, targetLanguage);
            int ttlHours = settingsService.getCacheTtlHours();

            // Check if entry already exists
            Optional<TranslationCache> existing = cacheRepository.findByContentHashAndSourceLanguageAndTargetLanguage(
                contentHash, sourceLanguage, targetLanguage
            );

            if (existing.isPresent()) {
                // Update existing entry
                TranslationCache cache = existing.get();
                cache.setTranslatedContent(translatedContent);
                cache.setExpiresAt(LocalDateTime.now().plusHours(ttlHours));
                cacheRepository.save(cache);
                log.debug("Updated translation cache (async): {} -> {}, {} chars",
                    sourceLanguage, targetLanguage, originalContent.length());
            } else {
                // Create new entry with temporary name
                TranslationCache cache = TranslationCache.builder()
                    .name("TEMP_" + System.currentTimeMillis() + "_" + System.nanoTime())
                    .contentHash(contentHash)
                    .sourceLanguage(sourceLanguage)
                    .targetLanguage(targetLanguage)
                    .originalContent(originalContent)
                    .translatedContent(translatedContent)
                    .characterCount(originalContent.length())
                    .hitCount(0L)
                    .expiresAt(LocalDateTime.now().plusHours(ttlHours))
                    .build();

                // Save to get the ID
                TranslationCache savedCache = cacheRepository.save(cache);

                // Generate and set the proper name using the entity ID
                String cacheName = generateCacheName(savedCache.getId());
                savedCache.setName(cacheName);
                cacheRepository.save(savedCache);

                log.debug("Saved translation to cache (async): {} - {} -> {}, {} chars",
                    cacheName, sourceLanguage, targetLanguage, originalContent.length());
            }

        } catch (Exception e) {
            log.error("Failed to save translation to cache (async): {}", e.getMessage(), e);
        }
    }

    /**
     * Record cache hit asynchronously.
     * Updates hit count without blocking the main flow.
     */
    @Async
    @Transactional
    public void recordCacheHitAsync(String contentHash, String sourceLanguage, String targetLanguage) {
        try {
            Optional<TranslationCache> cached = cacheRepository.findByContentHashAndSourceLanguageAndTargetLanguage(
                contentHash, sourceLanguage, targetLanguage
            );

            if (cached.isPresent()) {
                TranslationCache cache = cached.get();
                cache.recordHit();
                cacheRepository.save(cache);
                log.debug("Recorded cache hit (async) for {} -> {}", sourceLanguage, targetLanguage);
            }
        } catch (Exception e) {
            log.error("Failed to record cache hit (async): {}", e.getMessage(), e);
        }
    }

    /**
     * Clean up expired cache entries asynchronously.
     */
    @Async
    @Transactional
    public void cleanupExpiredCacheAsync() {
        try {
            int deleted = cacheRepository.deleteExpiredEntries();
            log.info("Cleaned up {} expired translation cache entries (async)", deleted);
        } catch (Exception e) {
            log.error("Failed to cleanup expired cache (async): {}", e.getMessage(), e);
        }
    }

    /**
     * Generate SHA-256 hash for cache key.
     */
    public String generateHash(String content, String sourceLanguage, String targetLanguage) {
        try {
            String combined = sourceLanguage + "|" + targetLanguage + "|" + content;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Offset added to entity ID to avoid exposing raw database IDs.
     */
    private static final long ID_OFFSET = 100000L;

    /**
     * Generate cache entry name using the entity ID (with offset) to guarantee uniqueness.
     * This approach is concurrency-safe as each entity has a unique auto-generated ID.
     *
     * Format: TRN_CACHE_{ID+OFFSET}{MM}{YY}
     *
     * Format breakdown:
     * - TRN_CACHE_ : Fixed prefix
     * - ID+OFFSET  : Entity ID plus offset (zero-padded to 6 digits for readability)
     * - MM         : Month (2 digits, zero-padded)
     * - YY         : Last 2 digits of year
     *
     * Examples:
     * - ID 1 in Dec 2024   → TRN_CACHE_1000011224
     * - ID 42 in Jan 2025  → TRN_CACHE_1000420125
     * - ID 1234 in Mar 2025 → TRN_CACHE_1012340325
     *
     * @param cacheId The saved cache ID (used to generate unique name)
     * @return Unique formatted cache name
     */
    public String generateCacheName(Long cacheId) {
        LocalDateTime now = LocalDateTime.now();
        String month = String.format("%02d", now.getMonthValue());
        String year = String.format("%02d", now.getYear() % 100);
        long obfuscatedId = cacheId + ID_OFFSET;
        String idFormatted = String.format("%06d", obfuscatedId);

        return String.format("TRN_CACHE_%s%s%s", idFormatted, month, year);
    }
}
