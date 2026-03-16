package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailSettingRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailSetting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Typed getters for email settings with @Value fallback.
 * Follows the FileSettingGetterServices pattern: DB first, property fallback.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailSettingGetterServices {

    private final EmailSettingRepository emailSettingRepository;

    // ---- Retention fallbacks ----
    @Value("${email.retention.days:365}")
    private Integer defaultRetentionDays;

    @Value("${email.retention.auto.cleanup.enabled:true}")
    private Boolean defaultAutoCleanupEnabled;

    @Value("${email.retention.trash.days:30}")
    private Integer defaultTrashRetentionDays;

    // ---- Fetch fallbacks ----
    @Value("${email.fetch.auto.enabled:false}")
    private Boolean defaultAutoFetchEnabled;

    @Value("${email.fetch.default.interval.minutes:5}")
    private Integer defaultFetchIntervalMinutes;

    @Value("${email.fetch.max.count:50}")
    private Integer defaultMaxFetchCount;

    @Value("${email.fetch.initial.sync.days:30}")
    private Integer defaultInitialSyncDays;

    // ---- Storage fallbacks ----
    @Value("${email.storage.base.path:./data/emails/}")
    private String defaultStorageBasePath;

    @Value("${email.storage.max.size:5368709120}")
    private Long defaultMaxStorageSize;

    @Value("${email.eml.max.file.size:26214400}")
    private Long defaultMaxEmlFileSize;

    @Value("${email.attachment.max.file.size:10485760}")
    private Long defaultMaxAttachmentFileSize;

    @Value("${email.sent.capture.enabled:true}")
    private Boolean defaultSentCaptureEnabled;

    // =====================================================================
    // Retention Getters
    // =====================================================================

    public Integer getRetentionDays() {
        return getIntegerSetting("email.retention.days", defaultRetentionDays);
    }

    public Boolean isAutoCleanupEnabled() {
        return getBooleanSetting("email.retention.auto.cleanup.enabled", defaultAutoCleanupEnabled);
    }

    public Integer getTrashRetentionDays() {
        return getIntegerSetting("email.retention.trash.days", defaultTrashRetentionDays);
    }

    // =====================================================================
    // Fetch Getters
    // =====================================================================

    public Boolean isAutoFetchEnabled() {
        return getBooleanSetting("email.fetch.auto.enabled", defaultAutoFetchEnabled);
    }

    public Integer getDefaultFetchIntervalMinutes() {
        return getIntegerSetting("email.fetch.default.interval.minutes", defaultFetchIntervalMinutes);
    }

    public Integer getMaxFetchCount() {
        return getIntegerSetting("email.fetch.max.count", defaultMaxFetchCount);
    }

    public Integer getInitialSyncDays() {
        return getIntegerSetting("email.fetch.initial.sync.days", defaultInitialSyncDays);
    }

    // =====================================================================
    // Storage Getters
    // =====================================================================

    public String getEmailStorageBasePath() {
        return getStringSetting("email.storage.base.path", defaultStorageBasePath);
    }

    public Long getMaxStorageSize() {
        return getLongSetting("email.storage.max.size", defaultMaxStorageSize);
    }

    public Long getMaxEmlFileSize() {
        return getLongSetting("email.eml.max.file.size", defaultMaxEmlFileSize);
    }

    public Long getMaxAttachmentFileSize() {
        return getLongSetting("email.attachment.max.file.size", defaultMaxAttachmentFileSize);
    }

    public Boolean isSentCaptureEnabled() {
        return getBooleanSetting("email.sent.capture.enabled", defaultSentCaptureEnabled);
    }

    // =====================================================================
    // Helper Methods
    // =====================================================================

    private String getStringSetting(String key, String fallback) {
        try {
            EmailSetting setting = emailSettingRepository.findActiveBySettingKey(key).orElse(null);
            if (setting != null && setting.getSettingValue() != null) {
                return setting.getSettingValue();
            }
        } catch (Exception e) {
            log.debug("Failed to read email setting '{}' from DB, using fallback: {}", key, fallback);
        }
        return fallback;
    }

    private Boolean getBooleanSetting(String key, Boolean fallback) {
        try {
            EmailSetting setting = emailSettingRepository.findActiveBySettingKey(key).orElse(null);
            if (setting != null && setting.getSettingValue() != null) {
                return Boolean.parseBoolean(setting.getSettingValue());
            }
        } catch (Exception e) {
            log.debug("Failed to read email setting '{}' from DB, using fallback: {}", key, fallback);
        }
        return fallback;
    }

    private Integer getIntegerSetting(String key, Integer fallback) {
        try {
            EmailSetting setting = emailSettingRepository.findActiveBySettingKey(key).orElse(null);
            if (setting != null && setting.getSettingValue() != null) {
                return Integer.parseInt(setting.getSettingValue());
            }
        } catch (Exception e) {
            log.debug("Failed to read email setting '{}' from DB, using fallback: {}", key, fallback);
        }
        return fallback;
    }

    private Long getLongSetting(String key, Long fallback) {
        try {
            EmailSetting setting = emailSettingRepository.findActiveBySettingKey(key).orElse(null);
            if (setting != null && setting.getSettingValue() != null) {
                return Long.parseLong(setting.getSettingValue());
            }
        } catch (Exception e) {
            log.debug("Failed to read email setting '{}' from DB, using fallback: {}", key, fallback);
        }
        return fallback;
    }
}
