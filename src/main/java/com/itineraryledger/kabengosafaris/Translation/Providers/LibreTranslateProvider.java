package com.itineraryledger.kabengosafaris.Translation.Providers;

import com.itineraryledger.kabengosafaris.Translation.Account.Entity.TranslationProviderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Slf4j
public class LibreTranslateProvider implements TranslationProvider {

    private final String baseUrl;
    private final String apiKey;
    private final int timeoutSeconds;

    private volatile Set<String> cachedLanguageCodes = null;
    private volatile long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;

    public LibreTranslateProvider(String baseUrl, String apiKey, int timeoutSeconds) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String translate(String text, String sourceLanguage, String targetLanguage) throws TranslationProviderException {
        if (text == null || text.isBlank()) {
            return text;
        }

        String translateUrl = baseUrl + "/translate";

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("q", text);
            requestBody.put("source", sourceLanguage);
            requestBody.put("target", targetLanguage);
            requestBody.put("format", "text");

            if (apiKey != null && !apiKey.isBlank()) {
                requestBody.put("api_key", apiKey);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            RestTemplate restTemplate = createRestTemplate();

            log.debug("LibreTranslate: translating {} -> {}, {} chars", sourceLanguage, targetLanguage, text.length());

            ResponseEntity<Map> response = restTemplate.exchange(translateUrl, HttpMethod.POST, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object translatedText = response.getBody().get("translatedText");
                if (translatedText != null) {
                    return translatedText.toString();
                }
            }

            throw new TranslationProviderException(
                    "LibreTranslate returned unexpected response: " + response.getStatusCode(),
                    TranslationProviderException.ErrorType.API_ERROR
            );

        } catch (RestClientException e) {
            throw new TranslationProviderException(
                    "Failed to connect to LibreTranslate: " + e.getMessage(),
                    TranslationProviderException.ErrorType.CONNECTION_ERROR, e
            );
        }
    }

    @Override
    public boolean isServiceAvailable() {
        try {
            RestTemplate restTemplate = createRestTemplate(Math.min(timeoutSeconds, 5));
            ResponseEntity<List> response = restTemplate.getForEntity(baseUrl + "/languages", List.class);
            return response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isEmpty();
        } catch (Exception e) {
            log.warn("LibreTranslate service check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getAvailableLanguages() throws TranslationProviderException {
        try {
            RestTemplate restTemplate = createRestTemplate();
            ResponseEntity<List> response = restTemplate.getForEntity(baseUrl + "/languages", List.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (List<Map<String, String>>) response.getBody();
            }

            throw new TranslationProviderException(
                    "Failed to get languages from LibreTranslate",
                    TranslationProviderException.ErrorType.API_ERROR
            );
        } catch (RestClientException e) {
            throw new TranslationProviderException(
                    "Failed to connect to LibreTranslate: " + e.getMessage(),
                    TranslationProviderException.ErrorType.CONNECTION_ERROR, e
            );
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public String detectLanguage(String text) throws TranslationProviderException {
        if (text == null || text.isBlank()) {
            throw new TranslationProviderException("Text is empty", TranslationProviderException.ErrorType.API_ERROR);
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("q", text.length() > 500 ? text.substring(0, 500) : text);

            if (apiKey != null && !apiKey.isBlank()) {
                requestBody.put("api_key", apiKey);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            RestTemplate restTemplate = createRestTemplate();

            ResponseEntity<List> response = restTemplate.exchange(
                    baseUrl + "/detect", HttpMethod.POST, requestEntity, List.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isEmpty()) {
                Map<String, Object> detection = (Map<String, Object>) response.getBody().get(0);
                Object language = detection.get("language");
                if (language != null) {
                    return language.toString();
                }
            }

            throw new TranslationProviderException(
                    "Language detection returned no result",
                    TranslationProviderException.ErrorType.API_ERROR
            );

        } catch (RestClientException e) {
            throw new TranslationProviderException(
                    "Language detection failed: " + e.getMessage(),
                    TranslationProviderException.ErrorType.CONNECTION_ERROR, e
            );
        }
    }

    @Override
    public boolean isLanguageSupported(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (cachedLanguageCodes == null || (now - cacheTimestamp) > CACHE_TTL_MS) {
            refreshLanguageCache();
        }

        if (cachedLanguageCodes == null || cachedLanguageCodes.isEmpty()) {
            return false;
        }

        return cachedLanguageCodes.contains(languageCode.toLowerCase().trim());
    }

    @Override
    public TranslationProviderType getProviderType() {
        return TranslationProviderType.LIBRE_TRANSLATE;
    }

    private synchronized void refreshLanguageCache() {
        try {
            List<Map<String, String>> languages = getAvailableLanguages();
            Set<String> codes = new HashSet<>();
            for (Map<String, String> lang : languages) {
                String code = lang.get("code");
                if (code != null && !code.isBlank()) {
                    codes.add(code.toLowerCase().trim());
                }
            }
            cachedLanguageCodes = codes;
            cacheTimestamp = System.currentTimeMillis();
        } catch (TranslationProviderException e) {
            log.warn("Failed to refresh LibreTranslate language cache: {}", e.getMessage());
            if (cachedLanguageCodes == null) {
                cachedLanguageCodes = Set.of();
            }
        }
    }

    private RestTemplate createRestTemplate() {
        return createRestTemplate(timeoutSeconds);
    }

    private RestTemplate createRestTemplate(int timeout) {
        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeout));
        factory.setReadTimeout(Duration.ofSeconds(timeout));
        restTemplate.setRequestFactory(factory);
        return restTemplate;
    }
}
