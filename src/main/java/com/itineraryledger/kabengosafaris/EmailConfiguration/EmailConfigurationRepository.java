package com.itineraryledger.kabengosafaris.EmailConfiguration;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for EmailConfiguration entity
 * Provides CRUD operations and custom queries for email configurations
 */
@Repository
public interface EmailConfigurationRepository extends JpaRepository<EmailConfiguration, Long> {

    /**
     * Find configuration by name
     */
    Optional<EmailConfiguration> findByName(String name);

    /**
     * Find configuration by from email address
     */
    Optional<EmailConfiguration> findByFromEmail(String fromEmail);

    /**
     * Find the default email configuration
     */
    Optional<EmailConfiguration> findByIsDefaultTrue();

    /**
     * Find all active configurations
     */
    List<EmailConfiguration> findByEnabledTrue();

    /**
     * Find all configurations for a specific provider
     */
    List<EmailConfiguration> findByProviderType(String providerType);

    /**
     * Find all enabled configurations for a provider
     */
    List<EmailConfiguration> findByProviderTypeAndEnabledTrue(String providerType);

    /**
     * Find configurations created by a specific user
     */
    List<EmailConfiguration> findByCreatedBy(String createdBy);

    /**
     * Count total configurations
     */
    Long countByEnabled(Boolean enabled);

    /**
     * Check if a configuration with given name exists
     */
    boolean existsByName(String name);

    /**
     * Check if a configuration with given from email exists
     */
    boolean existsByFromEmail(String fromEmail);

    /**
     * Update the default configuration flag
     * Sets all to false except the one with given id
     */
    @Query("UPDATE EmailConfiguration SET isDefault = false WHERE id != :id")
    void setOnlyOneDefault(@Param("id") Long id);

    /**
     * Update email sent count for a configuration
     */
    @Query("UPDATE EmailConfiguration SET emailsSentCount = emailsSentCount + 1 WHERE id = :id")
    void incrementEmailsSentCount(@Param("id") Long id);

    /**
     * Update failed email count for a configuration
     */
    @Query("UPDATE EmailConfiguration SET emailsFailedCount = emailsFailedCount + 1 WHERE id = :id")
    void incrementEmailsFailedCount(@Param("id") Long id);

    /**
     * Find configurations with highest success rate
     * Success rate = emailsSentCount / (emailsSentCount + emailsFailedCount)
     */
    @Query("SELECT ec FROM EmailConfiguration ec " +
           "WHERE ec.enabled = true " +
           "ORDER BY CASE WHEN (ec.emailsSentCount + ec.emailsFailedCount) = 0 THEN 1 " +
           "       ELSE ec.emailsSentCount / CAST(ec.emailsSentCount + ec.emailsFailedCount AS DOUBLE) END DESC")
    List<EmailConfiguration> findBySuccessRate();
}
