package com.itineraryledger.kabengosafaris.Translation.Cache;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity class to cache translations to reduce API calls to LibreTranslate.
 * Uses SHA-256 hash of (sourceLanguage + targetLanguage + content) as the cache key.
 */
@Entity
@Table(name = "translation_cache", indexes = {
    @Index(name = "idx_translation_cache_hash", columnList = "content_hash"),
    @Index(name = "idx_translation_cache_languages", columnList = "source_language, target_language"),
    @Index(name = "idx_translation_cache_expires", columnList = "expires_at"),
    @Index(name = "idx_translation_cache_name", columnList = "name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Auto-generated unique name for the cache entry.
     * Format: TRN_CACHE_{####}{MM}{YY}
     * Example: TRN_CACHE_00011224 (1st entry in December 2024)
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * SHA-256 hash of the original content + source + target language
     * Used as the cache lookup key for fast retrieval
     */
    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    /**
     * Source language code (e.g., 'en')
     */
    @Column(name = "source_language", nullable = false, length = 10)
    private String sourceLanguage;

    /**
     * Target language code (e.g., 'fr')
     */
    @Column(name = "target_language", nullable = false, length = 10)
    private String targetLanguage;

    /**
     * Original content that was translated
     * Stored for debugging/verification purposes
     */
    @Column(name = "original_content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String originalContent;

    /**
     * Translated content
     */
    @Column(name = "translated_content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String translatedContent;

    /**
     * Number of characters in the original content
     */
    @Column(name = "character_count", nullable = false)
    private Integer characterCount;

    /**
     * Number of times this cached translation was used
     */
    @Column(name = "hit_count", nullable = false)
    @Builder.Default
    private Long hitCount = 0L;

    /**
     * Timestamp when this translation was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this cache entry expires
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Timestamp when this cache was last accessed
     */
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    /**
     * Increment hit count and update last accessed timestamp
     */
    public void recordHit() {
        this.hitCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * Check if this cache entry has expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
