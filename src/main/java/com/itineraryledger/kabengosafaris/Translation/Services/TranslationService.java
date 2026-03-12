package com.itineraryledger.kabengosafaris.Translation.Services;

import com.itineraryledger.kabengosafaris.Translation.Cache.TranslationCache;
import com.itineraryledger.kabengosafaris.Translation.Cache.TranslationCacheRepository;
import com.itineraryledger.kabengosafaris.Translation.Settings.TranslationSettingGetterServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main Translation Service that orchestrates translation with caching.
 * This service:
 * - Checks cache before making API calls
 * - Handles text chunking for large content
 * - Falls back to original text on errors
 * - Manages cache storage and cleanup
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationService {

    private final LibreTranslateService libreTranslateService;
    private final TranslationCacheRepository cacheRepository;
    private final TranslationSettingGetterServices settingsService;
    private final TranslationCacheAsyncService cacheAsyncService;

    /**
     * Translate HTML content to the target language.
     * This is the main entry point for PDF translation.
     *
     * @param htmlContent the HTML content to translate
     * @param targetLanguage target language code (e.g., "fr", "de")
     * @return translated HTML content, or original content on error
     */
    public String translateHtml(String htmlContent, String targetLanguage) {
        return translateHtml(htmlContent, null, targetLanguage);
    }

    /**
     * Translate HTML content to the target language with specified source language.
     * For full HTML documents (with DOCTYPE), only the body content is translated
     * while preserving DOCTYPE, head, and HTML structure.
     *
     * @param htmlContent the HTML content to translate
     * @param sourceLanguage source language code (null for auto-detect or default)
     * @param targetLanguage target language code
     * @return translated HTML content, or original content on error
     */
    public String translateHtml(String htmlContent, String sourceLanguage, String targetLanguage) {
        // Return original if empty or null
        if (htmlContent == null || htmlContent.isBlank()) {
            return htmlContent;
        }

        // Return original if target is English (source language)
        if (targetLanguage == null || targetLanguage.isBlank() || "en".equalsIgnoreCase(targetLanguage)) {
            log.debug("Target language is English or not specified, returning original content");
            return htmlContent;
        }

        // Validate target language is supported in settings
        if (!settingsService.isLanguageSupported(targetLanguage)) {
            log.warn("Target language '{}' is not in supported languages list. Returning original content.", targetLanguage);
            return htmlContent;
        }

        // Also verify LibreTranslate actually supports this language
        if (!libreTranslateService.isLanguageSupportedByLibreTranslate(targetLanguage)) {
            log.warn("Target language '{}' is not supported by LibreTranslate instance. Returning original content.", targetLanguage);
            return htmlContent;
        }

        // Use default source language if not specified
        if (sourceLanguage == null || sourceLanguage.isBlank()) {
            sourceLanguage = settingsService.getDefaultSourceLanguage();
        }

        // Return original if source and target are the same
        if (sourceLanguage.equalsIgnoreCase(targetLanguage)) {
            log.debug("Source and target languages are the same, returning original content");
            return htmlContent;
        }

        try {
            // Check if LibreTranslate is enabled
            if (!settingsService.isLibreTranslateEnabled()) {
                log.info("LibreTranslate is disabled. Returning original content.");
                return htmlContent;
            }

            // For full HTML documents, only translate the body content
            // This preserves DOCTYPE, head, meta tags, styles, etc.
            if (isFullHtmlDocument(htmlContent)) {
                return translateFullHtmlDocument(htmlContent, sourceLanguage, targetLanguage);
            }

            // For HTML fragments, translate the entire content
            return translateContent(htmlContent, sourceLanguage, targetLanguage);

        } catch (LibreTranslateService.TranslationException e) {
            log.error("Translation failed: {}. Returning original content.", e.getMessage());
            return htmlContent; // Fallback to original
        } catch (Exception e) {
            log.error("Unexpected error during translation: {}. Returning original content.", e.getMessage(), e);
            return htmlContent; // Fallback to original
        }
    }

    /**
     * Check if content is a full HTML document (has DOCTYPE or html tag).
     */
    private boolean isFullHtmlDocument(String content) {
        String lowerContent = content.toLowerCase().trim();
        return lowerContent.startsWith("<!doctype") || lowerContent.startsWith("<html");
    }

    /**
     * Translate a full HTML document by extracting and translating only safe text content.
     * Preserves DOCTYPE, head, SVG elements, scripts, styles, and HTML structure.
     * @throws LibreTranslateService.TranslationException if translation fails
     */
    private String translateFullHtmlDocument(String htmlContent, String sourceLanguage, String targetLanguage)
            throws LibreTranslateService.TranslationException {
        // Pattern to match body content (case-insensitive)
        Pattern bodyPattern = Pattern.compile(
            "(<body[^>]*>)(.*?)(</body>)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        Matcher matcher = bodyPattern.matcher(htmlContent);

        if (!matcher.find()) {
            // No body tag found, translate entire content
            log.debug("No body tag found in HTML document, translating entire content");
            return translateContent(htmlContent, sourceLanguage, targetLanguage);
        }

        String beforeBody = htmlContent.substring(0, matcher.start());
        String bodyOpenTag = matcher.group(1);
        String bodyContent = matcher.group(2);
        String bodyCloseTag = matcher.group(3);
        String afterBody = htmlContent.substring(matcher.end());

        log.debug("Translating body content only ({} chars), preserving HTML structure", bodyContent.length());

        // Translate only the safe body content (excluding SVG, script, style)
        String translatedBody = translateBodyContentSafely(bodyContent, sourceLanguage, targetLanguage);

        // Reassemble the document
        return beforeBody + bodyOpenTag + translatedBody + bodyCloseTag + afterBody;
    }

    /**
     * Translate body content by extracting text nodes, translating them, and reinserting.
     * This preserves ALL HTML structure including tags and attributes.
     *
     * Uses a two-phase approach:
     * 1. Check cache for each segment individually (cache-friendly)
     * 2. Batch translate only uncached segments, then cache them individually
     */
    private String translateBodyContentSafely(String bodyContent, String sourceLanguage, String targetLanguage)
            throws LibreTranslateService.TranslationException {

        // Extract text segments and their positions
        List<TextSegment> textSegments = extractTextSegments(bodyContent);

        if (textSegments.isEmpty()) {
            log.debug("No translatable text found in body content");
            return bodyContent;
        }

        log.debug("Found {} text segments to translate", textSegments.size());

        // Phase 1: Check cache for each segment and collect uncached segments
        String[] translatedSegments = new String[textSegments.size()];
        List<Integer> uncachedIndices = new ArrayList<>();

        for (int i = 0; i < textSegments.size(); i++) {
            String segmentText = textSegments.get(i).text;

            if (settingsService.isCacheEnabled()) {
                Optional<String> cached = getFromCache(segmentText, sourceLanguage, targetLanguage);
                if (cached.isPresent()) {
                    translatedSegments[i] = cached.get();
                    continue;
                }
            }

            // Not in cache, mark for batch translation
            uncachedIndices.add(i);
        }

        log.debug("Cache hits: {}, segments to translate: {}",
            textSegments.size() - uncachedIndices.size(), uncachedIndices.size());

        // Phase 2: Translate uncached segments individually
        // We translate each segment separately to avoid separator corruption issues
        if (!uncachedIndices.isEmpty()) {
            for (int idx : uncachedIndices) {
                String originalText = textSegments.get(idx).text;

                try {
                    // Translate this segment individually
                    String translatedText = translateSegmentDirectly(originalText, sourceLanguage, targetLanguage);
                    translatedSegments[idx] = translatedText;

                    // Cache each segment individually
                    if (settingsService.isCacheEnabled()) {
                        cacheAsyncService.saveToCacheAsync(originalText, translatedText, sourceLanguage, targetLanguage);
                    }
                } catch (LibreTranslateService.TranslationException e) {
                    log.warn("Failed to translate segment: {}. Using original.", e.getMessage());
                    translatedSegments[idx] = originalText;
                }
            }
        }

        // Phase 3: Rebuild content with translated text
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        for (int i = 0; i < textSegments.size(); i++) {
            TextSegment segment = textSegments.get(i);

            // Add HTML before this text segment
            result.append(bodyContent, lastEnd, segment.start);

            // Add translated text
            result.append(translatedSegments[i]);

            lastEnd = segment.end;
        }

        // Add remaining HTML after last text segment
        result.append(bodyContent.substring(lastEnd));

        return result.toString();
    }

    /**
     * Translate a single text segment directly via LibreTranslate without caching.
     * Used for individual segment translations where caching is handled separately.
     */
    private String translateSegmentDirectly(String content, String sourceLanguage, String targetLanguage)
            throws LibreTranslateService.TranslationException {
        // Check if content needs chunking (for very large segments)
        int maxChars = settingsService.getMaxCharacters();
        if (content.length() > maxChars) {
            return translateLargeSegmentDirectly(content, sourceLanguage, targetLanguage);
        }

        // Translate via LibreTranslate (no caching here - handled by caller)
        return libreTranslateService.translate(content, sourceLanguage, targetLanguage);
    }

    /**
     * Translate large segment by chunking, without caching.
     * Used for segments that exceed max character limit.
     */
    private String translateLargeSegmentDirectly(String content, String sourceLanguage, String targetLanguage) {
        int chunkSize = settingsService.getChunkSize();
        List<String> chunks = splitIntoChunks(content, chunkSize);

        log.info("Translating large segment ({} chars) in {} chunks", content.length(), chunks.size());

        StringBuilder translatedContent = new StringBuilder();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);

            try {
                String translatedChunk = libreTranslateService.translate(chunk, sourceLanguage, targetLanguage);
                translatedContent.append(translatedChunk);
                log.debug("Segment chunk {}/{} translated successfully", i + 1, chunks.size());

            } catch (LibreTranslateService.TranslationException e) {
                log.warn("Failed to translate segment chunk {}/{}: {}. Using original.", i + 1, chunks.size(), e.getMessage());
                translatedContent.append(chunk); // Use original chunk on error
            }
        }

        return translatedContent.toString();
    }

    /**
     * Extract text segments from HTML, excluding content inside tags and certain elements.
     */
    private List<TextSegment> extractTextSegments(String html) {
        List<TextSegment> segments = new ArrayList<>();

        // Track position and whether we're inside a tag or skip element
        int pos = 0;
        int textStart = -1;
        boolean inTag = false;
        boolean inSkipElement = false;
        String skipElementName = null;

        // Elements whose content should not be translated
        java.util.Set<String> skipElements = java.util.Set.of(
            "script", "style", "svg", "code", "pre", "textarea", "noscript"
        );

        while (pos < html.length()) {
            char c = html.charAt(pos);

            if (c == '<') {
                // Save any accumulated text
                if (textStart >= 0 && !inSkipElement) {
                    String text = html.substring(textStart, pos).trim();
                    if (!text.isEmpty() && !text.matches("\\s*")) {
                        segments.add(new TextSegment(textStart, pos, html.substring(textStart, pos)));
                    }
                    textStart = -1;
                }

                inTag = true;

                // Check if this is a skip element start/end tag
                int tagEnd = html.indexOf('>', pos);
                if (tagEnd > pos) {
                    String tagContent = html.substring(pos + 1, tagEnd).trim().toLowerCase();
                    String tagName = tagContent.split("[\\s/>]")[0];

                    if (tagContent.startsWith("/")) {
                        // End tag
                        String endTagName = tagContent.substring(1).split("[\\s>]")[0];
                        if (endTagName.equals(skipElementName)) {
                            inSkipElement = false;
                            skipElementName = null;
                        }
                    } else if (skipElements.contains(tagName)) {
                        // Start of skip element
                        if (!tagContent.endsWith("/")) { // Not self-closing
                            inSkipElement = true;
                            skipElementName = tagName;
                        }
                    }
                }
            } else if (c == '>') {
                inTag = false;
                // Start tracking text after tag closes
                if (!inSkipElement) {
                    textStart = pos + 1;
                }
            } else if (!inTag && textStart < 0 && !inSkipElement) {
                // Start of text content
                textStart = pos;
            }

            pos++;
        }

        // Handle any remaining text at end
        if (textStart >= 0 && !inSkipElement) {
            String text = html.substring(textStart).trim();
            if (!text.isEmpty()) {
                segments.add(new TextSegment(textStart, html.length(), html.substring(textStart)));
            }
        }

        return segments;
    }

    /**
     * Represents a text segment with its position in the HTML.
     */
    private record TextSegment(int start, int end, String text) {}

    /**
     * Translate content (either full document or fragment).
     * For HTML content, uses segment-based translation to preserve structure.
     * @throws LibreTranslateService.TranslationException if translation fails
     */
    private String translateContent(String content, String sourceLanguage, String targetLanguage)
            throws LibreTranslateService.TranslationException {
        // Check if this looks like HTML content (contains tags)
        if (content.contains("<") && content.contains(">")) {
            // Use segment-based translation to preserve HTML structure
            return translateBodyContentSafely(content, sourceLanguage, targetLanguage);
        }

        // For plain text content, translate directly
        return translatePlainText(content, sourceLanguage, targetLanguage);
    }

    /**
     * Translate plain text content (no HTML).
     * @throws LibreTranslateService.TranslationException if translation fails
     */
    public String translatePlainText(String content, String sourceLanguage, String targetLanguage)
            throws LibreTranslateService.TranslationException {
        // Check if content needs chunking
        int maxChars = settingsService.getMaxCharacters();
        if (content.length() > maxChars) {
            return translateLargeContent(content, sourceLanguage, targetLanguage);
        }

        // Try to get from cache first
        if (settingsService.isCacheEnabled()) {
            Optional<String> cachedTranslation = getFromCache(content, sourceLanguage, targetLanguage);
            if (cachedTranslation.isPresent()) {
                log.debug("Cache hit for {} -> {} translation", sourceLanguage, targetLanguage);
                return cachedTranslation.get();
            }
        }

        // Translate via LibreTranslate
        String translatedContent = libreTranslateService.translate(content, sourceLanguage, targetLanguage);

        // Store in cache asynchronously (non-blocking)
        if (settingsService.isCacheEnabled()) {
            cacheAsyncService.saveToCacheAsync(content, translatedContent, sourceLanguage, targetLanguage);
        }

        return translatedContent;
    }

    /**
     * Translate large content by chunking it into smaller pieces.
     */
    private String translateLargeContent(String content, String sourceLanguage, String targetLanguage) {
        int chunkSize = settingsService.getChunkSize();
        List<String> chunks = splitIntoChunks(content, chunkSize);

        log.info("Translating large content ({} chars) in {} chunks", content.length(), chunks.size());

        StringBuilder translatedContent = new StringBuilder();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);

            try {
                // Check cache for each chunk
                Optional<String> cachedChunk = Optional.empty();
                if (settingsService.isCacheEnabled()) {
                    cachedChunk = getFromCache(chunk, sourceLanguage, targetLanguage);
                }

                String translatedChunk;
                if (cachedChunk.isPresent()) {
                    translatedChunk = cachedChunk.get();
                    log.debug("Chunk {}/{} retrieved from cache", i + 1, chunks.size());
                } else {
                    translatedChunk = libreTranslateService.translate(chunk, sourceLanguage, targetLanguage);

                    if (settingsService.isCacheEnabled()) {
                        cacheAsyncService.saveToCacheAsync(chunk, translatedChunk, sourceLanguage, targetLanguage);
                    }
                    log.debug("Chunk {}/{} translated successfully", i + 1, chunks.size());
                }

                translatedContent.append(translatedChunk);

            } catch (LibreTranslateService.TranslationException e) {
                log.warn("Failed to translate chunk {}/{}: {}. Using original.", i + 1, chunks.size(), e.getMessage());
                translatedContent.append(chunk); // Use original chunk on error
            }
        }

        return translatedContent.toString();
    }

    /**
     * Split content into chunks, trying to break at paragraph/sentence boundaries.
     */
    private List<String> splitIntoChunks(String content, int chunkSize) {
        List<String> chunks = new ArrayList<>();

        if (content.length() <= chunkSize) {
            chunks.add(content);
            return chunks;
        }

        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());

            // If not at the end, try to find a good break point
            if (end < content.length()) {
                // Try to break at paragraph
                int paraBreak = content.lastIndexOf("</p>", end);
                if (paraBreak > start && paraBreak > end - (chunkSize / 4)) {
                    end = paraBreak + 4; // Include </p>
                } else {
                    // Try to break at sentence
                    int sentenceBreak = content.lastIndexOf(". ", end);
                    if (sentenceBreak > start && sentenceBreak > end - (chunkSize / 4)) {
                        end = sentenceBreak + 2;
                    } else {
                        // Try to break at space
                        int spaceBreak = content.lastIndexOf(" ", end);
                        if (spaceBreak > start && spaceBreak > end - (chunkSize / 4)) {
                            end = spaceBreak + 1;
                        }
                    }
                }
            }

            chunks.add(content.substring(start, end));
            start = end;
        }

        return chunks;
    }

    /**
     * Get translation from cache.
     * Cache read is synchronous (we need the result), but hit recording is async.
     */
    @Transactional(readOnly = true)
    public Optional<String> getFromCache(String originalContent, String sourceLanguage, String targetLanguage) {
        String contentHash = generateHash(originalContent, sourceLanguage, targetLanguage);

        Optional<TranslationCache> cached = cacheRepository.findValidCacheEntry(
            contentHash, sourceLanguage, targetLanguage
        );

        if (cached.isPresent()) {
            // Record hit asynchronously (non-blocking)
            cacheAsyncService.recordCacheHitAsync(contentHash, sourceLanguage, targetLanguage);
            return Optional.of(cached.get().getTranslatedContent());
        }

        return Optional.empty();
    }

    /**
     * Save translation to cache asynchronously.
     * Delegates to async service to run in separate thread.
     */
    public void saveToCache(String originalContent, String translatedContent, String sourceLanguage, String targetLanguage) {
        cacheAsyncService.saveToCacheAsync(originalContent, translatedContent, sourceLanguage, targetLanguage);
    }

    /**
     * Generate SHA-256 hash for cache key.
     */
    private String generateHash(String content, String sourceLanguage, String targetLanguage) {
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
     * Clean up expired cache entries asynchronously.
     * Should be called periodically (e.g., via scheduled task).
     */
    public void cleanupExpiredCache() {
        cacheAsyncService.cleanupExpiredCacheAsync();
    }

    /**
     * Get cache statistics.
     */
    public CacheStats getCacheStats() {
        long validEntries = cacheRepository.countValidEntries();
        long totalHits = cacheRepository.countTotalHits();
        long charactersSaved = cacheRepository.countCharactersSaved();

        return new CacheStats(validEntries, totalHits, charactersSaved);
    }

    /**
     * Check if translation service is available and configured.
     */
    public boolean isAvailable() {
        return settingsService.isLibreTranslateEnabled() && libreTranslateService.isServiceAvailable();
    }

    /**
     * Cache statistics record.
     */
    public record CacheStats(long validEntries, long totalHits, long charactersSaved) {}
}
