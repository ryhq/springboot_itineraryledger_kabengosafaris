package com.itineraryledger.kabengosafaris.Translation.Settings;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.GlobalEnums.SettingDataType;

import java.time.LocalDateTime;

/**
 * Entity class to store Translation/LibreTranslate Configuration settings in the database.
 * This allows dynamic configuration changes without restarting the application.
 *
 * Replaces static code constants with database-driven settings for:
 * - LibreTranslate enabled/disabled
 * - LibreTranslate base URL
 * - Request timeout
 * - Maximum characters per translation
 * - Default source/target language
 * - Supported languages
 * - Translation caching settings
 */
@Entity
@Table(name = "translation_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = "setting_key")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Setting key (e.g., 'libretranslate.enabled', 'libretranslate.base.url')
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
     * Category of the translation setting
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Category category;

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
     * Categories for organizing translation settings
     */
    public enum Category {
        CONNECTION("LibreTranslate Connection Settings"),
        LANGUAGES("Language Settings"),
        CACHING("Translation Caching Settings"),
        LIMITS("Translation Limits Settings");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
