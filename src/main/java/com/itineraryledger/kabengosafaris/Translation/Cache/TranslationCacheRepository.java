package com.itineraryledger.kabengosafaris.Translation.Cache;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository interface for TranslationCache entity.
 * Provides database access operations for translation caching.
 */
@Repository
public interface TranslationCacheRepository extends JpaRepository<TranslationCache, Long>, JpaSpecificationExecutor<TranslationCache> {

    /**
     * Find cached translation by content hash and languages
     * @param contentHash SHA-256 hash of the content
     * @param sourceLanguage source language code
     * @param targetLanguage target language code
     * @return Optional containing the cached translation if found and not expired
     */
    @Query("SELECT tc FROM TranslationCache tc WHERE tc.contentHash = :contentHash " +
           "AND tc.sourceLanguage = :sourceLanguage AND tc.targetLanguage = :targetLanguage " +
           "AND tc.expiresAt > CURRENT_TIMESTAMP")
    Optional<TranslationCache> findValidCacheEntry(
        @Param("contentHash") String contentHash,
        @Param("sourceLanguage") String sourceLanguage,
        @Param("targetLanguage") String targetLanguage
    );

    /**
     * Find cached translation by content hash only (for any language pair)
     * @param contentHash SHA-256 hash of the content
     * @param sourceLanguage source language code
     * @param targetLanguage target language code
     * @return Optional containing the cached translation if found
     */
    Optional<TranslationCache> findByContentHashAndSourceLanguageAndTargetLanguage(
        String contentHash, String sourceLanguage, String targetLanguage
    );

    /**
     * Check if a valid (non-expired) cache entry exists
     * @param contentHash SHA-256 hash of the content
     * @param sourceLanguage source language code
     * @param targetLanguage target language code
     * @return true if valid cache exists
     */
    @Query("SELECT COUNT(tc) > 0 FROM TranslationCache tc WHERE tc.contentHash = :contentHash " +
           "AND tc.sourceLanguage = :sourceLanguage AND tc.targetLanguage = :targetLanguage " +
           "AND tc.expiresAt > CURRENT_TIMESTAMP")
    boolean existsValidCacheEntry(
        @Param("contentHash") String contentHash,
        @Param("sourceLanguage") String sourceLanguage,
        @Param("targetLanguage") String targetLanguage
    );

    /**
     * Delete all expired cache entries
     * @return number of deleted entries
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM TranslationCache tc WHERE tc.expiresAt < CURRENT_TIMESTAMP")
    int deleteExpiredEntries();

    /**
     * Delete cache entries older than specified date
     * @param beforeDate delete entries created before this date
     * @return number of deleted entries
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM TranslationCache tc WHERE tc.createdAt < :beforeDate")
    int deleteEntriesOlderThan(@Param("beforeDate") LocalDateTime beforeDate);

    /**
     * Delete all cache entries for a specific language pair
     * @param sourceLanguage source language code
     * @param targetLanguage target language code
     * @return number of deleted entries
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM TranslationCache tc WHERE tc.sourceLanguage = :sourceLanguage AND tc.targetLanguage = :targetLanguage")
    int deleteByLanguagePair(
        @Param("sourceLanguage") String sourceLanguage,
        @Param("targetLanguage") String targetLanguage
    );

    /**
     * Count total cached translations
     * @return total count
     */
    @Query("SELECT COUNT(tc) FROM TranslationCache tc WHERE tc.expiresAt > CURRENT_TIMESTAMP")
    long countValidEntries();

    /**
     * Count total cache hits
     * @return sum of all hit counts
     */
    @Query("SELECT COALESCE(SUM(tc.hitCount), 0) FROM TranslationCache tc")
    long countTotalHits();

    /**
     * Get total characters saved by caching (sum of character_count * hit_count)
     * @return total characters saved
     */
    @Query("SELECT COALESCE(SUM(tc.characterCount * tc.hitCount), 0) FROM TranslationCache tc WHERE tc.hitCount > 0")
    long countCharactersSaved();

    /**
     * Check if a cache entry with the given name exists
     * @param name the name to check
     * @return true if exists
     */
    boolean existsByName(String name);

    /**
     * Count cache entries created in a specific year and month
     * Used for generating sequential names
     * @param year the year
     * @param month the month (1-12)
     * @return count of entries
     */
    @Query("SELECT COUNT(tc) FROM TranslationCache tc WHERE YEAR(tc.createdAt) = :year AND MONTH(tc.createdAt) = :month")
    long countByYearAndMonth(@Param("year") int year, @Param("month") int month);

    /**
     * Find cache entry by name
     * @param name the unique name
     * @return Optional containing the cache entry if found
     */
    Optional<TranslationCache> findByName(String name);

    /**
     * Find the next cache entry ID (circular navigation)
     */
    @Query("SELECT tc.id FROM TranslationCache tc WHERE tc.id > :currentId ORDER BY tc.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    /**
     * Find the previous cache entry ID (circular navigation)
     */
    @Query("SELECT tc.id FROM TranslationCache tc WHERE tc.id < :currentId ORDER BY tc.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    /**
     * Find the first cache entry ID (for wrapping from last to first)
     */
    @Query("SELECT tc.id FROM TranslationCache tc ORDER BY tc.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    /**
     * Find the last cache entry ID (for wrapping from first to last)
     */
    @Query("SELECT tc.id FROM TranslationCache tc ORDER BY tc.id DESC LIMIT 1")
    Optional<Long> findLastId();

    /*
     * The languages this cache actually holds, for the filter dropdown. DISTINCT over the
     * rows rather than a list written into the client: the provider offers 101 languages and
     * this installation has used a handful, so offering all 101 would be 95 filters that
     * match nothing.
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT DISTINCT c.targetLanguage FROM TranslationCache c WHERE c.targetLanguage IS NOT NULL ORDER BY c.targetLanguage ASC")
    java.util.List<String> distinctTargetLanguages();
}
