package com.itineraryledger.kabengosafaris.ImageSettings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ImageSetting entity.
 * Provides database access operations for image configuration settings.
 */
@Repository
public interface ImageSettingRepository extends JpaRepository<ImageSetting, Long> {

    /**
     * Find image setting by setting key
     * @param settingKey the setting key (e.g., 'image.upload.enabled')
     * @return Optional containing the setting if found
     */
    Optional<ImageSetting> findBySettingKey(String settingKey);

    /**
     * Find active setting by setting key
     * @param settingKey the setting key
     * @return Optional containing the active setting if found
     */
    @Query("SELECT setting FROM ImageSetting setting WHERE setting.settingKey = :settingKey AND setting.active = true")
    Optional<ImageSetting> findActiveBySettingKey(@Param("settingKey") String settingKey);

    /**
     * Get all active image settings
     * @return List of all active settings
     */
    @Query("SELECT setting FROM ImageSetting setting WHERE setting.active = true ORDER BY setting.category, setting.settingKey")
    List<ImageSetting> findAllActive();

    /**
     * Get all active settings by category
     * @param category the category
     * @return List of active settings in that category
     */
    @Query("SELECT setting FROM ImageSetting setting WHERE setting.category = :category AND setting.active = true ORDER BY setting.settingKey")
    List<ImageSetting> findActiveByCategoryOrderBySettingKeyAsc(@Param("category") ImageSetting.Category category);

    /**
     * Get all system default settings
     * @return List of all system default settings
     */
    @Query("SELECT setting FROM ImageSetting setting WHERE setting.isSystemDefault = true ORDER BY setting.category, setting.settingKey")
    List<ImageSetting> findAllSystemDefaults();

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
    @Query("SELECT setting FROM ImageSetting setting WHERE setting.requiresRestart = true AND setting.active = true")
    List<ImageSetting> findAllThatRequireRestart();
}
