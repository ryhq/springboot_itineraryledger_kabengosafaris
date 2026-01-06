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
 * AccommodationEmail Entity - Manages email addresses for accommodations
 *
 * Supports multiple email addresses per accommodation (reservations, info, management, etc.)
 */
@Entity
@Table(name = "accommodation_emails", indexes = {
    @Index(name = "idx_accommodation_email_accommodation_id", columnList = "accommodation_id"),
    @Index(name = "idx_accommodation_email_type", columnList = "email_type"),
    @Index(name = "idx_accommodation_email_is_primary", columnList = "is_primary")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccommodationEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_type", nullable = false, length = 50)
    private EmailType emailType;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false; // Primary contact email

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "label", length = 100)
    private String label; // Optional label (e.g., "Reservations Manager", "General Inquiries")

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Email types for accommodation
     */
    public enum EmailType {
        GENERAL("General", "General information and inquiries"),
        RESERVATIONS("Reservations", "Email for booking reservations"),
        SALES("Sales", "Sales and business development"),
        SUPPORT("Support", "Customer support"),
        BILLING("Billing", "Billing and invoicing"),
        MARKETING("Marketing", "Marketing and promotions"),
        MANAGEMENT("Management", "Management contact"),
        OTHER("Other", "Other purposes");

        private final String displayName;
        private final String description;

        EmailType(String displayName, String description) {
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
