package com.itineraryledger.kabengosafaris.Security;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity class to store Security Configuration settings in the database.
 * This allows dynamic configuration changes without restarting the application.
 *
 * Replaces static code constants with database-driven settings for:
 * - JWT Token expiration time
 * - ID Obfuscation length and salt length
 * - Password policies
 * - Session timeouts
 * - And other security-related configurations
 */
@Entity
@Table(name = "security_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = "setting_key")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecuritySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Setting key (e.g., 'jwt.expiration.time.minutes', 'idObfuscator.obfuscated.length')
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
     * Category of the security setting (JWT, OBFUSCATION, PASSWORD, SESSION, etc.)
     */
    @Column(length = 50)
    private String category;

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
     * Supported setting data types
     */
    public enum SettingDataType {
        STRING, INTEGER, BOOLEAN, LONG, DOUBLE
    }

    /**
     * Categories for organizing settings
     */
    public enum Category {
        JWT("JWT Token Settings"),
        OBFUSCATION("ID Obfuscation Settings"),
        PASSWORD("Password Policy Settings"),
        SESSION("Session Management Settings"),
        ACCOUNT_LOCKOUT("Account Lockout Settings"),
        RATE_LIMIT("Rate Limiting Settings"),
        CORS("CORS Settings"),
        OTHER("Other Security Settings");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
