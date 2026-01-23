package com.itineraryledger.kabengosafaris.FileSettings;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.GlobalEnums.SettingDataType;

import java.time.LocalDateTime;

/**
 * Entity class to store File Upload Configuration settings in the database.
 * This allows dynamic configuration changes without restarting the application.
 *
 * Manages settings for:
 * - General file upload settings (enabled, max size, allowed/blocked extensions)
 * - Email signature upload settings
 * - Email template upload settings
 * - PDF template upload settings
 */
@Entity
@Table(name = "file_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = "setting_key")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Setting key (e.g., 'file.upload.enabled', 'file.upload.max.file.size')
     */
    @Column(name = "setting_key", nullable = false, length = 100)
    private String settingKey;

    /**
     * Setting value (stored as string, parsed based on data type)
     */
    @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
    private String settingValue;

    /**
     * Data type of the setting value
     * Possible values: STRING, INTEGER, BOOLEAN, LONG, DOUBLE
     */
    @Column(name = "data_type", nullable = false)
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
    @Column(name = "is_system_default", nullable = false)
    @Builder.Default
    private Boolean isSystemDefault = false;

    /**
     * Category of the file setting
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Category category;

    /**
     * Whether changing this setting requires application restart
     */
    @Column(name = "requires_restart", nullable = false)
    @Builder.Default
    private Boolean requiresRestart = false;

    /**
     * Timestamp when the setting was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the setting was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Categories for organizing file settings
     */
    public enum Category {
        UPLOAD("General File Upload Settings"),
        EMAIL_SIGNATURE("Email Signature Upload Settings"),
        EMAIL_TEMPLATE("Email Template Upload Settings"),
        PDF_TEMPLATE("PDF Template Upload Settings");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
