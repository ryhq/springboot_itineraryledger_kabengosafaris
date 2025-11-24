package com.itineraryledger.kabengosafaris.AuditLog.AuditLogSettings;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.GlobalEnums.SettingDataType;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity class to store Audit Logging Settings in the database.
 * This allows dynamic configuration changes without restarting the application.
 *
 * Replaces static application.properties audit configuration with database-driven settings.
 */
@Entity
@Table(name = "audit_log_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = "setting_key")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Setting key (e.g., 'audit.log.enabled', 'audit.log.retention.days')
     */
    @Column(nullable = false, length = 100)
    private String settingKey;

    /**
     * Setting value (stored as string, parsed based on data type)
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String settingValue;

    /**
     * Data type of the setting value
     * Possible values: STRING, INTEGER, BOOLEAN, LONG, DOUBLE
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SettingDataType dataType;

    /**
     * Human-readable description of this setting
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Whether this is an active setting
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Whether this setting is a system default (cannot be deleted)
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isSystemDefault = false;

    /**
     * Category of the audit log setting (GENERAL, CAPTURE, VALUES, etc.)
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Category category = Category.GENERAL;

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
     * Categories for organizing audit log settings
     */
    public enum Category {
        GENERAL("Audit Logging General Settings"),
        CAPTURE("Audit Logging Capture Settings"),
        VALUES("Audit Logging Value Settings");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
