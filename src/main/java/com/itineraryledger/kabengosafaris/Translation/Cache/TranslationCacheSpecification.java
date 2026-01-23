package com.itineraryledger.kabengosafaris.Translation.Cache;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * TranslationCacheSpecification - Provides reusable Specification objects for filtering TranslationCache entities
 *
 * Specification allows for dynamic, type-safe query construction using the Criteria API
 * Each method returns a Specification<TranslationCache> that can be combined with other specifications
 */
public class TranslationCacheSpecification {

    /**
     * Filter by name (case-insensitive partial match)
     */
    public static Specification<TranslationCache> nameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isEmpty()) {
                return cb.conjunction(); // No filter applied
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    /**
     * Filter by source language (exact match)
     */
    public static Specification<TranslationCache> hasSourceLanguage(String sourceLanguage) {
        return (root, query, cb) -> {
            if (sourceLanguage == null || sourceLanguage.isEmpty()) {
                return cb.conjunction(); // No filter applied
            }
            return cb.equal(cb.lower(root.get("sourceLanguage")), sourceLanguage.toLowerCase());
        };
    }

    /**
     * Filter by target language (exact match)
     */
    public static Specification<TranslationCache> hasTargetLanguage(String targetLanguage) {
        return (root, query, cb) -> {
            if (targetLanguage == null || targetLanguage.isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(cb.lower(root.get("targetLanguage")), targetLanguage.toLowerCase());
        };
    }

    /**
     * Filter by content hash (exact match)
     */
    public static Specification<TranslationCache> hasContentHash(String contentHash) {
        return (root, query, cb) -> {
            if (contentHash == null || contentHash.isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("contentHash"), contentHash);
        };
    }

    /**
     * Filter by original content (case-insensitive partial match)
     */
    public static Specification<TranslationCache> originalContentLike(String originalContent) {
        return (root, query, cb) -> {
            if (originalContent == null || originalContent.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("originalContent")), "%" + originalContent.toLowerCase() + "%");
        };
    }

    /**
     * Filter by translated content (case-insensitive partial match)
     */
    public static Specification<TranslationCache> translatedContentLike(String translatedContent) {
        return (root, query, cb) -> {
            if (translatedContent == null || translatedContent.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("translatedContent")), "%" + translatedContent.toLowerCase() + "%");
        };
    }

    /**
     * Filter by minimum hit count
     */
    public static Specification<TranslationCache> hitCountGreaterThanOrEqual(Long minHitCount) {
        return (root, query, cb) -> {
            if (minHitCount == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("hitCount"), minHitCount);
        };
    }

    /**
     * Filter by maximum hit count
     */
    public static Specification<TranslationCache> hitCountLessThanOrEqual(Long maxHitCount) {
        return (root, query, cb) -> {
            if (maxHitCount == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("hitCount"), maxHitCount);
        };
    }

    /**
     * Filter by minimum character count
     */
    public static Specification<TranslationCache> characterCountGreaterThanOrEqual(Integer minCharCount) {
        return (root, query, cb) -> {
            if (minCharCount == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("characterCount"), minCharCount);
        };
    }

    /**
     * Filter by maximum character count
     */
    public static Specification<TranslationCache> characterCountLessThanOrEqual(Integer maxCharCount) {
        return (root, query, cb) -> {
            if (maxCharCount == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("characterCount"), maxCharCount);
        };
    }

    /**
     * Filter by created after date
     */
    public static Specification<TranslationCache> createdAfter(LocalDateTime createdAfter) {
        return (root, query, cb) -> {
            if (createdAfter == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("createdAt"), createdAfter);
        };
    }

    /**
     * Filter by created before date
     */
    public static Specification<TranslationCache> createdBefore(LocalDateTime createdBefore) {
        return (root, query, cb) -> {
            if (createdBefore == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), createdBefore);
        };
    }

    /**
     * Filter by expired status (expired entries where expiresAt < now)
     */
    public static Specification<TranslationCache> isExpired(Boolean expired) {
        return (root, query, cb) -> {
            if (expired == null) {
                return cb.conjunction();
            }
            LocalDateTime now = LocalDateTime.now();
            if (expired) {
                return cb.lessThan(root.get("expiresAt"), now);
            } else {
                return cb.greaterThanOrEqualTo(root.get("expiresAt"), now);
            }
        };
    }

    /**
     * Filter by expires after date
     */
    public static Specification<TranslationCache> expiresAfter(LocalDateTime expiresAfter) {
        return (root, query, cb) -> {
            if (expiresAfter == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("expiresAt"), expiresAfter);
        };
    }

    /**
     * Filter by expires before date
     */
    public static Specification<TranslationCache> expiresBefore(LocalDateTime expiresBefore) {
        return (root, query, cb) -> {
            if (expiresBefore == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("expiresAt"), expiresBefore);
        };
    }

    /**
     * Filter by last accessed after date
     */
    public static Specification<TranslationCache> lastAccessedAfter(LocalDateTime lastAccessedAfter) {
        return (root, query, cb) -> {
            if (lastAccessedAfter == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("lastAccessedAt"), lastAccessedAfter);
        };
    }

    /**
     * Filter for entries that have been accessed (hitCount > 0)
     */
    public static Specification<TranslationCache> hasBeenAccessed(Boolean accessed) {
        return (root, query, cb) -> {
            if (accessed == null) {
                return cb.conjunction();
            }
            if (accessed) {
                return cb.greaterThan(root.get("hitCount"), 0L);
            } else {
                return cb.equal(root.get("hitCount"), 0L);
            }
        };
    }
}
