package com.itineraryledger.kabengosafaris.AuditLog.AuditLogSettings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
/**
 * Repository interface for AuditLogSetting entity.
 * Provides database access operations for audit logging settings.
 */
@Repository
public interface AuditLogSettingRepository extends JpaRepository<AuditLogSetting, Long> {

    /**
     * Find setting by setting key
     * @param settingKey the setting key (e.g., 'audit.log.enabled')
     * @return AuditLogSetting if found, else null
     */
    AuditLogSetting findBySettingKey(String settingKey);
    
    /**
     * Check if a setting key exists
     * @param settingKey the setting key
     * @return true if exists, false otherwise
     */
    boolean existsBySettingKey(String settingKey);
    
}
