package com.itineraryledger.kabengosafaris.Backup.BackupSettings;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.GlobalEnums.SettingDataType;

import java.time.LocalDateTime;

/**
 * Entity class to store Backup Configuration settings in the database.
 * This allows dynamic configuration changes without restarting the application.
 *
 * Manages backup settings for:
 * - Database backups (MySQL/PostgreSQL)
 * - File system backups (documents, images, etc.)
 * - Backup scheduling and retention
 * - Storage locations and compression
 */
@Entity
@Table(name = "backup_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = "setting_key")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Setting key (e.g., 'backup.enabled', 'backup.retention.days')
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
     * Category of the backup setting (GENERAL, SCHEDULE, DATABASE, etc.)
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Category category = Category.GENERAL;

    /**
     * Whether changing this setting requires application restart
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean requiresRestart = false;

    /**
     * Timestamp when the setting was created
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the setting was last updated
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Categories for organizing backup settings
     */
    public enum Category {
        GENERAL("General Backup Settings"),
        SCHEDULE("Backup Scheduling Settings"),
        DATABASE("Database Backup Settings"),
        FILES("File System Backup Settings"),
        STORAGE("Backup Storage Settings"),
        RETENTION("Backup Retention Settings"),
        COMPRESSION("Backup Compression Settings"),
        NOTIFICATION("Backup Notification Settings");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
