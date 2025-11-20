package com.itineraryledger.kabengosafaris.AuditLog.Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Service for managing Audit Logging Configuration.
 * Provides methods to create, read, update, and delete audit log configurations from the database.
 *
 * This service allows dynamic modification of audit settings without restarting the application.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuditLogConfigService {

    private final AuditLogConfigRepository auditLogConfigRepository;

    /**
     * Get configuration value by key, with type conversion
     * @param configKey the configuration key
     * @return the configuration value as a string
     * @throws NoSuchElementException if configuration is not found
     */
    public String getConfigValue(String configKey) {
        return auditLogConfigRepository.findActiveByConfigKey(configKey)
                .orElseThrow(() -> new NoSuchElementException("Configuration not found: " + configKey))
                .getConfigValue();
    }

    /**
     * Get configuration value as Integer
     * @param configKey the configuration key
     * @return the configuration value as Integer
     */
    public Integer getConfigValueAsInteger(String configKey) {
        String value = getConfigValue(configKey);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.error("Failed to parse configuration '{}' as Integer: {}", configKey, value);
            throw new IllegalArgumentException("Configuration value is not a valid integer: " + configKey);
        }
    }

    /**
     * Get configuration value as Long
     * @param configKey the configuration key
     * @return the configuration value as Long
     */
    public Long getConfigValueAsLong(String configKey) {
        String value = getConfigValue(configKey);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.error("Failed to parse configuration '{}' as Long: {}", configKey, value);
            throw new IllegalArgumentException("Configuration value is not a valid long: " + configKey);
        }
    }

    /**
     * Get configuration value as Boolean
     * @param configKey the configuration key
     * @return the configuration value as Boolean
     */
    public Boolean getConfigValueAsBoolean(String configKey) {
        String value = getConfigValue(configKey);
        return Boolean.parseBoolean(value);
    }

    /**
     * Get configuration value as Double
     * @param configKey the configuration key
     * @return the configuration value as Double
     */
    public Double getConfigValueAsDouble(String configKey) {
        String value = getConfigValue(configKey);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.error("Failed to parse configuration '{}' as Double: {}", configKey, value);
            throw new IllegalArgumentException("Configuration value is not a valid double: " + configKey);
        }
    }

    /**
     * Get configuration with all details
     * @param configKey the configuration key
     * @return the AuditLogConfig entity
     */
    @Transactional(readOnly = true)
    public AuditLogConfig getConfig(String configKey) {
        return auditLogConfigRepository.findActiveByConfigKey(configKey)
                .orElseThrow(() -> new NoSuchElementException("Configuration not found: " + configKey));
    }

    /**
     * Save or update a configuration
     * @param config the configuration to save
     * @return the saved configuration
     */
    public AuditLogConfig saveConfig(AuditLogConfig config) {
        try {
            AuditLogConfig saved = auditLogConfigRepository.save(config);
            log.info("Configuration saved/updated: {}", config.getConfigKey());
            return saved;
        } catch (Exception e) {
            log.error("Failed to save configuration: {}", config.getConfigKey(), e);
            throw e;
        }
    }

    /**
     * Create a new configuration
     * @param configKey the configuration key
     * @param configValue the configuration value
     * @param dataType the data type
     * @param description the description
     * @return the created configuration
     */
    public AuditLogConfig createConfig(String configKey, String configValue,
                                       AuditLogConfig.ConfigDataType dataType,
                                       String description) {
        if (auditLogConfigRepository.existsByConfigKey(configKey)) {
            throw new IllegalArgumentException("Configuration already exists: " + configKey);
        }

        AuditLogConfig config = AuditLogConfig.builder()
                .configKey(configKey)
                .configValue(configValue)
                .dataType(dataType)
                .description(description)
                .active(true)
                .isSystemDefault(false)
                .build();

        return saveConfig(config);
    }

    /**
     * Update an existing configuration value
     * @param configKey the configuration key
     * @param newValue the new configuration value
     * @return the updated configuration
     */
    public AuditLogConfig updateConfigValue(String configKey, String newValue) {
        AuditLogConfig config = getConfig(configKey);
        config.setConfigValue(newValue);
        return saveConfig(config);
    }

    /**
     * Deactivate a configuration (soft delete)
     * @param configKey the configuration key
     */
    public void deactivateConfig(String configKey) {
        AuditLogConfig config = getConfig(configKey);
        if (config.getIsSystemDefault()) {
            throw new IllegalArgumentException("Cannot deactivate system default configuration: " + configKey);
        }
        config.setActive(false);
        saveConfig(config);
        log.info("Configuration deactivated: {}", configKey);
    }

    /**
     * Reactivate a configuration
     * @param configKey the configuration key
     */
    public void reactivateConfig(String configKey) {
        AuditLogConfig config = auditLogConfigRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new NoSuchElementException("Configuration not found: " + configKey));
        config.setActive(true);
        saveConfig(config);
        log.info("Configuration reactivated: {}", configKey);
    }

    /**
     * Get all active configurations
     * @return list of all active configurations
     */
    @Transactional(readOnly = true)
    public List<AuditLogConfig> getAllActiveConfigs() {
        return auditLogConfigRepository.findAllActive();
    }

    /**
     * Get all system default configurations
     * @return list of all system default configurations
     */
    @Transactional(readOnly = true)
    public List<AuditLogConfig> getAllSystemDefaults() {
        return auditLogConfigRepository.findAllSystemDefaults();
    }

    /**
     * Delete a configuration (hard delete)
     * Note: System default configurations cannot be deleted
     * @param configKey the configuration key
     */
    public void deleteConfig(String configKey) {
        AuditLogConfig config = getConfig(configKey);
        if (config.getIsSystemDefault()) {
            throw new IllegalArgumentException("Cannot delete system default configuration: " + configKey);
        }
        auditLogConfigRepository.delete(config);
        log.info("Configuration deleted: {}", configKey);
    }

    /**
     * Check if a configuration exists
     * @param configKey the configuration key
     * @return true if configuration exists and is active
     */
    @Transactional(readOnly = true)
    public boolean configExists(String configKey) {
        return auditLogConfigRepository.findActiveByConfigKey(configKey).isPresent();
    }

    /**
     * Reset configuration to system default value
     * Note: Only works with system default configurations
     * @param configKey the configuration key
     */
    public void resetToDefault(String configKey) {
        List<AuditLogConfig> systemDefaults = getAllSystemDefaults();
        var defaultConfig = systemDefaults.stream()
                .filter(config -> config.getConfigKey().equals(configKey))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("System default configuration not found: " + configKey));

        updateConfigValue(configKey, defaultConfig.getConfigValue());
        log.info("Configuration reset to system default: {}", configKey);
    }
}
