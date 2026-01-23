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
import com.itineraryledger.kabengosafaris.FileSettings.FileSetting;
import com.itineraryledger.kabengosafaris.FileSettings.FileSettingRepository;

/**
 * Initializer for File Upload Settings.
 * Runs at application startup and initializes default file settings in the database.
 *
 * This ensures that the database has the required file upload settings
 * even if they're not explicitly created by the user.
 *
 * Properties can be overridden via application.properties but this initializer loads them into the database.
 *
 * Runs AFTER ImageSettingsInitializer (Order = HIGHEST_PRECEDENCE + 17)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FileSettingsInitializer implements ApplicationRunner, Ordered {

    private final FileSettingRepository fileSettingRepository;

    // =====================================================================
    // Injected values from application.properties - GENERAL UPLOAD Settings
    // =====================================================================

    @Value("${file.upload.enabled:true}")
    private Boolean fileUploadEnabled;

    @Value("${spring.servlet.multipart.max-file-size:10MB}")
    private String multipartMaxFileSize;

    @Value("${spring.servlet.multipart.max-request-size:50MB}")
    private String multipartMaxRequestSize;

    @Value("${file.upload.allowed.extensions:pdf,doc,docx,xls,xlsx,csv,txt,zip}")
    private String fileUploadAllowedExtensions;

    @Value("${file.upload.blocked.extensions:exe,bat,sh,cmd,ps1,jar,msi,dll,com,scr,vbs,js}")
    private String fileUploadBlockedExtensions;

    @Value("${file.upload.sanitize.filename:true}")
    private Boolean fileUploadSanitizeFilename;

    @Value("${file.upload.validate.content.type:true}")
    private Boolean fileUploadValidateContentType;

    // =====================================================================
    // Injected values from application.properties - EMAIL SIGNATURE Settings
    // =====================================================================

    @Value("${email.signature.max.file.size:1048576}")
    private Long emailSignatureMaxFileSize;

    // =====================================================================
    // Injected values from application.properties - EMAIL TEMPLATE Settings
    // =====================================================================

    @Value("${email.template.max.file.size:2097152}")
    private Long emailTemplateMaxFileSize;

    // =====================================================================
    // Injected values from application.properties - PDF TEMPLATE Settings
    // =====================================================================

    @Value("${pdf.template.max.file.size:5242880}")
    private Long pdfTemplateMaxFileSize;

    /**
     * Set order - runs after ImageSettingsInitializer
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 17;
    }

    /**
     * Run initialization at application startup
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        printStartBanner();

        try {
            initializeFileSettings();
            printEndBanner(true);
        } catch (Exception e) {
            log.error("Error during File Settings initialization", e);
            printEndBanner(false);
        }
    }

    private void printStartBanner() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════════╗");
        log.info("║                                                                    ║");
        log.info("║               FILE SETTINGS INITIALIZER - START                    ║");
        log.info("║                                                                    ║");
        log.info("╚════════════════════════════════════════════════════════════════════╝");
        log.info("");
    }

    private void printEndBanner(boolean success) {
        log.info("");
        if (success) {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║          ✓ FILE SETTINGS INITIALIZER - COMPLETED                   ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        } else {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║          ✗ FILE SETTINGS INITIALIZER - FAILED                      ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        }
        log.info("");
    }

    /**
     * Initialize or update file settings in the database
     */
    private void initializeFileSettings() {
        // =====================================================================
        // GENERAL UPLOAD Settings
        // =====================================================================

        createOrUpdateSetting(
            "file.upload.enabled",
            String.valueOf(fileUploadEnabled),
            SettingDataType.BOOLEAN,
            "Enable or disable file uploads globally. When disabled, all file upload requests will be rejected.",
            FileSetting.Category.UPLOAD,
            false
        );

        createOrUpdateSetting(
            "spring.servlet.multipart.max-file-size",
            multipartMaxFileSize,
            SettingDataType.STRING,
            "Maximum file size for uploads (e.g., '10MB', '5MB'). This is the Spring multipart max file size setting.",
            FileSetting.Category.UPLOAD,
            true
        );

        createOrUpdateSetting(
            "spring.servlet.multipart.max-request-size",
            multipartMaxRequestSize,
            SettingDataType.STRING,
            "Maximum request size for multipart uploads (e.g., '50MB', '100MB'). Should be larger than max-file-size to allow multiple files.",
            FileSetting.Category.UPLOAD,
            true
        );

        createOrUpdateSetting(
            "file.upload.allowed.extensions",
            fileUploadAllowedExtensions,
            SettingDataType.STRING,
            "Comma-separated list of allowed file extensions (e.g., 'pdf,doc,docx,xls,xlsx'). Only files with these extensions can be uploaded.",
            FileSetting.Category.UPLOAD,
            false
        );

        createOrUpdateSetting(
            "file.upload.blocked.extensions",
            fileUploadBlockedExtensions,
            SettingDataType.STRING,
            "Comma-separated list of blocked/dangerous file extensions (e.g., 'exe,bat,sh,cmd'). Files with these extensions will always be rejected.",
            FileSetting.Category.UPLOAD,
            false
        );

        createOrUpdateSetting(
            "file.upload.sanitize.filename",
            String.valueOf(fileUploadSanitizeFilename),
            SettingDataType.BOOLEAN,
            "Remove special characters and sanitize filenames during upload to prevent path traversal and other security issues.",
            FileSetting.Category.UPLOAD,
            false
        );

        createOrUpdateSetting(
            "file.upload.validate.content.type",
            String.valueOf(fileUploadValidateContentType),
            SettingDataType.BOOLEAN,
            "Validate that the file's MIME type matches its extension to prevent file spoofing attacks.",
            FileSetting.Category.UPLOAD,
            false
        );

        // =====================================================================
        // EMAIL SIGNATURE Settings
        // =====================================================================

        createOrUpdateSetting(
            "file.email.signature.max.file.size",
            String.valueOf(emailSignatureMaxFileSize),
            SettingDataType.LONG,
            "Maximum file size for email signature uploads in bytes. Default is 1MB (1048576 bytes).",
            FileSetting.Category.EMAIL_SIGNATURE,
            false
        );

        // =====================================================================
        // EMAIL TEMPLATE Settings
        // =====================================================================

        createOrUpdateSetting(
            "file.email.template.max.file.size",
            String.valueOf(emailTemplateMaxFileSize),
            SettingDataType.LONG,
            "Maximum file size for email template uploads in bytes. Default is 2MB (2097152 bytes).",
            FileSetting.Category.EMAIL_TEMPLATE,
            false
        );

        // =====================================================================
        // PDF TEMPLATE Settings
        // =====================================================================

        createOrUpdateSetting(
            "file.pdf.template.max.file.size",
            String.valueOf(pdfTemplateMaxFileSize),
            SettingDataType.LONG,
            "Maximum file size for PDF template uploads in bytes. Default is 5MB (5242880 bytes).",
            FileSetting.Category.PDF_TEMPLATE,
            false
        );

        log.info("All file settings have been initialized");
    }

    /**
     * Create or update a file setting
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
                                        String description, FileSetting.Category category,
                                        Boolean requiresRestart) {
        try {
            if (fileSettingRepository.existsBySettingKey(settingKey)) {
                log.debug("⊘ Setting already exists, skipping: {}", settingKey);
                return;
            }

            FileSetting setting = FileSetting.builder()
                .settingKey(settingKey)
                .settingValue(settingValue)
                .dataType(dataType)
                .description(description)
                .category(category)
                .active(true)
                .isSystemDefault(true)
                .requiresRestart(requiresRestart)
                .build();

            fileSettingRepository.save(setting);
            log.info("File setting initialized: {} = {} (category: {})", settingKey, settingValue, category);

        } catch (Exception e) {
            log.warn("Failed to initialize file setting {}: {}", settingKey, e.getMessage());
        }
    }
}
