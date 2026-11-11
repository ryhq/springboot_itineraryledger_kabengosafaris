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
     * Which provider produced {@link #translatedContent}.
     *
     * <p>Null means the row predates provenance tracking and nothing recorded it — NOT that it
     * came from no provider. Those rows are the re-translation candidates after a provider
     * change, which is the whole reason this column exists: without it, switching engine leaves
     * you unable to tell which of tens of thousands of entries are the old engine's work, and
     * since the cache is consulted before the provider, the new engine is never asked for them.
     */
    @Column(name = "provider", length = 40)
    private String provider;

    /**
     * Somebody edited this translation by hand.
     *
     * <p>Set only by the correction endpoint, never by a provider write.
     */
    @Column(name = "human_corrected", nullable = false)
    @Builder.Default
    private Boolean humanCorrected = false;

    /**
     * A person has read this translation and stands behind it.
     *
     * <p>Set when it is corrected, and settable on its own for "I read it, it was already
     * right". **A verified entry is never overwritten by a provider and never expires** — see
     * {@link #isExpired()}. Without that, a correction survives only until its TTL lapses and
     * the next request quietly replaces it with the machine's version again, with nothing
     * anywhere reporting that the fix was undone.
     */
    @Column(name = "verified", nullable = false)
    @Builder.Default
    private Boolean verified = false;

    /** Who made the correction — username, for the audit trail. */
    @Column(name = "corrected_by", length = 255)
    private String correctedBy;

    /** When the correction was made. */
    @Column(name = "corrected_at")
    private LocalDateTime correctedAt;

    /**
     * Increment hit count and update last accessed timestamp
     */
    public void recordHit() {
        this.hitCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * Check if this cache entry has expired.
     *
     * <p>A verified entry never does. Expiry exists so machine output is refreshed; a human
     * answer does not go stale, and letting it lapse would hand the string back to the provider
     * to get wrong again.
     */
    public boolean isExpired() {
        if (Boolean.TRUE.equals(this.verified)) {
            return false;
        }
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    /** True when a provider may replace this row's translation. */
    public boolean isProviderWritable() {
        return !Boolean.TRUE.equals(this.verified) && !Boolean.TRUE.equals(this.humanCorrected);
    }
}
