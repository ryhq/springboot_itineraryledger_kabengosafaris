package com.itineraryledger.kabengosafaris.Translation.Settings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TranslationSetting entity.
 * Provides database access operations for translation configuration settings.
 */
@Repository
public interface TranslationSettingRepository extends JpaRepository<TranslationSetting, Long> {

    /**
     * Find translation setting by setting key
     * @param settingKey the setting key (e.g., 'libretranslate.enabled')
     * @return Optional containing the setting if found
     */
    Optional<TranslationSetting> findBySettingKey(String settingKey);

    /**
     * Find active setting by setting key
     * @param settingKey the setting key
     * @return Optional containing the active setting if found
     */
    @Query("SELECT setting FROM TranslationSetting setting WHERE setting.settingKey = :settingKey AND setting.active = true")
    Optional<TranslationSetting> findActiveBySettingKey(@Param("settingKey") String settingKey);

    /**
     * Get all active translation settings
     * @return List of all active settings
     */
    @Query("SELECT setting FROM TranslationSetting setting WHERE setting.active = true ORDER BY setting.category, setting.settingKey")
    List<TranslationSetting> findAllActive();

    /**
     * Get all active settings by category
     * @param category the category
     * @return List of active settings in that category
     */
    @Query("SELECT setting FROM TranslationSetting setting WHERE setting.category = :category AND setting.active = true ORDER BY setting.settingKey")
    List<TranslationSetting> findActiveByCategoryOrderBySettingKeyAsc(@Param("category") TranslationSetting.Category category);

    /**
     * Get all system default settings
     * @return List of all system default settings
     */
    @Query("SELECT setting FROM TranslationSetting setting WHERE setting.isSystemDefault = true ORDER BY setting.category, setting.settingKey")
    List<TranslationSetting> findAllSystemDefaults();

    /**
     * Check if a setting key exists
     * @param settingKey the setting key
     * @return true if exists, false otherwise
     */
    boolean existsBySettingKey(String settingKey);

    /**
     * Find all settings that require restart on change
     * @return List of settings that require restart
     */
    @Query("SELECT setting FROM TranslationSetting setting WHERE setting.requiresRestart = true AND setting.active = true")
    List<TranslationSetting> findAllThatRequireRestart();
}
