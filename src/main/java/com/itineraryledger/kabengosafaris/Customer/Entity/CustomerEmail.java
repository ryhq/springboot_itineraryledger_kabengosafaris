package com.itineraryledger.kabengosafaris.Customer.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * CustomerEmail Entity - Manages email addresses for customers
 *
 * Supports multiple email addresses per customer (personal, work, travel agent, etc.)
 */
@Entity
@Table(name = "customer_emails",
    indexes = {
        @Index(name = "idx_customer_email_customer_id", columnList = "customer_id"),
        @Index(name = "idx_customer_email_type", columnList = "email_type"),
        @Index(name = "idx_customer_email_is_primary", columnList = "is_primary")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_type", nullable = false, length = 50)
    @Builder.Default
    private EmailType emailType = EmailType.PERSONAL;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false; // Primary contact email

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(length = 100)
    private String label; // Optional label (e.g., "Personal Assistant", "Spouse")

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Email types for customers
     */
    public enum EmailType {
        PERSONAL("Personal", "Personal email address"),
        WORK("Work", "Work/business email"),
        TRAVEL_AGENT("Travel Agent", "Travel agent contact"),
        SPOUSE("Spouse", "Spouse/partner email"),
        ASSISTANT("Assistant", "Secretary/assistant email"),
        BILLING("Billing", "Billing and invoicing"),
        EMERGENCY("Emergency", "Emergency contact email"),
        OTHER("Other", "Other email type");

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
