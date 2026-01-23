package com.itineraryledger.kabengosafaris.Translation.Cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for TranslationCache entity used in API responses.
 * Contains obfuscated ID and excludes sensitive internal data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationCacheDTO {

    /**
     * Obfuscated cache entry ID
     */
    private String id;

    /**
     * Auto-generated unique name for the cache entry.
     * Format: TRN_CACHE_{####}{MM}{YY}
     */
    private String name;

    /**
     * SHA-256 hash of the original content + source + target language
     */
    private String contentHash;

    /**
     * Source language code (e.g., 'en')
     */
    private String sourceLanguage;

    /**
     * Target language code (e.g., 'fr')
     */
    private String targetLanguage;

    /**
     * Original content that was translated (may be truncated for large content)
     */
    private String originalContent;

    /**
     * Translated content (may be truncated for large content)
     */
    private String translatedContent;

    /**
     * Whether the original content was truncated in this DTO
     */
    private Boolean originalContentTruncated;

    /**
     * Whether the translated content was truncated in this DTO
     */
    private Boolean translatedContentTruncated;

    /**
     * Number of characters in the original content
     */
    private Integer characterCount;

    /**
     * Number of times this cached translation was used
     */
    private Long hitCount;

    /**
     * Timestamp when this translation was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when this cache entry expires
     */
    private LocalDateTime expiresAt;

    /**
     * Timestamp when this cache was last accessed
     */
    private LocalDateTime lastAccessedAt;

    /**
     * Whether this cache entry is expired
     */
    private Boolean isExpired;
}
