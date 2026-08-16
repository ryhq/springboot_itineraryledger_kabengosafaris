package com.itineraryledger.kabengosafaris.Translation.Cache;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for retrieving TranslationCache entries with filtering, pagination, and sorting.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TranslationCacheGetterService {

    private final TranslationCacheRepository cacheRepository;
    private final IdObfuscator idObfuscator;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;
    private final com.itineraryledger.kabengosafaris.Response.ListStats listStats;

    /**
     * Maximum content length to include in DTO responses.
     * Larger content will be truncated.
     */
    private static final int MAX_CONTENT_LENGTH = 500;

    /**
     * Valid sort fields that map to TranslationCache entity fields.
     */
    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
            "createdAt", "expiresAt", "sourceLanguage", "targetLanguage",
            "characterCount", "hitCount", "lastAccessedAt", "name"
    );

    /**
     * Get all translation cache entries with optional filtering, pagination, and sorting.
     *
     * @param page               Page number (0-based)
     * @param size               Page size
     * @param name               Filter by name (partial match, case-insensitive)
     * @param sourceLanguage     Filter by source language (exact match)
     * @param targetLanguage     Filter by target language (exact match)
     * @param contentHash        Filter by content hash (exact match)
     * @param originalContent    Filter by original content (partial match)
     * @param translatedContent  Filter by translated content (partial match)
     * @param minHitCount        Filter by minimum hit count
     * @param maxHitCount        Filter by maximum hit count
     * @param minCharCount       Filter by minimum character count
     * @param maxCharCount       Filter by maximum character count
     * @param createdAfter       Filter by created after date
     * @param createdBefore      Filter by created before date
     * @param expired            Filter by expired status (true = expired, false = valid)
     * @param accessed           Filter by accessed status (true = has been accessed, false = never accessed)
     * @param sortBy               Sort field (e.g., "createdAt", "hitCount", "characterCount")
     * @param sortDirection        Sort direction ("asc" or "desc")
     * @return ResponseEntity with paginated results or validation error
     */
    public ResponseEntity<?> getAllCacheEntries(
            int page,
            int size,
            String name,
            String sourceLanguage,
            String targetLanguage,
            String contentHash,
            String originalContent,
            String translatedContent,
            Long minHitCount,
            Long maxHitCount,
            Integer minCharCount,
            Integer maxCharCount,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            Boolean expired,
            Boolean accessed,
            Boolean includeStats,
            String sortBy,
            String sortDirection
    ) {
        log.debug("Fetching translation cache entries with filters - page: {}, size: {}, name: {}, sourceLanguage: {}, " +
                        "targetLanguage: {}, contentHash: {}, minHitCount: {}, maxHitCount: {}, " +
                        "minCharCount: {}, maxCharCount: {}, createdAfter: {}, createdBefore: {}, " +
                        "expired: {}, accessed: {}, sortBy: {}, sortDirection: {}",
                page, size, name, sourceLanguage, targetLanguage, contentHash, minHitCount, maxHitCount,
                minCharCount, maxCharCount, createdAfter, createdBefore, expired, accessed, sortBy, sortDirection);

        // Validate pagination parameters
        if (page < 0) {
            log.warn("Invalid page number: {}", page);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Page number cannot be negative", "INVALID_PAGE")
            );
        }
        if (size <= 0) {
            log.warn("Invalid page size: {}", size);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Page size must be greater than 0", "INVALID_SIZE")
            );
        }

        // Validate sort field
        String effectiveSortBy = (sortBy != null && !sortBy.isBlank()) ? sortBy : "createdAt";
        if (!VALID_SORT_FIELDS.contains(effectiveSortBy)) {
            log.warn("Invalid sort field: {}", sortBy);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
            );
        }

        // Setup sorting
        Sort.Direction direction = Sort.Direction.DESC;
        if ("asc".equalsIgnoreCase(sortDirection)) {
            direction = Sort.Direction.ASC;
        }

        Pageable paging = PageRequest.of(page, size, Sort.by(direction, effectiveSortBy));

        // Build dynamic specification
        Specification<TranslationCache> specification = Specification.unrestricted();

        if (name != null && !name.isBlank()) {
            specification = specification.and(TranslationCacheSpecification.nameLike(name));
        }

        if (sourceLanguage != null && !sourceLanguage.isBlank()) {
            specification = specification.and(TranslationCacheSpecification.hasSourceLanguage(sourceLanguage));
        }

        if (targetLanguage != null && !targetLanguage.isBlank()) {
            specification = specification.and(TranslationCacheSpecification.hasTargetLanguage(targetLanguage));
        }

        if (contentHash != null && !contentHash.isBlank()) {
            specification = specification.and(TranslationCacheSpecification.hasContentHash(contentHash));
        }

        if (originalContent != null && !originalContent.isBlank()) {
            specification = specification.and(TranslationCacheSpecification.originalContentLike(originalContent));
        }

        if (translatedContent != null && !translatedContent.isBlank()) {
            specification = specification.and(TranslationCacheSpecification.translatedContentLike(translatedContent));
        }

        if (minHitCount != null) {
            specification = specification.and(TranslationCacheSpecification.hitCountGreaterThanOrEqual(minHitCount));
        }

        if (maxHitCount != null) {
            specification = specification.and(TranslationCacheSpecification.hitCountLessThanOrEqual(maxHitCount));
        }

        if (minCharCount != null) {
            specification = specification.and(TranslationCacheSpecification.characterCountGreaterThanOrEqual(minCharCount));
        }

        if (maxCharCount != null) {
            specification = specification.and(TranslationCacheSpecification.characterCountLessThanOrEqual(maxCharCount));
        }

        if (createdAfter != null) {
            specification = specification.and(TranslationCacheSpecification.createdAfter(createdAfter));
        }

        if (createdBefore != null) {
            specification = specification.and(TranslationCacheSpecification.createdBefore(createdBefore));
        }

        if (expired != null) {
            specification = specification.and(TranslationCacheSpecification.isExpired(expired));
        }

        if (accessed != null) {
            specification = specification.and(TranslationCacheSpecification.hasBeenAccessed(accessed));
        }

        // Execute query with specifications
        Page<TranslationCache> pagedCacheEntries = cacheRepository.findAll(specification, paging);

        // Convert to DTOs
        List<TranslationCacheDTO> cacheDTOs = getCacheDTOs(pagedCacheEntries.getContent());

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("cacheEntries", cacheDTOs);
        response.put("currentPage", pagedCacheEntries.getNumber());
        response.put("totalItems", pagedCacheEntries.getTotalElements());
        response.put("totalPages", pagedCacheEntries.getTotalPages());
        response.put("sortBy", effectiveSortBy);
        response.put("sortDirection", direction.name().toLowerCase());
        response.put("validSortFields", VALID_SORT_FIELDS);
        response.put("hasNext", pagedCacheEntries.hasNext());
        response.put("hasPrevious", pagedCacheEntries.hasPrevious());
        response.put("pageSize", pagedCacheEntries.getSize());
        response.put("currentSortBy", effectiveSortBy);
        response.put("currentSortDirection", direction.name().toLowerCase());
        /*
         * Counters for the WHOLE filtered set, from the same specification as the rows. This
         * list is 15,000 rows long, so a page-scope-only summary was answering questions
         * about ten of them and saying so — true, but almost never what was asked.
         */
        if (!Boolean.FALSE.equals(includeStats)) {
            response.put("stats", buildStats(specification));
        }
        // the languages actually present, so the filter can only offer values that match
        response.put("targetLanguages", cacheRepository.distinctTargetLanguages());

        log.info("Successfully fetched {} translation cache entries on page {}", cacheDTOs.size(), page);
        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "Successfully retrieved translation cache entries.",
                        response
                )
        );
    }

    /**
     * Get a single translation cache entry by obfuscated ID.
     *
     * @param idObfuscated The obfuscated cache entry ID
     * @return ResponseEntity with ApiResponse containing cache entry or error
     */
    /**
     * The cards over the cache.
     *
     * Every one of them answers "is this cache earning its keep": what has lapsed and will
     * be paid for again, what was translated once and never reused, and what is carrying the
     * load. All reachable as filters, all from the same specification as the rows.
     */
    private Map<String, Object> buildStats(Specification<TranslationCache> spec) {
        return listStats.of(TranslationCache.class, spec)
            .total()
            .count("expired", TranslationCacheSpecification.isExpired(true))
            .complement("valid", "expired")
            .count("neverReused", TranslationCacheSpecification.hitCountLessThanOrEqual(0L))
            .count("reusedOnce", TranslationCacheSpecification.hitCountGreaterThanOrEqual(1L))
            .count("reused10Plus", TranslationCacheSpecification.hitCountGreaterThanOrEqual(10L))
            .count("reused100Plus", TranslationCacheSpecification.hitCountGreaterThanOrEqual(100L))
            /*
             * Long entries are where the money is: providers bill per character, so one
             * 1,000-character paragraph reused ten times saves more than a hundred one-word
             * labels.
             */
            .count("longEntries", TranslationCacheSpecification.characterCountGreaterThanOrEqual(500))
            .count("neverAccessed", TranslationCacheSpecification.hasBeenAccessed(false))
            .recency(TranslationCacheSpecification::createdAfter)
            .build();
    }

    public ResponseEntity<ApiResponse<?>> getCacheEntry(String idObfuscated) {
        try {
            // Decode obfuscated ID
            Long id = idObfuscator.decodeId(idObfuscated);

            // Find cache entry
            TranslationCache cacheEntry = cacheRepository.findById(id).orElse(null);

            if (cacheEntry == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error(404, "Translation cache entry not found", "RESOURCE_NOT_FOUND")
                );
            }

            /*
             * Ordered newest-first like the list, and reporting the position — arrows with no
             * "3 of 40" between them leave somebody paging through 15,000 entries with no
             * idea where they are or when it ends.
             */
            Map<String, Object> nav = recordNavigation.navigate(
                TranslationCache.class, Specification.unrestricted(), "createdAt", false, id);
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("cacheEntry", convertToDTO(cacheEntry, false));
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            log.info("Successfully retrieved translation cache entry {}", id);
            return ResponseEntity.ok(
                    ApiResponse.success(
                            200,
                            "Successfully retrieved translation cache entry.",
                            response
                    )
            );
        } catch (Exception e) {
            log.error("Error getting translation cache entry", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to get translation cache entry", "GET_CACHE_ENTRY_FAILED")
            );
        }
    }

    /**
     * Convert list of TranslationCache entities to DTOs.
     */
    private List<TranslationCacheDTO> getCacheDTOs(List<TranslationCache> cacheEntries) {
        return cacheEntries.stream()
                .map(entry -> convertToDTO(entry, true))
                .toList();
    }

    /**
     * Convert TranslationCache entity to DTO with obfuscated ID.
     *
     * @param cacheEntry The cache entry to convert
     * @param truncateContent Whether to truncate large content
     * @return TranslationCacheDTO
     */
    private TranslationCacheDTO convertToDTO(TranslationCache cacheEntry, boolean truncateContent) {
        String originalContent = cacheEntry.getOriginalContent();
        String translatedContent = cacheEntry.getTranslatedContent();
        boolean originalTruncated = false;
        boolean translatedTruncated = false;

        if (truncateContent) {
            if (originalContent != null && originalContent.length() > MAX_CONTENT_LENGTH) {
                originalContent = originalContent.substring(0, MAX_CONTENT_LENGTH) + "...";
                originalTruncated = true;
            }
            if (translatedContent != null && translatedContent.length() > MAX_CONTENT_LENGTH) {
                translatedContent = translatedContent.substring(0, MAX_CONTENT_LENGTH) + "...";
                translatedTruncated = true;
            }
        }

        return TranslationCacheDTO.builder()
                .id(idObfuscator.encodeId(cacheEntry.getId()))
                .name(cacheEntry.getName())
                .contentHash(cacheEntry.getContentHash())
                .sourceLanguage(cacheEntry.getSourceLanguage())
                .targetLanguage(cacheEntry.getTargetLanguage())
                .originalContent(originalContent)
                .translatedContent(translatedContent)
                .originalContentTruncated(originalTruncated)
                .translatedContentTruncated(translatedTruncated)
                .characterCount(cacheEntry.getCharacterCount())
                .hitCount(cacheEntry.getHitCount())
                .createdAt(cacheEntry.getCreatedAt())
                .expiresAt(cacheEntry.getExpiresAt())
                .lastAccessedAt(cacheEntry.getLastAccessedAt())
                .isExpired(cacheEntry.isExpired())
                .build();
    }
}
