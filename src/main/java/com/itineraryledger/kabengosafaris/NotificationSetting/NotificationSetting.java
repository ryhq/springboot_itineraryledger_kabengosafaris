package com.itineraryledger.kabengosafaris.NotificationSetting;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.GlobalEnums.SettingDataType;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = "setting_key")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String settingKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String settingValue;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SettingDataType dataType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isSystemDefault = false;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Category category = Category.NEWSLETTER;

    @Column(nullable = false)
    @Builder.Default
    private Boolean requiresRestart = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum Category {
        NEWSLETTER("Newsletter Notification Settings"),
        BOOKING_INQUIRY("Booking Inquiry Notification Settings"),
        CONTACT_US("Contact Us Notification Settings"),
        /**
         * Reminders that a supplier's bill falls due.
         *
         * The other three are about somebody contacting US. This one is the opposite: nobody writes
         * in to say a provisional hold is about to lapse, so the only way the date is not missed is
         * if this installation says so first.
         */
        BILL_DUE("Bill Due Reminder Settings");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
