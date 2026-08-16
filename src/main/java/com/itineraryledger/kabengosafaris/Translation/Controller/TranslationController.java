package com.itineraryledger.kabengosafaris.Translation.Controller;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Translation.Cache.TranslationCacheGetterService;
import com.itineraryledger.kabengosafaris.Translation.Cache.TranslationCacheRepository;
import com.itineraryledger.kabengosafaris.Translation.Providers.TranslationProvider;
import com.itineraryledger.kabengosafaris.Translation.Providers.TranslationProviderException;
import com.itineraryledger.kabengosafaris.Translation.Providers.TranslationProviderFactory;
import com.itineraryledger.kabengosafaris.Translation.Services.TranslationService;
import com.itineraryledger.kabengosafaris.Translation.Settings.TranslationSettingGetterServices;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Translation Service Management APIs.
 *
 * Provides endpoints for:
 * - Checking translation service health
 * - Getting available languages
 * - Viewing cache statistics and entries
 * - Clearing translation cache
 * - Testing the translation service
 */
@RestController
@RequestMapping("/api/translation")
@RequiredArgsConstructor
@Slf4j
public class TranslationController {

    private final TranslationService translationService;
    private final TranslationProviderFactory providerFactory;
    private final TranslationCacheRepository cacheRepository;
    private final TranslationCacheGetterService cacheGetterService;
    private final TranslationSettingGetterServices settingsService;

