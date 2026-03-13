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
public class GoogleCloudTranslationProvider implements TranslationProvider {

    private final String baseUrl;
    private final String apiKey;
    private final int timeoutSeconds;

    private volatile Set<String> cachedLanguageCodes = null;
    private volatile long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;

    public GoogleCloudTranslationProvider(String baseUrl, String apiKey, int timeoutSeconds) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String translate(String text, String sourceLanguage, String targetLanguage) throws TranslationProviderException {
        if (text == null || text.isBlank()) {
            return text;
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("q", text);
            requestBody.put("source", sourceLanguage);
            requestBody.put("target", targetLanguage);
            requestBody.put("format", "text");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            RestTemplate restTemplate = createRestTemplate();

            String url = baseUrl + "?key=" + apiKey;

            log.debug("Google Cloud: translating {} -> {}, {} chars", sourceLanguage, targetLanguage, text.length());

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                if (data != null) {
                    List<Map<String, Object>> translations = (List<Map<String, Object>>) data.get("translations");
                    if (translations != null && !translations.isEmpty()) {
                        Object translatedText = translations.get(0).get("translatedText");
                        if (translatedText != null) {
                            return translatedText.toString();
                        }
                    }
                }
            }

            throw new TranslationProviderException(
                    "Google Cloud Translation returned unexpected response: " + response.getStatusCode(),
                    TranslationProviderException.ErrorType.API_ERROR
            );

        } catch (RestClientException e) {
            throw new TranslationProviderException(
                    "Failed to connect to Google Cloud Translation: " + e.getMessage(),
                    TranslationProviderException.ErrorType.CONNECTION_ERROR, e
            );
        }
    }

    @Override
    public boolean isServiceAvailable() {
        try {
            getAvailableLanguages();
            return true;
        } catch (Exception e) {
            log.warn("Google Cloud Translation service check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getAvailableLanguages() throws TranslationProviderException {
        try {
            RestTemplate restTemplate = createRestTemplate();
            String url = baseUrl + "/languages?key=" + apiKey + "&target=en";

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                if (data != null) {
                    List<Map<String, Object>> languages = (List<Map<String, Object>>) data.get("languages");
                    if (languages != null) {
                        List<Map<String, String>> result = new ArrayList<>();
                        for (Map<String, Object> lang : languages) {
                            Map<String, String> entry = new HashMap<>();
                            entry.put("code", String.valueOf(lang.get("language")));
                            entry.put("name", String.valueOf(lang.getOrDefault("name", lang.get("language"))));
                            result.add(entry);
                        }
                        return result;
                    }
                }
            }

            throw new TranslationProviderException(
                    "Failed to get languages from Google Cloud Translation",
                    TranslationProviderException.ErrorType.API_ERROR
            );
        } catch (RestClientException e) {
            throw new TranslationProviderException(
                    "Failed to connect to Google Cloud Translation: " + e.getMessage(),
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

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            RestTemplate restTemplate = createRestTemplate();

            String url = baseUrl + "/detect?key=" + apiKey;

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                if (data != null) {
                    List<List<Map<String, Object>>> detections = (List<List<Map<String, Object>>>) data.get("detections");
                    if (detections != null && !detections.isEmpty() && !detections.get(0).isEmpty()) {
                        Object language = detections.get(0).get(0).get("language");
                        if (language != null) {
                            return language.toString();
                        }
                    }
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
        return TranslationProviderType.GOOGLE_CLOUD;
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
            log.warn("Failed to refresh Google Cloud language cache: {}", e.getMessage());
            if (cachedLanguageCodes == null) {
                cachedLanguageCodes = Set.of();
            }
        }
    }

    private RestTemplate createRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        restTemplate.setRequestFactory(factory);
        return restTemplate;
    }
}
