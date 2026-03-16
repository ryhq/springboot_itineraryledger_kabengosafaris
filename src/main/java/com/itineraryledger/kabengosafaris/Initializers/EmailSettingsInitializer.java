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
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailSettingRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailSetting;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailSettingsInitializer implements ApplicationRunner, Ordered {

    private final EmailSettingRepository emailSettingRepository;

    @Value("${email.retention.days:365}")
    private Integer retentionDays;

    @Value("${email.retention.auto.cleanup.enabled:true}")
    private Boolean autoCleanupEnabled;

    @Value("${email.retention.trash.days:30}")
    private Integer trashRetentionDays;

    @Value("${email.fetch.auto.enabled:false}")
    private Boolean autoFetchEnabled;

    @Value("${email.fetch.default.interval.minutes:5}")
    private Integer fetchIntervalMinutes;

    @Value("${email.fetch.max.count:50}")
    private Integer maxFetchCount;

    @Value("${email.fetch.initial.sync.days:30}")
    private Integer initialSyncDays;

    @Value("${email.storage.base.path:./data/emails/}")
    private String storageBasePath;

    @Value("${email.storage.max.size:5368709120}")
    private Long maxStorageSize;

    @Value("${email.eml.max.file.size:26214400}")
    private Long maxEmlFileSize;

    @Value("${email.attachment.max.file.size:10485760}")
    private Long maxAttachmentFileSize;

    @Value("${email.sent.capture.enabled:true}")
    private Boolean sentCaptureEnabled;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════════╗");
        log.info("║            EMAIL SETTINGS INITIALIZER - START                      ║");
        log.info("╚════════════════════════════════════════════════════════════════════╝");

        try {
            // Retention settings
            createIfNotExists("email.retention.days", String.valueOf(retentionDays), SettingDataType.INTEGER,
                "Number of days to retain emails (0 = unlimited)", EmailSetting.Category.EMAIL_RETENTION, false);

            createIfNotExists("email.retention.auto.cleanup.enabled", String.valueOf(autoCleanupEnabled), SettingDataType.BOOLEAN,
                "Enable automatic cleanup of old emails", EmailSetting.Category.EMAIL_RETENTION, false);

            createIfNotExists("email.retention.trash.days", String.valueOf(trashRetentionDays), SettingDataType.INTEGER,
                "Number of days to retain emails in trash before permanent deletion", EmailSetting.Category.EMAIL_RETENTION, false);

            // Fetch settings
            createIfNotExists("email.fetch.auto.enabled", String.valueOf(autoFetchEnabled), SettingDataType.BOOLEAN,
                "Enable automatic email fetching for all accounts", EmailSetting.Category.EMAIL_FETCH, false);

            createIfNotExists("email.fetch.default.interval.minutes", String.valueOf(fetchIntervalMinutes), SettingDataType.INTEGER,
                "Default fetch interval in minutes", EmailSetting.Category.EMAIL_FETCH, false);

            createIfNotExists("email.fetch.max.count", String.valueOf(maxFetchCount), SettingDataType.INTEGER,
                "Maximum number of emails to fetch per cycle", EmailSetting.Category.EMAIL_FETCH, false);

            createIfNotExists("email.fetch.initial.sync.days", String.valueOf(initialSyncDays), SettingDataType.INTEGER,
                "Number of days to look back on first sync (0 = fetch all)", EmailSetting.Category.EMAIL_FETCH, false);

            // Storage settings
            createIfNotExists("email.storage.base.path", storageBasePath, SettingDataType.STRING,
                "Base directory path for email file storage", EmailSetting.Category.EMAIL_STORAGE, true);

            createIfNotExists("email.storage.max.size", String.valueOf(maxStorageSize), SettingDataType.LONG,
                "Maximum total storage size for emails in bytes (default 5GB)", EmailSetting.Category.EMAIL_STORAGE, false);

            createIfNotExists("email.eml.max.file.size", String.valueOf(maxEmlFileSize), SettingDataType.LONG,
                "Maximum size of a single .eml file in bytes (default 25MB)", EmailSetting.Category.EMAIL_STORAGE, false);

            createIfNotExists("email.attachment.max.file.size", String.valueOf(maxAttachmentFileSize), SettingDataType.LONG,
                "Maximum size of a single email attachment in bytes (default 10MB)", EmailSetting.Category.EMAIL_STORAGE, false);

            createIfNotExists("email.sent.capture.enabled", String.valueOf(sentCaptureEnabled), SettingDataType.BOOLEAN,
                "Enable capturing all outgoing system emails as .eml files in the SENT folder", EmailSetting.Category.EMAIL_STORAGE, false);

            log.info("║         EMAIL SETTINGS INITIALIZER - COMPLETED                    ║");
        } catch (Exception e) {
            log.error("Error during Email Settings initialization", e);
            log.info("║         EMAIL SETTINGS INITIALIZER - FAILED                        ║");
        }

        log.info("╚════════════════════════════════════════════════════════════════════╝");
        log.info("");
    }

    private void createIfNotExists(String key, String value, SettingDataType dataType,
            String description, EmailSetting.Category category, boolean requiresRestart) {
        if (!emailSettingRepository.existsBySettingKey(key)) {
            EmailSetting setting = EmailSetting.builder()
                .settingKey(key)
                .settingValue(value)
                .dataType(dataType)
                .description(description)
                .active(true)
                .isSystemDefault(true)
                .category(category)
                .requiresRestart(requiresRestart)
                .build();
            emailSettingRepository.save(setting);
            log.info("  Created email setting: {} = {}", key, value);
        } else {
            log.debug("  Email setting already exists: {}", key);
        }
    }
}
