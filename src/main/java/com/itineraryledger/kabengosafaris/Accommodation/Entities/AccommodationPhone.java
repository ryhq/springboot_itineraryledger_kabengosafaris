package com.itineraryledger.kabengosafaris.Accommodation.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * AccommodationPhone Entity - Manages phone numbers for accommodations
 *
 * Supports multiple phone numbers per accommodation (landline, mobile, WhatsApp, etc.)
 */
@Entity
@Table(name = "accommodation_phones", indexes = {
    @Index(name = "idx_accommodation_phone_accommodation_id", columnList = "accommodation_id"),
    @Index(name = "idx_accommodation_phone_type", columnList = "phone_type"),
    @Index(name = "idx_accommodation_phone_is_primary", columnList = "is_primary")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccommodationPhone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    @Column(name = "phone_number", nullable = false, length = 50)
    private String phoneNumber; // E.g., "+255 123 456 789"

    @Column(name = "country_code", length = 10)
    private String countryCode; // E.g., "+255" for Tanzania

    @Enumerated(EnumType.STRING)
    @Column(name = "phone_type", nullable = false, length = 50)
    private PhoneType phoneType;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false; // Primary contact number

    @Builder.Default
    @Column(name = "is_whatsapp", nullable = false)
    private Boolean isWhatsApp = false; // WhatsApp available on this number

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "label", length = 100)
    private String label; // Optional label (e.g., "Reception 24/7", "Emergency Line")

    @Column(name = "operating_hours", length = 200)
    private String operatingHours; // E.g., "24/7" or "8:00 AM - 6:00 PM"

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Phone types for accommodation
     */
    public enum PhoneType {
        LANDLINE("Landline", "Fixed landline number"),
        MOBILE("Mobile", "Mobile phone number"),
        RESERVATIONS("Reservations", "Reservations hotline"),
        RECEPTION("Reception", "Reception desk"),
        EMERGENCY("Emergency", "Emergency contact"),
        FAX("Fax", "Fax number"),
        TOLL_FREE("Toll Free", "Toll-free number"),
        WHATSAPP("WhatsApp", "WhatsApp Business number"),
        OTHER("Other", "Other phone type");

        private final String displayName;
        private final String description;

        PhoneType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }
}
