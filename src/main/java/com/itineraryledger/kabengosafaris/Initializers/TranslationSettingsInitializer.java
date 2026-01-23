package com.itineraryledger.kabengosafaris.Initializers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.GlobalEnums.SettingDataType;
import com.itineraryledger.kabengosafaris.Translation.Settings.TranslationSetting;
import com.itineraryledger.kabengosafaris.Translation.Settings.TranslationSettingRepository;

/**
 * Initializer for Translation Settings.
 * Runs at application startup and initializes default translation settings in the database.
 *
 * This ensures that the database has the required translation/LibreTranslate settings
 * even if they're not explicitly created by the user.
 *
 * Properties can be overridden via application.properties but this initializer loads them into the database.
 *
 * Runs AFTER PdfDocumentInitializer (Order = HIGHEST_PRECEDENCE + 15)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TranslationSettingsInitializer implements ApplicationRunner, Ordered {

    private final TranslationSettingRepository translationSettingRepository;

    // =====================================================================
    // Injected values from application.properties - CONNECTION Settings
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
    // Injected values from application.properties - LANGUAGES Settings
    // =====================================================================

    @Value("${translation.default.source.language:en}")
    private String defaultSourceLanguage;

    @Value("${translation.default.target.language:fr}")
    private String defaultTargetLanguage;

    @Value("${translation.supported.languages:en,fr,de,es,it,pt,sw}")
    private String supportedLanguages;

    // =====================================================================
    // Injected values from application.properties - CACHING Settings
    // =====================================================================

    @Value("${translation.cache.enabled:true}")
    private Boolean cacheEnabled;

    @Value("${translation.cache.ttl.hours:168}")
    private Integer cacheTtlHours;

    // =====================================================================
    // Injected values from application.properties - LIMITS Settings
    // =====================================================================

    @Value("${translation.max.characters:10000}")
    private Integer maxCharacters;

    @Value("${translation.chunk.size:5000}")
    private Integer chunkSize;

    /**
     * Set order - runs after PdfDocumentInitializer
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 15;
    }

    /**
     * Run initialization at application startup
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        printStartBanner();

        try {
            initializeTranslationSettings();
            printEndBanner(true);
        } catch (Exception e) {
            log.error("Error during Translation Settings initialization", e);
            printEndBanner(false);
        }
    }

    private void printStartBanner() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════════╗");
        log.info("║                                                                    ║");
        log.info("║            TRANSLATION SETTINGS INITIALIZER - START                ║");
        log.info("║                                                                    ║");
        log.info("╚════════════════════════════════════════════════════════════════════╝");
        log.info("");
    }

    private void printEndBanner(boolean success) {
        log.info("");
        if (success) {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║       ✓ TRANSLATION SETTINGS INITIALIZER - COMPLETED               ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        } else {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║       ✗ TRANSLATION SETTINGS INITIALIZER - FAILED                  ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        }
        log.info("");
    }

    /**
     * Initialize or update translation settings in the database
     */
    private void initializeTranslationSettings() {
        // =====================================================================
        // CONNECTION Settings
        // =====================================================================

        createOrUpdateSetting(
            "libretranslate.enabled",
            String.valueOf(libretranslateEnabled),
            SettingDataType.BOOLEAN,
            "Enable or disable LibreTranslate integration. When disabled, translation requests will return the original text.",
            TranslationSetting.Category.CONNECTION,
            false
        );

        createOrUpdateSetting(
            "libretranslate.base.url",
            libretranslateBaseUrl,
            SettingDataType.STRING,
            "Base URL for LibreTranslate API (e.g., http://localhost:5000 for Docker, or https://libretranslate.com for cloud)",
            TranslationSetting.Category.CONNECTION,
            false
        );

        createOrUpdateSetting(
            "libretranslate.timeout.seconds",
            String.valueOf(libretranslateTimeoutSeconds),
            SettingDataType.INTEGER,
            "HTTP request timeout in seconds for LibreTranslate API calls",
            TranslationSetting.Category.CONNECTION,
            false
        );

        createOrUpdateSetting(
            "libretranslate.api.key",
            libretranslateApiKey != null ? libretranslateApiKey : "",
            SettingDataType.STRING,
            "API key for authenticated LibreTranslate instances (leave empty for open instances)",
            TranslationSetting.Category.CONNECTION,
            false
        );

        // =====================================================================
        // LANGUAGES Settings
        // =====================================================================

        createOrUpdateSetting(
            "translation.default.source.language",
            defaultSourceLanguage,
            SettingDataType.STRING,
            "Default source language code for translations (e.g., 'en' for English)",
            TranslationSetting.Category.LANGUAGES,
            false
        );

        createOrUpdateSetting(
            "translation.default.target.language",
            defaultTargetLanguage,
            SettingDataType.STRING,
            "Default target language code when no specific language is requested (e.g., 'fr' for French)",
            TranslationSetting.Category.LANGUAGES,
            false
        );

        createOrUpdateSetting(
            "translation.supported.languages",
            supportedLanguages,
            SettingDataType.STRING,
            "Comma-separated list of supported language codes (e.g., 'en,fr,de,es,it,pt,sw'). Only languages in this list can be used for translation.",
            TranslationSetting.Category.LANGUAGES,
            false
        );

        // =====================================================================
        // CACHING Settings
        // =====================================================================

        createOrUpdateSetting(
            "translation.cache.enabled",
            String.valueOf(cacheEnabled),
            SettingDataType.BOOLEAN,
            "Enable or disable translation caching. When enabled, translated content is cached to reduce API calls.",
            TranslationSetting.Category.CACHING,
            false
        );

        createOrUpdateSetting(
            "translation.cache.ttl.hours",
            String.valueOf(cacheTtlHours),
            SettingDataType.INTEGER,
            "Time-to-live for cached translations in hours. Default is 168 hours (7 days).",
            TranslationSetting.Category.CACHING,
            false
        );

        // =====================================================================
        // LIMITS Settings
        // =====================================================================

        createOrUpdateSetting(
            "translation.max.characters",
            String.valueOf(maxCharacters),
            SettingDataType.INTEGER,
            "Maximum number of characters allowed per translation request. Larger texts will be chunked.",
            TranslationSetting.Category.LIMITS,
            false
        );

        createOrUpdateSetting(
            "translation.chunk.size",
            String.valueOf(chunkSize),
            SettingDataType.INTEGER,
            "Size of text chunks when splitting large texts for translation. Should be less than max characters.",
            TranslationSetting.Category.LIMITS,
            false
        );

        log.info("All translation settings have been initialized");
    }

    /**
     * Create or update a translation setting
     * If the setting already exists (by key), it will not be overwritten
     * This preserves any user modifications to settings
     *
     * @param settingKey the setting key
     * @param settingValue the setting value
     * @param dataType the data type
     * @param description the description
     * @param category the category
     * @param requiresRestart whether changing this setting requires restart
     */
    private void createOrUpdateSetting(String settingKey, String settingValue,
                                        SettingDataType dataType,
                                        String description, TranslationSetting.Category category,
                                        Boolean requiresRestart) {
        try {
            if (translationSettingRepository.existsBySettingKey(settingKey)) {
                log.debug("⊘ Setting already exists, skipping: {}", settingKey);
                return;
            }

            TranslationSetting setting = TranslationSetting.builder()
                .settingKey(settingKey)
                .settingValue(settingValue)
                .dataType(dataType)
                .description(description)
                .category(category)
                .active(true)
                .isSystemDefault(true)
                .requiresRestart(requiresRestart)
                .build();

            translationSettingRepository.save(setting);
            log.info("Translation setting initialized: {} = {} (category: {})", settingKey, settingValue, category);

        } catch (Exception e) {
            log.warn("Failed to initialize translation setting {}: {}", settingKey, e.getMessage());
        }
    }
}
