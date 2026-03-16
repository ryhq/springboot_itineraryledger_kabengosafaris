package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.GlobalEnums.SettingDataType;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = "setting_key")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", nullable = false, length = 100)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
    private String settingValue;

    @Column(name = "data_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private SettingDataType dataType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "is_system_default", nullable = false)
    @Builder.Default
    private Boolean isSystemDefault = false;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(name = "requires_restart", nullable = false)
    @Builder.Default
    private Boolean requiresRestart = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum Category {
        EMAIL_RETENTION("Email Retention Settings"),
        EMAIL_FETCH("Email Fetch Settings"),
        EMAIL_STORAGE("Email Storage Settings");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
