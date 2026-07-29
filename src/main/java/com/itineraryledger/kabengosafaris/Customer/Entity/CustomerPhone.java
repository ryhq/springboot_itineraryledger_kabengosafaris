package com.itineraryledger.kabengosafaris.Customer.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * CustomerPhone Entity - Manages phone numbers for customers
 *
 * Supports multiple phone numbers per customer (personal, work, WhatsApp, emergency, etc.)
 */
@Entity
@Table(name = "customer_phones",
    indexes = {
        @Index(name = "idx_customer_phone_customer_id", columnList = "customer_id"),
        @Index(name = "idx_customer_phone_type", columnList = "phone_type"),
        @Index(name = "idx_customer_phone_is_primary", columnList = "is_primary"),
        @Index(name = "idx_customer_phone_number", columnList = "phone_number")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerPhone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    @NotBlank(message = "Phone number is required")
    @Column(name = "phone_number", nullable = false, length = 50)
    private String phoneNumber; // E.g., "+1 555 123 4567"

    @Column(name = "country_code", length = 10)
    private String countryCode; // E.g., "+1" for USA, "+255" for Tanzania

    @Enumerated(EnumType.STRING)
    @Column(name = "phone_type", nullable = false, length = 50)
    @Builder.Default
    private PhoneType phoneType = PhoneType.MOBILE;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false; // Primary contact number

    @Builder.Default
    @Column(name = "is_whatsapp", nullable = false)
    private Boolean isWhatsApp = false; // WhatsApp available on this number

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(length = 100)
    private String label; // Optional label (e.g., "Home", "Office", "Spouse")

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Phone types for customers
     */
    public enum PhoneType {
        MOBILE("Mobile", "Mobile phone number"),
        LANDLINE("Landline", "Fixed landline number"),
        WORK("Work", "Work/office number"),
        HOME("Home", "Home landline"),
        WHATSAPP("WhatsApp", "WhatsApp number"),
        SPOUSE("Spouse", "Spouse/partner phone"),
        ASSISTANT("Assistant", "Secretary/assistant phone"),
        EMERGENCY("Emergency", "Emergency contact"),
        FAX("Fax", "Fax number"),
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
