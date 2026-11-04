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
        BILL_DUE("Bill Due Reminder Settings"),
        /**
         * The client portal: a customer has asked us to look, accepted, or declined.
         *
         * These were going to the BACKUP alert list, which is a page about scheduling and
         * retention. Nobody deciding who should hear that a customer accepted a quotation would
         * look there, and twice nobody did. They belong beside the other three, which are also
         * "somebody outside has done something and one of us needs to know".
         *
         * The acceptance is the one that must not be missed: pressing Accept in the portal
         * deliberately does NOT move the quotation's own status, because that path is audited
         * and the audit row needs a real person. If this reaches nobody, a customer who has said
         * yes is sitting in a state nothing here watches.
         */
        CLIENT_PORTAL("Client Portal Notification Settings");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
