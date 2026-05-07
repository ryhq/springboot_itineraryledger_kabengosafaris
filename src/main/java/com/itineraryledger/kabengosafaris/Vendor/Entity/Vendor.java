package com.itineraryledger.kabengosafaris.Vendor.Entity;

import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.Vendor.Enums.VendorType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Vendor — an external party we PAY (lodges, fuel stations, mechanics, park
 * authorities, transporters, landlords, insurers, etc).
 *
 * Lighter than Customer by design: no documents / contacts collections, no
 * VIP/blacklist flags, no statistics. The sole job is to identify a payee
 * and carry the contact + payment-detail fields needed for the Expense module.
 */
@Entity
@Table(name = "vendors",
    indexes = {
        @Index(name = "idx_vendor_code", columnList = "code"),
        @Index(name = "idx_vendor_type", columnList = "type"),
        @Index(name = "idx_vendor_name", columnList = "name"),
        @Index(name = "idx_vendor_is_active", columnList = "is_active")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_vendor_code", columnNames = {"code"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Auto-generated unique code (e.g. VND-000001).
     * Format: VND-{6-digit padded id}.
     */
    @NotBlank(message = "Vendor code is required")
    @Column(length = 50, unique = true, nullable = false)
    private String code;

    @NotBlank(message = "Vendor name is required")
    @Column(nullable = false, length = 200)
    private String name;

    @NotNull(message = "Vendor type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VendorType type;

    @Column(name = "contact_person", length = 200)
    private String contactPerson;

    @Column(length = 200)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String country;

    /**
     * ISO 4217 currency the vendor prefers to be paid in. Defaults to TZS but
     * the company can pay in any currency at expense-payment time.
     */
    @Column(name = "preferred_currency", length = 3)
    @Builder.Default
    private String preferredCurrency = "TZS";

    @Column(name = "payment_terms", length = 200)
    private String paymentTerms;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // ========================
    // AUDIT
    // ========================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private User updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    public String generateCode() {
        if (id == null) return null;
        return String.format("VND-%06d", id);
    }
}
