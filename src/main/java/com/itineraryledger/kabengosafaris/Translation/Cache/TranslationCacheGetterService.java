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

    /**
     * Maximum content length to include in DTO responses.
     * Larger content will be truncated.
     */
    private static final int MAX_CONTENT_LENGTH = 500;

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
     * @param sortDirection            Sort direction ("asc" or "desc")
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
            String sortDirection
    ) {
        log.debug("Fetching translation cache entries with filters - page: {}, size: {}, name: {}, sourceLanguage: {}, " +
                        "targetLanguage: {}, contentHash: {}, minHitCount: {}, maxHitCount: {}, " +
                        "minCharCount: {}, maxCharCount: {}, createdAfter: {}, createdBefore: {}, " +
                        "expired: {}, accessed: {}, sortDirection: {}",
                page, size, name, sourceLanguage, targetLanguage, contentHash, minHitCount, maxHitCount,
                minCharCount, maxCharCount, createdAfter, createdBefore, expired, accessed, sortDirection);

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

        // Setup sorting (always sort by createdAt)
        Sort.Direction direction = Sort.Direction.DESC;
        if ("asc".equalsIgnoreCase(sortDirection)) {
            direction = Sort.Direction.ASC;
        }

        Pageable paging = PageRequest.of(page, size, Sort.by(direction, "createdAt"));

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

            log.info("Successfully retrieved translation cache entry {}", id);
            return ResponseEntity.ok(
                    ApiResponse.success(
                            200,
                            "Successfully retrieved translation cache entry.",
                            convertToDTO(cacheEntry, false) // Don't truncate for single entry view
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
