package com.itineraryledger.kabengosafaris.AuditLog.Config;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity class to store Audit Logging Configuration in the database.
 * This allows dynamic configuration changes without restarting the application.
 *
 * Replaces static application.properties audit configuration with database-driven settings.
 */
@Entity
@Table(name = "audit_log_config", uniqueConstraints = {
        @UniqueConstraint(columnNames = "config_key")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Configuration key (e.g., 'audit.log.enabled', 'audit.log.retention.days')
     */
    @Column(nullable = false, length = 100)
    private String configKey;

    /**
     * Configuration value (stored as string, parsed based on data type)
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String configValue;

    /**
     * Data type of the configuration value
     * Possible values: STRING, INTEGER, BOOLEAN, LONG
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ConfigDataType dataType;

    /**
     * Human-readable description of this configuration
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Whether this is an active configuration
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Whether this configuration is a system default (cannot be deleted)
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isSystemDefault = false;

    /**
     * Timestamp when the configuration was created
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the configuration was last updated
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Supported configuration data types
     */
    public enum ConfigDataType {
        STRING, INTEGER, BOOLEAN, LONG, DOUBLE
    }
}