    /**
     * Get available languages from the active translation provider.
     */
    @GetMapping("/languages")
    @PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
    public ResponseEntity<ApiResponse<?>> getAvailableLanguages() {
        log.info("GET /api/translation/languages - Fetching available languages");

        try {
            TranslationProvider provider = providerFactory.getActiveProvider();
            List<Map<String, String>> languages = provider.getAvailableLanguages();

            Map<String, Object> response = new HashMap<>();
            response.put("languages", languages);
            response.put("provider", provider.getProviderType().getDisplayName());
            response.put("supportedLanguages", settingsService.getSupportedLanguages());
            response.put("defaultSourceLanguage", settingsService.getDefaultSourceLanguage());
            response.put("defaultTargetLanguage", settingsService.getDefaultTargetLanguage());

            return ResponseEntity.ok(ApiResponse.success(200, "Available languages retrieved", response));
        } catch (TranslationProviderException e) {
            log.warn("Failed to fetch languages from translation provider: {}", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("languages", List.of());
            response.put("supportedLanguages", settingsService.getSupportedLanguages());
            response.put("defaultSourceLanguage", settingsService.getDefaultSourceLanguage());
            response.put("defaultTargetLanguage", settingsService.getDefaultTargetLanguage());
            response.put("warning", "Translation provider unavailable, showing configured languages only");

            return ResponseEntity.ok(ApiResponse.success(200, "Configured languages retrieved (provider unavailable)", response));
        }
    }

    /**
     * Check translation service health.
     * Returns detailed status of the active translation provider.
     */
    @GetMapping("/health")
    @PreAuthorize("hasAuthority('PERM_TEST_TRANSLATION_SERVICE')")
    public ResponseEntity<ApiResponse<?>> checkHealth() {
        log.info("GET /api/translation/health - Checking translation service health");

        Map<String, Object> health = new HashMap<>();

        // Check if any provider is available
        boolean hasProvider = providerFactory.hasActiveProvider();
        health.put("enabled", hasProvider);

        if (!hasProvider) {
            health.put("status", "DISABLED");
            health.put("message", "No translation provider configured");
            return ResponseEntity.ok(ApiResponse.success(200, "Translation service is disabled", health));
        }

        try {
            TranslationProvider provider = providerFactory.getActiveProvider();
            health.put("providerType", provider.getProviderType().name());
            health.put("providerName", provider.getProviderType().getDisplayName());

            boolean available = provider.isServiceAvailable();
            health.put("available", available);

            if (available) {
                health.put("status", "HEALTHY");
                health.put("message", provider.getProviderType().getDisplayName() + " is running and responding");

                try {
                    List<Map<String, String>> languages = provider.getAvailableLanguages();
                    health.put("languageCount", languages.size());
                    health.put("languages", languages.stream()
                        .map(lang -> lang.get("code"))
                        .toList());
                } catch (TranslationProviderException e) {
                    health.put("languageWarning", "Could not fetch language list");
                }
            } else {
                health.put("status", "UNHEALTHY");
                health.put("message", provider.getProviderType().getDisplayName() + " is not responding");
            }

            health.put("cacheEnabled", settingsService.isCacheEnabled());

            String statusMessage = available ? "Translation service is healthy" : "Translation service is not available";
            return ResponseEntity.ok(ApiResponse.success(200, statusMessage, health));

        } catch (TranslationProviderException e) {
            health.put("status", "ERROR");
            health.put("message", e.getMessage());
            return ResponseEntity.ok(ApiResponse.success(200, "Translation service error", health));
        }
    }

    /**
     * Get translation cache statistics.
     * Returns metrics about cache usage and performance.
     */
    @GetMapping("/cache/stats")
    @PreAuthorize("hasAuthority('PERM_READ_TRANSLATION_CACHE_STATS')")
    public ResponseEntity<ApiResponse<?>> getCacheStats() {
        log.info("GET /api/translation/cache/stats - Fetching cache statistics");

        TranslationService.CacheStats stats = translationService.getCacheStats();

        Map<String, Object> response = new HashMap<>();
        response.put("validEntries", stats.validEntries());
        response.put("totalHits", stats.totalHits());
        response.put("charactersSaved", stats.charactersSaved());
        response.put("cacheEnabled", settingsService.isCacheEnabled());
        response.put("cacheTtlHours", settingsService.getCacheTtlHours());

        // Calculate cache efficiency
        long totalEntries = cacheRepository.count();
        response.put("totalEntries", totalEntries);

        if (stats.validEntries() > 0 && stats.totalHits() > 0) {
            double hitRate = (double) stats.totalHits() / (stats.totalHits() + totalEntries) * 100;
            response.put("estimatedHitRate", String.format("%.2f%%", hitRate));
        } else {
            response.put("estimatedHitRate", "N/A");
        }

        return ResponseEntity.ok(ApiResponse.success(200, "Cache statistics retrieved", response));
    }

    /**
     * Clear translation cache.
     * Can clear all entries or filter by criteria.
     */
    @DeleteMapping("/cache")
    @PreAuthorize("hasAuthority('PERM_CLEAR_TRANSLATION_CACHE')")
    public ResponseEntity<ApiResponse<?>> clearCache(
        @RequestParam(required = false) String sourceLanguage,
        @RequestParam(required = false) String targetLanguage,
        @RequestParam(required = false, defaultValue = "false") boolean expiredOnly
    ) {
        log.info("DELETE /api/translation/cache - Clearing cache (sourceLanguage={}, targetLanguage={}, expiredOnly={})",
            sourceLanguage, targetLanguage, expiredOnly);

        Map<String, Object> response = new HashMap<>();
        int deletedCount;

        if (expiredOnly) {
            // Only clear expired entries (synchronous for API response)
            deletedCount = cacheRepository.deleteExpiredEntries();
            response.put("action", "CLEARED_EXPIRED");
            response.put("message", "Expired cache entries cleared");
        } else if (sourceLanguage != null && targetLanguage != null) {
            // Clear specific language pair
            deletedCount = cacheRepository.deleteByLanguagePair(sourceLanguage, targetLanguage);
            response.put("action", "CLEARED_LANGUAGE_PAIR");
            response.put("sourceLanguage", sourceLanguage);
            response.put("targetLanguage", targetLanguage);
            response.put("message", String.format("Cache cleared for %s -> %s translations", sourceLanguage, targetLanguage));
        } else {
            // Clear all cache entries
            deletedCount = (int) cacheRepository.count();
            cacheRepository.deleteAll();
            response.put("action", "CLEARED_ALL");
            response.put("message", "All cache entries cleared");
        }

        response.put("deletedEntries", deletedCount);

        log.info("Cache cleared: {} entries deleted", deletedCount);
        return ResponseEntity.ok(ApiResponse.success(200, "Translation cache cleared", response));
    }

    /**
     * Test translation service with a sample text.
     * Useful for verifying the service is working correctly.
     */
    @PostMapping("/test")
    @PreAuthorize("hasAuthority('PERM_TEST_TRANSLATION_SERVICE')")
    public ResponseEntity<ApiResponse<?>> testTranslation(
        @RequestParam(defaultValue = "Welcome to Tanzania Safari!") String text,
        @RequestParam(defaultValue = "en") String sourceLanguage,
        @RequestParam(defaultValue = "fr") String targetLanguage
    ) {
        log.info("POST /api/translation/test - Testing translation from {} to {}", sourceLanguage, targetLanguage);

        Map<String, Object> response = new HashMap<>();
        response.put("originalText", text);
        response.put("sourceLanguage", sourceLanguage);
        response.put("targetLanguage", targetLanguage);

        // Check if provider is available
        if (!providerFactory.hasActiveProvider()) {
            response.put("success", false);
            response.put("error", "No translation provider configured");
            return ResponseEntity.ok(ApiResponse.success(200, "Translation test failed - no provider", response));
        }

        // Check if languages are supported
        if (!settingsService.isLanguageSupported(sourceLanguage)) {
            response.put("success", false);
            response.put("error", "Source language '" + sourceLanguage + "' is not supported");
            return ResponseEntity.ok(ApiResponse.success(200, "Translation test failed - unsupported source language", response));
        }

        if (!settingsService.isLanguageSupported(targetLanguage)) {
            response.put("success", false);
            response.put("error", "Target language '" + targetLanguage + "' is not supported");
            return ResponseEntity.ok(ApiResponse.success(200, "Translation test failed - unsupported target language", response));
        }

        try {
            TranslationProvider provider = providerFactory.getActiveProvider();
            response.put("provider", provider.getProviderType().getDisplayName());

            long startTime = System.currentTimeMillis();
            String translatedText = provider.translate(text, sourceLanguage, targetLanguage);
            long duration = System.currentTimeMillis() - startTime;

            response.put("success", true);
            response.put("translatedText", translatedText);
            response.put("durationMs", duration);

            log.info("Translation test successful: '{}' -> '{}' in {}ms", text, translatedText, duration);
            return ResponseEntity.ok(ApiResponse.success(200, "Translation test successful", response));

        } catch (TranslationProviderException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("errorType", e.getErrorType().name());

            log.warn("Translation test failed: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.success(200, "Translation test failed", response));
        }
    }

    /**
     * Detect the language of a given text.
     */
    @PostMapping("/detect")
    @PreAuthorize("hasAuthority('PERM_TEST_TRANSLATION_SERVICE')")
    public ResponseEntity<ApiResponse<?>> detectLanguage(
        @RequestParam @NotBlank String text
    ) {
        log.info("POST /api/translation/detect - Detecting language for text ({} chars)", text.length());

        Map<String, Object> response = new HashMap<>();
        response.put("text", text.length() > 100 ? text.substring(0, 100) + "..." : text);

        // Check if provider is available
        if (!providerFactory.hasActiveProvider()) {
            response.put("detected", false);
            response.put("error", "No translation provider configured");
            return ResponseEntity.ok(ApiResponse.success(200, "Language detection failed - no provider", response));
        }

        try {
            TranslationProvider provider = providerFactory.getActiveProvider();
            String detectedLanguage = provider.detectLanguage(text);
            response.put("detected", true);
            response.put("language", detectedLanguage);
            response.put("provider", provider.getProviderType().getDisplayName());

            return ResponseEntity.ok(ApiResponse.success(200, "Language detected", response));

        } catch (TranslationProviderException e) {
            response.put("detected", false);
            response.put("error", e.getMessage());

            return ResponseEntity.ok(ApiResponse.success(200, "Language detection failed", response));
        }
    }

    /**
     * Get all translation cache entries with optional filtering, pagination, and sorting.
     *
     * @param page              Page number (0-based), default: 0
     * @param size              Page size, default: 10
     * @param name              Filter by name (partial match, case-insensitive)
     * @param sourceLanguage    Filter by source language (exact match)
     * @param targetLanguage    Filter by target language (exact match)
     * @param contentHash       Filter by content hash (exact match)
     * @param originalContent   Filter by original content (partial match, case-insensitive)
     * @param translatedContent Filter by translated content (partial match, case-insensitive)
     * @param minHitCount       Filter by minimum hit count
     * @param maxHitCount       Filter by maximum hit count
     * @param minCharCount      Filter by minimum character count
     * @param maxCharCount      Filter by maximum character count
     * @param createdAfter      Filter by created after date (ISO format: yyyy-MM-ddTHH:mm:ss)
     * @param createdBefore     Filter by created before date (ISO format: yyyy-MM-ddTHH:mm:ss)
     * @param expired           Filter by expired status (true = expired, false = valid)
     * @param accessed          Filter by accessed status (true = has been accessed, false = never accessed)
     * @param sortBy              Sort field, default: "createdAt"
     * @param sortDirection       Sort direction: "asc" or "desc", default: "desc"
     * @return ResponseEntity with paginated cache entries
     *
     * Example: GET /api/translation/cache/entries?page=0&size=10&name=TRN_CACHE&sourceLanguage=en&targetLanguage=fr&sortBy=createdAt&sortDirection=desc
     */
    @GetMapping("/cache/entries")
    @PreAuthorize("hasAuthority('PERM_READ_TRANSLATION_CACHE')")
    public ResponseEntity<?> getAllCacheEntries(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String sourceLanguage,
        @RequestParam(required = false) String targetLanguage,
        @RequestParam(required = false) String contentHash,
        @RequestParam(required = false) String originalContent,
        @RequestParam(required = false) String translatedContent,
        @RequestParam(required = false) Long minHitCount,
        @RequestParam(required = false) Long maxHitCount,
        @RequestParam(required = false) Integer minCharCount,
        @RequestParam(required = false) Integer maxCharCount,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
        @RequestParam(required = false) Boolean expired,
        @RequestParam(required = false) Boolean accessed,
        /*
         * The house name for the one search box. Without it the list page's search sent
         * `keyword`, Spring dropped it on the floor, and every row came back as if nothing
         * had been typed — a search that silently returns everything.
         */
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/translation/cache/entries - Fetching cache entries with filters");
        return cacheGetterService.getAllCacheEntries(
            page, size, name, sourceLanguage, targetLanguage, contentHash, originalContent, translatedContent,
            minHitCount, maxHitCount, minCharCount, maxCharCount, createdAfter, createdBefore,
            expired, accessed, keyword, includeStats, sortBy, sortDirection
        );
    }

    /**
     * Get a single translation cache entry by ID.
     *
     * @param id Obfuscated cache entry ID
     * @return ResponseEntity with cache entry details or error
     *
     * Example: GET /api/translation/cache/entries/abc123def456
     */
    @GetMapping("/cache/entries/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_TRANSLATION_CACHE')")
    public ResponseEntity<ApiResponse<?>> getCacheEntry(@PathVariable String id) {
        log.info("GET /api/translation/cache/entries/{} - Fetching cache entry", id);
        return cacheGetterService.getCacheEntry(id);
    }
}
