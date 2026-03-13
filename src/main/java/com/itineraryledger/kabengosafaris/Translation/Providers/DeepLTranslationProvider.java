package com.itineraryledger.kabengosafaris.Translation.Providers;

import com.itineraryledger.kabengosafaris.Translation.Account.Entity.TranslationProviderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Slf4j
public class DeepLTranslationProvider implements TranslationProvider {

    private final String baseUrl;
    private final String apiKey;
    private final int timeoutSeconds;

    private volatile Set<String> cachedLanguageCodes = null;
    private volatile long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;

    public DeepLTranslationProvider(String baseUrl, String apiKey, int timeoutSeconds) {
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
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("text", text);
            formData.add("source_lang", sourceLanguage.toUpperCase());
            formData.add("target_lang", targetLanguage.toUpperCase());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("Authorization", "DeepL-Auth-Key " + apiKey);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);
            RestTemplate restTemplate = createRestTemplate();

            String url = baseUrl + "/v2/translate";

            log.debug("DeepL: translating {} -> {}, {} chars", sourceLanguage, targetLanguage, text.length());

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> translations = (List<Map<String, Object>>) response.getBody().get("translations");
                if (translations != null && !translations.isEmpty()) {
                    Object translatedText = translations.get(0).get("text");
                    if (translatedText != null) {
                        return translatedText.toString();
                    }
                }
            }

            throw new TranslationProviderException(
                    "DeepL returned unexpected response: " + response.getStatusCode(),
                    TranslationProviderException.ErrorType.API_ERROR
            );

        } catch (RestClientException e) {
            throw new TranslationProviderException(
                    "Failed to connect to DeepL: " + e.getMessage(),
                    TranslationProviderException.ErrorType.CONNECTION_ERROR, e
            );
        }
    }

    @Override
    public boolean isServiceAvailable() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "DeepL-Auth-Key " + apiKey);

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            RestTemplate restTemplate = createRestTemplate(Math.min(timeoutSeconds, 5));

            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/v2/usage", HttpMethod.GET, requestEntity, Map.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("DeepL service check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getAvailableLanguages() throws TranslationProviderException {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "DeepL-Auth-Key " + apiKey);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            RestTemplate restTemplate = createRestTemplate();

            ResponseEntity<List> response = restTemplate.exchange(
                    baseUrl + "/v2/languages", HttpMethod.GET, requestEntity, List.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> rawLanguages = (List<Map<String, Object>>) response.getBody();
                List<Map<String, String>> result = new ArrayList<>();

                for (Map<String, Object> lang : rawLanguages) {
                    Map<String, String> entry = new HashMap<>();
                    String code = String.valueOf(lang.get("language"));
                    entry.put("code", code.toLowerCase());
                    entry.put("name", String.valueOf(lang.get("name")));
                    result.add(entry);
                }
                return result;
            }

            throw new TranslationProviderException(
                    "Failed to get languages from DeepL",
                    TranslationProviderException.ErrorType.API_ERROR
            );
        } catch (RestClientException e) {
            throw new TranslationProviderException(
                    "Failed to connect to DeepL: " + e.getMessage(),
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
            // DeepL doesn't have a dedicated detect endpoint.
            // Translate without source_lang and extract detected_source_language from response.
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("text", text.length() > 500 ? text.substring(0, 500) : text);
            formData.add("target_lang", "EN");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("Authorization", "DeepL-Auth-Key " + apiKey);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);
            RestTemplate restTemplate = createRestTemplate();

            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/v2/translate", HttpMethod.POST, requestEntity, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> translations = (List<Map<String, Object>>) response.getBody().get("translations");
                if (translations != null && !translations.isEmpty()) {
                    Object detectedLang = translations.get(0).get("detected_source_language");
                    if (detectedLang != null) {
                        return detectedLang.toString().toLowerCase();
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
        return TranslationProviderType.DEEPL;
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
            log.warn("Failed to refresh DeepL language cache: {}", e.getMessage());
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
