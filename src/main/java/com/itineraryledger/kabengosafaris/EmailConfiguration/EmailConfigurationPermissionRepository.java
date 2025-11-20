package com.itineraryledger.kabengosafaris.EmailConfiguration;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for EmailConfigurationPermission entity
 * Manages access control to email configurations for users and roles
 */
@Repository
public interface EmailConfigurationPermissionRepository extends JpaRepository<EmailConfigurationPermission, Long> {

    /**
     * Find permissions by email configuration
     */
    List<EmailConfigurationPermission> findByEmailConfiguration(EmailConfiguration config);

    /**
     * Find active permissions by email configuration
     */
    List<EmailConfigurationPermission> findByEmailConfigurationAndEnabledTrue(EmailConfiguration config);

    /**
     * Find permissions for a specific user
     */
    List<EmailConfigurationPermission> findByUserIdAndEnabledTrue(Long userId);

    /**
     * Find permissions for a specific role
     */
    List<EmailConfigurationPermission> findByRoleIdAndEnabledTrue(Long roleId);

    /**
     * Find permission by configuration and user
     */
    List<EmailConfigurationPermission> findByEmailConfigurationAndUserId(EmailConfiguration config, Long userId);

    /**
     * Find permission by configuration and role
     */
    List<EmailConfigurationPermission> findByEmailConfigurationAndRoleId(EmailConfiguration config, Long roleId);

    /**
     * Find all valid (non-expired) permissions for a configuration
     */
    @Query("SELECT ecp FROM EmailConfigurationPermission ecp " +
           "WHERE ecp.emailConfiguration = :config " +
           "AND ecp.enabled = true " +
           "AND (ecp.expiresAt IS NULL OR ecp.expiresAt > :now)")
    List<EmailConfigurationPermission> findValidPermissions(
        @Param("config") EmailConfiguration config,
        @Param("now") LocalDateTime now
    );

    /**
     * Find all expired permissions
     */
    @Query("SELECT ecp FROM EmailConfigurationPermission ecp " +
           "WHERE ecp.expiresAt IS NOT NULL " +
           "AND ecp.expiresAt <= :now")
    List<EmailConfigurationPermission> findExpiredPermissions(@Param("now") LocalDateTime now);

    /**
     * Check if a user has permission to use a configuration
     */
    @Query("SELECT COUNT(ecp) > 0 FROM EmailConfigurationPermission ecp " +
           "WHERE ecp.emailConfiguration.id = :configId " +
           "AND ecp.user.id = :userId " +
           "AND ecp.enabled = true " +
           "AND (ecp.expiresAt IS NULL OR ecp.expiresAt > :now)")
    boolean userHasAccessToConfig(
        @Param("configId") Long configId,
        @Param("userId") Long userId,
        @Param("now") LocalDateTime now
    );

    /**
     * Check if a role has permission to use a configuration
     */
    @Query("SELECT COUNT(ecp) > 0 FROM EmailConfigurationPermission ecp " +
           "WHERE ecp.emailConfiguration.id = :configId " +
           "AND ecp.role.id = :roleId " +
           "AND ecp.enabled = true " +
           "AND (ecp.expiresAt IS NULL OR ecp.expiresAt > :now)")
    boolean roleHasAccessToConfig(
        @Param("configId") Long configId,
        @Param("roleId") Long roleId,
        @Param("now") LocalDateTime now
    );

    /**
     * Find all permissions granted by a specific user
     */
    List<EmailConfigurationPermission> findByGrantedBy(String grantedBy);

    /**
     * Delete expired permissions
     */
    @Query("DELETE FROM EmailConfigurationPermission ecp " +
           "WHERE ecp.expiresAt IS NOT NULL " +
           "AND ecp.expiresAt <= :now")
    void deleteExpiredPermissions(@Param("now") LocalDateTime now);
}
