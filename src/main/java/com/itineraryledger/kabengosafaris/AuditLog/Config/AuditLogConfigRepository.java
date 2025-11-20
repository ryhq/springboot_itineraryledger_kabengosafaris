package com.itineraryledger.kabengosafaris.AuditLog.Config;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AuditLogConfig entity.
 * Provides database access operations for audit logging configurations.
 */
@Repository
public interface AuditLogConfigRepository extends JpaRepository<AuditLogConfig, Long> {

    /**
     * Find configuration by config key
     * @param configKey the configuration key (e.g., 'audit.log.enabled')
     * @return Optional containing the configuration if found
     */
    Optional<AuditLogConfig> findByConfigKey(String configKey);

    /**
     * Find active configuration by config key
     * @param configKey the configuration key
     * @return Optional containing the active configuration if found
     */
    @Query("SELECT config FROM AuditLogConfig config WHERE config.configKey = :configKey AND config.active = true")
    Optional<AuditLogConfig> findActiveByConfigKey(@Param("configKey") String configKey);

    /**
     * Get all active configurations
     * @return List of all active configurations
     */
    @Query("SELECT config FROM AuditLogConfig config WHERE config.active = true ORDER BY config.configKey")
    List<AuditLogConfig> findAllActive();

    /**
     * Get all system default configurations
     * @return List of all system default configurations
     */
    @Query("SELECT config FROM AuditLogConfig config WHERE config.isSystemDefault = true ORDER BY config.configKey")
    List<AuditLogConfig> findAllSystemDefaults();

    /**
     * Check if a configuration key exists
     * @param configKey the configuration key
     * @return true if exists, false otherwise
     */
    boolean existsByConfigKey(String configKey);
}
