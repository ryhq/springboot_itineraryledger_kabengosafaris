package com.itineraryledger.kabengosafaris.Translation.Services;

import com.itineraryledger.kabengosafaris.Translation.Settings.TranslationSettingGetterServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for communicating with LibreTranslate API.
 * Handles the low-level HTTP communication with LibreTranslate.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LibreTranslateService {

    private final TranslationSettingGetterServices settingsService;

    /**
     * Cached set of available language codes from LibreTranslate.
     * Refreshed periodically to avoid repeated API calls.
     */
    private volatile java.util.Set<String> cachedLanguageCodes = null;
    private volatile long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

    /**
     * Translate text using LibreTranslate API
     *
     * @param text the text to translate
     * @param sourceLanguage source language code (e.g., "en")
     * @param targetLanguage target language code (e.g., "fr")
     * @return translated text
     * @throws TranslationException if translation fails
     */
    public String translate(String text, String sourceLanguage, String targetLanguage) throws TranslationException {
        if (text == null || text.isBlank()) {
            return text;
        }

        // Check if LibreTranslate is enabled
        if (!settingsService.isLibreTranslateEnabled()) {
            log.warn("LibreTranslate is disabled. Returning original text.");
            throw new TranslationException("LibreTranslate is disabled", TranslationException.ErrorType.SERVICE_DISABLED);
        }

        String baseUrl = settingsService.getLibreTranslateBaseUrl();
        String apiKey = settingsService.getLibreTranslateApiKey();
        int timeoutSeconds = settingsService.getLibreTranslateTimeoutSeconds();

        String translateUrl = baseUrl + "/translate";

        try {
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("q", text);
            requestBody.put("source", sourceLanguage);
            requestBody.put("target", targetLanguage);
            requestBody.put("format", "text"); // Use text mode - HTML tags are preserved by TranslationService

            // Add API key if configured
            if (apiKey != null && !apiKey.isBlank()) {
                requestBody.put("api_key", apiKey);
            }

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // Create RestTemplate with timeout
            RestTemplate restTemplate = createRestTemplate(timeoutSeconds);

            log.debug("Sending translation request to LibreTranslate: {} -> {}, {} chars",
                sourceLanguage, targetLanguage, text.length());

            // Make the request
            ResponseEntity<Map> response = restTemplate.exchange(
                translateUrl,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object translatedText = response.getBody().get("translatedText");
                if (translatedText != null) {
                    String result = translatedText.toString();
                    log.debug("Translation successful: {} chars -> {} chars",
                        text.length(), result.length());
                    return result;
                }
            }

            log.error("LibreTranslate returned unexpected response: {}", response.getStatusCode());
            throw new TranslationException(
                "LibreTranslate returned unexpected response: " + response.getStatusCode(),
                TranslationException.ErrorType.API_ERROR
            );

        } catch (RestClientException e) {
            log.error("Failed to connect to LibreTranslate at {}: {}", baseUrl, e.getMessage());
            throw new TranslationException(
                "Failed to connect to LibreTranslate: " + e.getMessage(),
                TranslationException.ErrorType.CONNECTION_ERROR,
                e
            );
        }
    }

    /**
     * Check if LibreTranslate service is available
     *
     * @return true if service is reachable and responding
     */
    public boolean isServiceAvailable() {
        if (!settingsService.isLibreTranslateEnabled()) {
            return false;
        }

        String baseUrl = settingsService.getLibreTranslateBaseUrl();
        int timeoutSeconds = Math.min(settingsService.getLibreTranslateTimeoutSeconds(), 5); // Quick check

        try {
            RestTemplate restTemplate = createRestTemplate(timeoutSeconds);
            // /languages returns a List (array), not a Map
            ResponseEntity<List> response = restTemplate.getForEntity(
                baseUrl + "/languages",
                List.class
            );
            return response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isEmpty();
        } catch (Exception e) {
            log.warn("LibreTranslate service check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get available languages from LibreTranslate
     *
     * @return list of available languages with their codes and names
     * @throws TranslationException if request fails
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getAvailableLanguages() throws TranslationException {
        if (!settingsService.isLibreTranslateEnabled()) {
            throw new TranslationException("LibreTranslate is disabled", TranslationException.ErrorType.SERVICE_DISABLED);
        }

        String baseUrl = settingsService.getLibreTranslateBaseUrl();
        int timeoutSeconds = settingsService.getLibreTranslateTimeoutSeconds();

        try {
            RestTemplate restTemplate = createRestTemplate(timeoutSeconds);
            ResponseEntity<List> response = restTemplate.getForEntity(
                baseUrl + "/languages",
                List.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (List<Map<String, String>>) response.getBody();
            }

            throw new TranslationException(
                "Failed to get languages from LibreTranslate",
                TranslationException.ErrorType.API_ERROR
            );

        } catch (RestClientException e) {
            throw new TranslationException(
                "Failed to connect to LibreTranslate: " + e.getMessage(),
                TranslationException.ErrorType.CONNECTION_ERROR,
                e
            );
        }
    }

    /**
     * Check if a specific language is supported by LibreTranslate.
     * Uses caching to avoid repeated API calls.
     *
     * @param languageCode the language code to check (e.g., "fr", "de", "pt")
     * @return true if the language is supported by LibreTranslate
     */
    public boolean isLanguageSupportedByLibreTranslate(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return false;
        }

        if (!settingsService.isLibreTranslateEnabled()) {
            return false;
        }

        // Check if cache is valid
        long now = System.currentTimeMillis();
        if (cachedLanguageCodes == null || (now - cacheTimestamp) > CACHE_TTL_MS) {
            refreshLanguageCache();
        }

        // If cache refresh failed, return false to skip translation
        if (cachedLanguageCodes == null || cachedLanguageCodes.isEmpty()) {
            log.warn("Language cache is empty, cannot verify language support for: {}", languageCode);
            return false;
        }

        return cachedLanguageCodes.contains(languageCode.toLowerCase().trim());
    }

    /**
     * Refresh the cached language codes from LibreTranslate.
     */
    private synchronized void refreshLanguageCache() {
        try {
            List<Map<String, String>> languages = getAvailableLanguages();
            java.util.Set<String> codes = new java.util.HashSet<>();

            for (Map<String, String> lang : languages) {
                String code = lang.get("code");
                if (code != null && !code.isBlank()) {
                    codes.add(code.toLowerCase().trim());
                }
            }

            cachedLanguageCodes = codes;
            cacheTimestamp = System.currentTimeMillis();
            log.debug("Refreshed LibreTranslate language cache with {} languages", codes.size());

        } catch (TranslationException e) {
            log.warn("Failed to refresh language cache from LibreTranslate: {}", e.getMessage());
            // Keep existing cache if available, or set to empty
            if (cachedLanguageCodes == null) {
                cachedLanguageCodes = java.util.Set.of();
            }
        }
    }

    /**
     * Detect the language of the given text
     *
     * @param text the text to analyze
     * @return detected language code
     * @throws TranslationException if detection fails
     */
    @SuppressWarnings("unchecked")
    public String detectLanguage(String text) throws TranslationException {
        if (!settingsService.isLibreTranslateEnabled()) {
            throw new TranslationException("LibreTranslate is disabled", TranslationException.ErrorType.SERVICE_DISABLED);
        }

        if (text == null || text.isBlank()) {
            return settingsService.getDefaultSourceLanguage();
        }

        String baseUrl = settingsService.getLibreTranslateBaseUrl();
        String apiKey = settingsService.getLibreTranslateApiKey();
        int timeoutSeconds = settingsService.getLibreTranslateTimeoutSeconds();

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("q", text.length() > 500 ? text.substring(0, 500) : text); // Limit for detection

            if (apiKey != null && !apiKey.isBlank()) {
                requestBody.put("api_key", apiKey);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            RestTemplate restTemplate = createRestTemplate(timeoutSeconds);
            ResponseEntity<List> response = restTemplate.exchange(
                baseUrl + "/detect",
                HttpMethod.POST,
                requestEntity,
                List.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isEmpty()) {
                Map<String, Object> detection = (Map<String, Object>) response.getBody().get(0);
                Object language = detection.get("language");
                if (language != null) {
                    return language.toString();
                }
            }

            return settingsService.getDefaultSourceLanguage();

        } catch (RestClientException e) {
            log.warn("Language detection failed, using default: {}", e.getMessage());
            return settingsService.getDefaultSourceLanguage();
        }
    }

    /**
     * Create RestTemplate with configured timeout
     */
    private RestTemplate createRestTemplate(int timeoutSeconds) {
        RestTemplate restTemplate = new RestTemplate();

        // Configure timeouts using SimpleClientHttpRequestFactory
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
            new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        restTemplate.setRequestFactory(factory);

        return restTemplate;
    }

    /**
     * Custom exception for translation errors
     */
    public static class TranslationException extends Exception {
        private final ErrorType errorType;

        public enum ErrorType {
            SERVICE_DISABLED,
            CONNECTION_ERROR,
            API_ERROR,
            INVALID_LANGUAGE,
            TEXT_TOO_LONG,
            RATE_LIMITED
        }

        public TranslationException(String message, ErrorType errorType) {
            super(message);
            this.errorType = errorType;
        }

        public TranslationException(String message, ErrorType errorType, Throwable cause) {
            super(message, cause);
            this.errorType = errorType;
        }

        public ErrorType getErrorType() {
            return errorType;
        }
    }
}
