package com.itineraryledger.kabengosafaris.Security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SecuritySettings entity.
 * Provides database access operations for security configuration settings.
 */
@Repository
public interface SecuritySettingsRepository extends JpaRepository<SecuritySettings, Long> {

    /**
     * Find security setting by setting key
     * @param settingKey the setting key (e.g., 'jwt.expiration.time.minutes')
     * @return Optional containing the setting if found
     */
    Optional<SecuritySettings> findBySettingKey(String settingKey);

    /**
     * Find active setting by setting key
     * @param settingKey the setting key
     * @return Optional containing the active setting if found
     */
    @Query("SELECT setting FROM SecuritySettings setting WHERE setting.settingKey = :settingKey AND setting.active = true")
    Optional<SecuritySettings> findActiveBySettingKey(@Param("settingKey") String settingKey);

    /**
     * Get all active security settings
     * @return List of all active settings
     */
    @Query("SELECT setting FROM SecuritySettings setting WHERE setting.active = true ORDER BY setting.category, setting.settingKey")
    List<SecuritySettings> findAllActive();

    /**
     * Get all active settings by category
     * @param category the category
     * @return List of active settings in that category
     */
    @Query("SELECT setting FROM SecuritySettings setting WHERE setting.category = :category AND setting.active = true ORDER BY setting.settingKey")
    List<SecuritySettings> findActiveByCategoryOrderBySettingKeyAsc(@Param("category") String category);

    /**
     * Get all system default settings
     * @return List of all system default settings
     */
    @Query("SELECT setting FROM SecuritySettings setting WHERE setting.isSystemDefault = true ORDER BY setting.category, setting.settingKey")
    List<SecuritySettings> findAllSystemDefaults();

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
    @Query("SELECT setting FROM SecuritySettings setting WHERE setting.requiresRestart = true AND setting.active = true")
    List<SecuritySettings> findAllThatRequireRestart();
}
