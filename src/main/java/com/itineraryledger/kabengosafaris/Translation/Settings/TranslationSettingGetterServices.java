package com.itineraryledger.kabengosafaris.Translation.Settings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Service for reading translation settings with fallback to application.properties values.
 * This service provides getter methods that first check the database, then fall back to
 * default values from application.properties if the database value is not available or inactive.
 */
@Service
public class TranslationSettingGetterServices {

    // =====================================================================
    // Fallback values from application.properties - CONNECTION Settings
    // =====================================================================

    @Value("${translation.libretranslate.enabled:false}")
    private Boolean libretranslateEnabled;

    @Value("${translation.libretranslate.base.url:http://localhost:5000}")
    private String libretranslateBaseUrl;

    @Value("${translation.libretranslate.timeout.seconds:30}")
    private Integer libretranslateTimeoutSeconds;

    @Value("${translation.libretranslate.api.key:}")
    private String libretranslateApiKey;

    // =====================================================================
    // Fallback values from application.properties - LANGUAGES Settings
    // =====================================================================

    @Value("${translation.default.source.language:en}")
    private String defaultSourceLanguage;

    @Value("${translation.default.target.language:fr}")
    private String defaultTargetLanguage;

    @Value("${translation.supported.languages:en,fr,de,es,it,pt,sw}")
    private String supportedLanguages;

    // =====================================================================
    // Fallback values from application.properties - CACHING Settings
    // =====================================================================

    @Value("${translation.cache.enabled:true}")
    private Boolean cacheEnabled;

    @Value("${translation.cache.ttl.hours:168}")
    private Integer cacheTtlHours;

    // =====================================================================
    // Fallback values from application.properties - LIMITS Settings
    // =====================================================================

    @Value("${translation.max.characters:10000}")
    private Integer maxCharacters;

    @Value("${translation.chunk.size:5000}")
    private Integer chunkSize;

    @Autowired
    private TranslationSettingRepository translationSettingRepository;

    // =====================================================================
    // CONNECTION Settings Getters
    // =====================================================================

    /**
     * Check if LibreTranslate is enabled
     * @return true if LibreTranslate is enabled
     */
    public Boolean isLibreTranslateEnabled() {
        TranslationSetting setting = translationSettingRepository
                .findBySettingKey("libretranslate.enabled").orElse(null);
        if (setting == null || !setting.getActive()) {
            return libretranslateEnabled;
        }
        try {
            return Boolean.parseBoolean(setting.getSettingValue());
        } catch (Exception e) {
            return libretranslateEnabled;
        }
    }

    /**
     * Get LibreTranslate base URL
     * @return the LibreTranslate base URL
     */
    public String getLibreTranslateBaseUrl() {
        TranslationSetting setting = translationSettingRepository
                .findBySettingKey("libretranslate.base.url").orElse(null);
        if (setting == null || !setting.getActive()) {
            return libretranslateBaseUrl;
        }
        String value = setting.getSettingValue();
        return (value != null && !value.isBlank()) ? value : libretranslateBaseUrl;
    }

    /**
     * Get LibreTranslate request timeout in seconds
     * @return timeout in seconds
     */
    public Integer getLibreTranslateTimeoutSeconds() {
        TranslationSetting setting = translationSettingRepository
                .findBySettingKey("libretranslate.timeout.seconds").orElse(null);
        if (setting == null || !setting.getActive()) {
            return libretranslateTimeoutSeconds;
        }
        try {
            return Integer.parseInt(setting.getSettingValue());
        } catch (NumberFormatException e) {
            return libretranslateTimeoutSeconds;
        }
    }

    /**
     * Get LibreTranslate API key (optional, for authenticated instances)
     * @return the API key or empty string
     */
    public String getLibreTranslateApiKey() {
        TranslationSetting setting = translationSettingRepository
                .findBySettingKey("libretranslate.api.key").orElse(null);
        if (setting == null || !setting.getActive()) {
            return libretranslateApiKey;
        }
        return setting.getSettingValue() != null ? setting.getSettingValue() : "";
    }

