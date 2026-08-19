package com.itineraryledger.kabengosafaris.Feature;

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
 * One feature switch, stored.
 *
 * Shaped like every other settings row in this system — key, value, data type, category — on purpose:
 * the panel already has a page that renders rows of that shape, so the switches need no new screen,
 * and an administrator finds them where they look for every other switch.
 */
@Entity
@Table(name = "feature_settings", uniqueConstraints = @UniqueConstraint(columnNames = "setting_key"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** the feature's key, e.g. `fleet` */
    @Column(name = "setting_key", nullable = false, length = 100)
    private String settingKey;

    /** "true" or "false" — a string, like every other settings row */
    @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
    private String settingValue;

    @Column(name = "data_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SettingDataType dataType = SettingDataType.BOOLEAN;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** the part of the panel this feature belongs to */
    @Column(nullable = false, length = 40)
    private String category;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean enabled() {
        return Boolean.parseBoolean(settingValue);
    }
}
