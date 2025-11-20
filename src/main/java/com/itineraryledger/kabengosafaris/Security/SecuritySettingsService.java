package com.itineraryledger.kabengosafaris.Security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Service for managing Security Settings.
 * Provides methods to create, read, update, and delete security configuration settings from the database.
 *
 * This service allows dynamic modification of security settings without restarting the application.
 * Used by IdObfuscator, JwtTokenProvider, and other security components.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SecuritySettingsService {

    private final SecuritySettingsRepository securitySettingsRepository;

    /**
     * Get setting value by key, with type conversion
     * @param settingKey the setting key
     * @return the setting value as a string
     * @throws NoSuchElementException if setting is not found
     */
    public String getSettingValue(String settingKey) {
        return securitySettingsRepository.findActiveBySettingKey(settingKey)
                .orElseThrow(() -> new NoSuchElementException("Setting not found: " + settingKey))
                .getSettingValue();
    }

    /**
     * Get setting value as Integer
     * @param settingKey the setting key
     * @return the setting value as Integer
     */
    public Integer getSettingValueAsInteger(String settingKey) {
        String value = getSettingValue(settingKey);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.error("Failed to parse setting '{}' as Integer: {}", settingKey, value);
            throw new IllegalArgumentException("Setting value is not a valid integer: " + settingKey);
        }
    }

    /**
     * Get setting value as Long
     * @param settingKey the setting key
     * @return the setting value as Long
     */
    public Long getSettingValueAsLong(String settingKey) {
        String value = getSettingValue(settingKey);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.error("Failed to parse setting '{}' as Long: {}", settingKey, value);
            throw new IllegalArgumentException("Setting value is not a valid long: " + settingKey);
        }
    }

    /**
     * Get setting value as Boolean
     * @param settingKey the setting key
     * @return the setting value as Boolean
     */
    public Boolean getSettingValueAsBoolean(String settingKey) {
        String value = getSettingValue(settingKey);
        return Boolean.parseBoolean(value);
    }

    /**
     * Get setting value as Double
     * @param settingKey the setting key
     * @return the setting value as Double
     */
    public Double getSettingValueAsDouble(String settingKey) {
        String value = getSettingValue(settingKey);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.error("Failed to parse setting '{}' as Double: {}", settingKey, value);
            throw new IllegalArgumentException("Setting value is not a valid double: " + settingKey);
        }
    }

    /**
     * Get setting with all details
     * @param settingKey the setting key
     * @return the SecuritySettings entity
     */
    @Transactional(readOnly = true)
    public SecuritySettings getSetting(String settingKey) {
        return securitySettingsRepository.findActiveBySettingKey(settingKey)
                .orElseThrow(() -> new NoSuchElementException("Setting not found: " + settingKey));
    }

    /**
     * Save or update a setting
     * @param setting the setting to save
     * @return the saved setting
     */
    public SecuritySettings saveSetting(SecuritySettings setting) {
        try {
            SecuritySettings saved = securitySettingsRepository.save(setting);
            log.info("Security setting saved/updated: {} (category: {})", setting.getSettingKey(), setting.getCategory());
            return saved;
        } catch (Exception e) {
            log.error("Failed to save security setting: {}", setting.getSettingKey(), e);
            throw e;
        }
    }

    /**
     * Create a new setting
     * @param settingKey the setting key
     * @param settingValue the setting value
     * @param dataType the data type
     * @param description the description
     * @param category the category
     * @return the created setting
     */
    public SecuritySettings createSetting(String settingKey, String settingValue,
                                          SecuritySettings.SettingDataType dataType,
                                          String description, String category) {
        if (securitySettingsRepository.existsBySettingKey(settingKey)) {
            throw new IllegalArgumentException("Setting already exists: " + settingKey);
        }

        SecuritySettings setting = SecuritySettings.builder()
                .settingKey(settingKey)
                .settingValue(settingValue)
                .dataType(dataType)
                .description(description)
                .category(category)
                .active(true)
                .isSystemDefault(false)
                .requiresRestart(false)
                .build();

        return saveSetting(setting);
    }

    /**
     * Update an existing setting value
     * @param settingKey the setting key
     * @param newValue the new setting value
     * @return the updated setting
     */
    public SecuritySettings updateSettingValue(String settingKey, String newValue) {
        SecuritySettings setting = getSetting(settingKey);
        setting.setSettingValue(newValue);
        return saveSetting(setting);
    }

    /**
     * Deactivate a setting (soft delete)
     * @param settingKey the setting key
     */
    public void deactivateSetting(String settingKey) {
        SecuritySettings setting = getSetting(settingKey);
        if (setting.getIsSystemDefault()) {
            throw new IllegalArgumentException("Cannot deactivate system default setting: " + settingKey);
        }
        setting.setActive(false);
        saveSetting(setting);
        log.info("Security setting deactivated: {}", settingKey);
    }

    /**
     * Reactivate a deactivated setting
     * @param settingKey the setting key
     */
    public void reactivateSetting(String settingKey) {
        SecuritySettings setting = securitySettingsRepository.findBySettingKey(settingKey)
                .orElseThrow(() -> new NoSuchElementException("Setting not found: " + settingKey));
        setting.setActive(true);
        saveSetting(setting);
        log.info("Security setting reactivated: {}", settingKey);
    }

    /**
     * Get all active settings
     * @return list of all active settings
     */
    @Transactional(readOnly = true)
    public List<SecuritySettings> getAllActiveSettings() {
        return securitySettingsRepository.findAllActive();
    }

    /**
     * Get all active settings by category
     * @param category the category
     * @return list of active settings in that category
     */
    @Transactional(readOnly = true)
    public List<SecuritySettings> getActiveSettingsByCategory(String category) {
        return securitySettingsRepository.findActiveByCategoryOrderBySettingKeyAsc(category);
    }

    /**
     * Get all system default settings
     * @return list of all system default settings
     */
    @Transactional(readOnly = true)
    public List<SecuritySettings> getAllSystemDefaults() {
        return securitySettingsRepository.findAllSystemDefaults();
    }

    /**
     * Get all settings that require restart on change
     * @return list of settings requiring restart
     */
    @Transactional(readOnly = true)
    public List<SecuritySettings> getSettingsThatRequireRestart() {
        return securitySettingsRepository.findAllThatRequireRestart();
    }

    /**
     * Delete a setting (hard delete)
     * Note: System default settings cannot be deleted
     * @param settingKey the setting key
     */
    public void deleteSetting(String settingKey) {
        SecuritySettings setting = getSetting(settingKey);
        if (setting.getIsSystemDefault()) {
            throw new IllegalArgumentException("Cannot delete system default setting: " + settingKey);
        }
        securitySettingsRepository.delete(setting);
        log.info("Security setting deleted: {}", settingKey);
    }

    /**
     * Check if a setting exists
     * @param settingKey the setting key
     * @return true if setting exists and is active
     */
    @Transactional(readOnly = true)
    public boolean settingExists(String settingKey) {
        return securitySettingsRepository.findActiveBySettingKey(settingKey).isPresent();
    }

    /**
     * Reset setting to system default value
     * Note: Only works with system default settings
     * @param settingKey the setting key
     */
    public void resetToDefault(String settingKey) {
        List<SecuritySettings> systemDefaults = getAllSystemDefaults();
        var defaultSetting = systemDefaults.stream()
                .filter(setting -> setting.getSettingKey().equals(settingKey))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("System default setting not found: " + settingKey));

        updateSettingValue(settingKey, defaultSetting.getSettingValue());
        log.info("Security setting reset to system default: {}", settingKey);
    }

    // Convenience methods for commonly used security settings

    /**
     * Get JWT expiration time in milliseconds
     * @return expiration time in milliseconds
     */
    public long getJwtExpirationTimeMillis() {
        return getSettingValueAsLong("jwt.expiration.time.minutes") * 60 * 1000;
    }

    /**
     * Get JWT refresh token expiration time in milliseconds
     * @return refresh token expiration time in milliseconds
     */
    public long getJwtRefreshExpirationTimeMillis() {
        return getSettingValueAsLong("jwt.refresh.expiration.time.minutes") * 60 * 1000;
    }

    /**
     * Get ID obfuscation length
     * @return obfuscated ID length
     */
    public int getIdObfuscationLength() {
        return getSettingValueAsInteger("idObfuscator.obfuscated.length");
    }

    /**
     * Get ID obfuscation salt length
     * @return salt length for ID obfuscation
     */
    public int getIdObfuscationSaltLength() {
        return getSettingValueAsInteger("idObfuscator.salt.length");
    }

    /**
     * Check if ID obfuscation is enabled
     * @return true if enabled
     */
    public boolean isIdObfuscationEnabled() {
        return getSettingValueAsBoolean("idObfuscator.enabled");
    }

    /**
     * Get account lockout max failed attempts
     * @return max failed attempts before lockout
     */
    public int getAccountLockoutMaxAttempts() {
        return getSettingValueAsInteger("accountLockout.maxFailedAttempts");
    }

    /**
     * Get account lockout duration in minutes
     * @return lockout duration in minutes
     */
    public int getAccountLockoutDurationMinutes() {
        return getSettingValueAsInteger("accountLockout.lockoutDurationMinutes");
    }

    /**
     * Check if account lockout is enabled
     * @return true if enabled
     */
    public boolean isAccountLockoutEnabled() {
        return getSettingValueAsBoolean("accountLockout.enabled");
    }
}
