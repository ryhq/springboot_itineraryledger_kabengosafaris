package com.itineraryledger.kabengosafaris.Backup.BackupSettings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for BackupSetting entity
 * Provides CRUD operations and custom queries for backup settings
 */
@Repository
public interface BackupSettingsRepository extends JpaRepository<BackupSetting, Long> {

    /**
     * Find a setting by its key
     *
     * @param settingKey the setting key to search for
     * @return Optional containing the BackupSetting if found
     */
    Optional<BackupSetting> findBySettingKey(String settingKey);

    /**
     * Check if a setting exists by its key
     *
     * @param settingKey the setting key to check
     * @return true if the setting exists, false otherwise
     */
    boolean existsBySettingKey(String settingKey);

    /**
     * Find all active settings
     *
     * @return List of active BackupSettings
     */
    List<BackupSetting> findByActiveTrue();

    /**
     * Find all settings by category
     *
     * @param category the category to filter by
     * @return List of BackupSettings in the specified category
     */
    List<BackupSetting> findByCategory(BackupSetting.Category category);

    /**
     * Find all active settings by category
     *
     * @param category the category to filter by
     * @return List of active BackupSettings in the specified category
     */
    List<BackupSetting> findByCategoryAndActiveTrue(BackupSetting.Category category);

    /**
     * Find all system default settings
     *
     * @return List of system default BackupSettings
     */
    List<BackupSetting> findByIsSystemDefaultTrue();

    /**
     * Delete a setting by its key (only if it's not a system default)
     *
     * @param settingKey the setting key to delete
     */
    void deleteBySettingKeyAndIsSystemDefaultFalse(String settingKey);
}