    // =====================================================================
    // LANGUAGES Settings Getters
    // =====================================================================

    /**
     * Get default source language code
     * @return source language code (e.g., "en")
     */
    public String getDefaultSourceLanguage() {
        TranslationSetting setting = translationSettingRepository
                .findBySettingKey("translation.default.source.language").orElse(null);
        if (setting == null || !setting.getActive()) {
            return defaultSourceLanguage;
        }
        String value = setting.getSettingValue();
        return (value != null && !value.isBlank()) ? value : defaultSourceLanguage;
    }

    /**
     * Get default target language code
     * @return target language code (e.g., "fr")
     */
    public String getDefaultTargetLanguage() {
        TranslationSetting setting = translationSettingRepository
                .findBySettingKey("translation.default.target.language").orElse(null);
        if (setting == null || !setting.getActive()) {
            return defaultTargetLanguage;
        }
        String value = setting.getSettingValue();
        return (value != null && !value.isBlank()) ? value : defaultTargetLanguage;
    }

    /**
     * Get supported languages as comma-separated string
     * @return comma-separated language codes
     */
    public String getSupportedLanguagesString() {
        TranslationSetting setting = translationSettingRepository
                .findBySettingKey("translation.supported.languages").orElse(null);
        if (setting == null || !setting.getActive()) {
            return supportedLanguages;
        }
        String value = setting.getSettingValue();
        return (value != null && !value.isBlank()) ? value : supportedLanguages;
    }

    /**
     * Get supported languages as list
     * @return list of supported language codes
     */
    public List<String> getSupportedLanguages() {
        String languages = getSupportedLanguagesString();
        return Arrays.asList(languages.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Check if a language is supported
     * @param languageCode the language code to check
     * @return true if supported
     */
    public boolean isLanguageSupported(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return false;
        }
        return getSupportedLanguages().contains(languageCode.toLowerCase().trim());
    }

    // =====================================================================
    // CACHING Settings Getters
    // =====================================================================

    /**
     * Check if translation caching is enabled
     * @return true if caching is enabled
     */
    public Boolean isCacheEnabled() {
        TranslationSetting setting = translationSettingRepository
                .findBySettingKey("translation.cache.enabled").orElse(null);
        if (setting == null || !setting.getActive()) {
            return cacheEnabled;
        }
        try {
            return Boolean.parseBoolean(setting.getSettingValue());
        } catch (Exception e) {
            return cacheEnabled;
        }
    }

    /**
     * Get cache time-to-live in hours
     * @return TTL in hours
     */
    public Integer getCacheTtlHours() {
        TranslationSetting setting = translationSettingRepository
                .findBySettingKey("translation.cache.ttl.hours").orElse(null);
        if (setting == null || !setting.getActive()) {
            return cacheTtlHours;
        }
        try {
            return Integer.parseInt(setting.getSettingValue());
        } catch (NumberFormatException e) {
            return cacheTtlHours;
        }
    }

    // =====================================================================
    // LIMITS Settings Getters
    // =====================================================================

    /**
     * Get maximum characters allowed per translation request
     * @return max characters
     */
    public Integer getMaxCharacters() {
        TranslationSetting setting = translationSettingRepository
                .findBySettingKey("translation.max.characters").orElse(null);
        if (setting == null || !setting.getActive()) {
            return maxCharacters;
        }
        try {
            return Integer.parseInt(setting.getSettingValue());
        } catch (NumberFormatException e) {
            return maxCharacters;
        }
    }

    /**
     * Get chunk size for splitting large texts
     * @return chunk size in characters
     */
    public Integer getChunkSize() {
        TranslationSetting setting = translationSettingRepository
                .findBySettingKey("translation.chunk.size").orElse(null);
        if (setting == null || !setting.getActive()) {
            return chunkSize;
        }
        try {
            return Integer.parseInt(setting.getSettingValue());
        } catch (NumberFormatException e) {
            return chunkSize;
        }
    }
}
