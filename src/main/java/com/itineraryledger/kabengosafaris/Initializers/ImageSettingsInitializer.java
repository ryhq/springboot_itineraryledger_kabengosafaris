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
import com.itineraryledger.kabengosafaris.ImageSettings.ImageSetting;
import com.itineraryledger.kabengosafaris.ImageSettings.ImageSettingRepository;

/**
 * Initializer for Image Settings.
 * Runs at application startup and initializes default image settings in the database.
 *
 * This ensures that the database has the required image upload settings
 * even if they're not explicitly created by the user.
 *
 * Properties can be overridden via application.properties but this initializer loads them into the database.
 *
 * Runs AFTER TranslationSettingsInitializer (Order = HIGHEST_PRECEDENCE + 16)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ImageSettingsInitializer implements ApplicationRunner, Ordered {

    private final ImageSettingRepository imageSettingRepository;

    // =====================================================================
    // Injected values from application.properties - UPLOAD Settings
    // =====================================================================

    @Value("${image.upload.enabled:true}")
    private Boolean imageUploadEnabled;

    @Value("${image.upload.max.file.size:5242880}")
    private Long imageUploadMaxFileSize;

    @Value("${image.upload.allowed.formats:jpg,jpeg,png,webp,gif}")
    private String imageUploadAllowedFormats;

    /**
     * Set order - runs after TranslationSettingsInitializer
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 16;
    }

    /**
     * Run initialization at application startup
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        printStartBanner();

        try {
            initializeImageSettings();
            printEndBanner(true);
        } catch (Exception e) {
            log.error("Error during Image Settings initialization", e);
            printEndBanner(false);
        }
    }

    private void printStartBanner() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════════╗");
        log.info("║                                                                    ║");
        log.info("║              IMAGE SETTINGS INITIALIZER - START                    ║");
        log.info("║                                                                    ║");
        log.info("╚════════════════════════════════════════════════════════════════════╝");
        log.info("");
    }

    private void printEndBanner(boolean success) {
        log.info("");
        if (success) {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║         ✓ IMAGE SETTINGS INITIALIZER - COMPLETED                   ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        } else {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║         ✗ IMAGE SETTINGS INITIALIZER - FAILED                      ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        }
        log.info("");
    }

    /**
     * Initialize or update image settings in the database
     */
    private void initializeImageSettings() {
        // =====================================================================
        // UPLOAD Settings
        // =====================================================================

        createOrUpdateSetting(
            "image.upload.enabled",
            String.valueOf(imageUploadEnabled),
            SettingDataType.BOOLEAN,
            "Enable or disable image uploads. When disabled, image upload requests will be rejected.",
            ImageSetting.Category.UPLOAD,
            false
        );

        createOrUpdateSetting(
            "image.upload.max.file.size",
            String.valueOf(imageUploadMaxFileSize),
            SettingDataType.LONG,
            "Maximum file size for image uploads in bytes. Default is 5MB (5242880 bytes).",
            ImageSetting.Category.UPLOAD,
            false
        );

        createOrUpdateSetting(
            "image.upload.allowed.formats",
            imageUploadAllowedFormats,
            SettingDataType.STRING,
            "Comma-separated list of allowed image formats (e.g., 'jpg,jpeg,png,webp,gif'). Only images with these extensions can be uploaded.",
            ImageSetting.Category.UPLOAD,
            false
        );

        log.info("All image settings have been initialized");
    }

    /**
     * Create or update an image setting
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
                                        String description, ImageSetting.Category category,
                                        Boolean requiresRestart) {
        try {
            if (imageSettingRepository.existsBySettingKey(settingKey)) {
                log.debug("⊘ Setting already exists, skipping: {}", settingKey);
                return;
            }

            ImageSetting setting = ImageSetting.builder()
                .settingKey(settingKey)
                .settingValue(settingValue)
                .dataType(dataType)
                .description(description)
                .category(category)
                .active(true)
                .isSystemDefault(true)
                .requiresRestart(requiresRestart)
                .build();

            imageSettingRepository.save(setting);
            log.info("Image setting initialized: {} = {} (category: {})", settingKey, settingValue, category);

        } catch (Exception e) {
            log.warn("Failed to initialize image setting {}: {}", settingKey, e.getMessage());
        }
    }
}
