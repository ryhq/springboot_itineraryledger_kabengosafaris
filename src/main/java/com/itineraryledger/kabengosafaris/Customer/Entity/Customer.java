package com.itineraryledger.kabengosafaris.Customer.Entity;

import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerSource;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer Entity - Centralized customer/client management
 *
 * Every safari booking needs a customer reference. This entity manages:
 * - Individual travelers
 * - Corporate clients
 * - Travel agents/partners
 *
 * Supports tracking of customer lifetime value, preferences, and communication history.
 */
@Entity
@Table(name = "customers",
    indexes = {
        @Index(name = "idx_customer_code", columnList = "code"),
        @Index(name = "idx_customer_type", columnList = "customer_type"),
        @Index(name = "idx_customer_company_name", columnList = "company_name"),
        @Index(name = "idx_customer_first_name", columnList = "first_name"),
        @Index(name = "idx_customer_last_name", columnList = "last_name"),
        @Index(name = "idx_customer_nationality", columnList = "nationality"),
        @Index(name = "idx_customer_is_active", columnList = "is_active"),
        @Index(name = "idx_customer_is_vip", columnList = "is_vip"),
        @Index(name = "idx_customer_is_blacklisted", columnList = "is_blacklisted"),
        @Index(name = "idx_customer_source", columnList = "source")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_customer_code", columnNames = {"code"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================
    // IDENTIFICATION
    // ========================

    /**
     * Auto-generated unique code (e.g., CUS-000101)
     * Format: CUS-{6-digit padded ID + 100}
     */
    @NotBlank(message = "Customer code is required")
    @Column(length = 50, unique = true, nullable = false)
    private String code;

    @NotNull(message = "Customer type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false, length = 20)
    private CustomerType customerType;

    // ========================
    // PERSONAL INFORMATION
    // ========================

    @Column(length = 10)
    private String title; // Mr, Mrs, Ms, Dr, etc.

    @Column(name = "first_name", length = 100)
    private String firstName; // Required for INDIVIDUAL

    @Column(name = "last_name", length = 100)
    private String lastName; // Required for INDIVIDUAL

    @Column(name = "company_name", length = 200)
    private String companyName; // Required for CORPORATE/TRAVEL_AGENT

    // ========================
    // IDENTITY DOCUMENTS
    // ========================

    @Column(length = 100)
    private String nationality; // Country of citizenship

    @Column(length = 100)
    private String residency; // Country of residence

    @Column(name = "passport_number", length = 50)
    private String passportNumber;

    @Column(name = "passport_expiry")
    private LocalDate passportExpiry;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth; // For age-based pricing

    // ========================
    // ADDRESS
    // ========================

    @Column(columnDefinition = "TEXT")
    private String address; // Street address

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state; // State/Province

    @Column(length = 100)
    private String country;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    // ========================
    // PREFERENCES
    // ========================

    @Column(name = "preferred_language", length = 10)
    @Builder.Default
    private String preferredLanguage = "en"; // Communication language code

    @Column(name = "preferred_currency", length = 10)
    @Builder.Default
    private String preferredCurrency = "USD"; // Billing currency

    // ========================
    // ACQUISITION & SOURCE
    // ========================

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CustomerSource source; // How customer found us

    @Column(name = "referred_by", length = 200)
    private String referredBy; // Referral source name

    // ========================
    // SPECIAL REQUIREMENTS
    // ========================

    @Lob
    @Column(name = "dietary_requirements", columnDefinition = "TEXT")
    private String dietaryRequirements;

    @Lob
    @Column(name = "medical_conditions", columnDefinition = "TEXT")
    private String medicalConditions;

    @Lob
    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    /**
     * Safari interests stored as JSON array
     * e.g., ["wildlife", "photography", "birding", "walking safaris"]
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String interests;

    // ========================
    // INTERNAL MANAGEMENT
    // ========================

    @Lob
    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes; // Staff-only notes

    @Builder.Default
    @Column(name = "is_vip", nullable = false)
    private Boolean isVip = false; // VIP customer flag

    @Builder.Default
    @Column(name = "is_blacklisted", nullable = false)
    private Boolean isBlacklisted = false; // Problem customer flag

    @Lob
    @Column(name = "blacklist_reason", columnDefinition = "TEXT")
    private String blacklistReason; // Reason if blacklisted

    // ========================
    // COMPUTED STATISTICS
    // ========================

    @Builder.Default
    @Column(name = "total_bookings")
    private Integer totalBookings = 0; // Number of safaris

    @Builder.Default
    @Column(name = "total_spent", precision = 15, scale = 2)
    private BigDecimal totalSpent = BigDecimal.ZERO; // Lifetime value

    @Column(name = "last_booking_date")
    private LocalDateTime lastBookingDate; // Most recent booking

    // ========================
    // STATUS & METADATA
    // ========================

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_by")
    private Long createdBy; // User who created

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ========================
    // RELATIONSHIPS
    // ========================

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CustomerEmail> emails = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CustomerPhone> phones = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CustomerDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<CustomerNote> notes = new ArrayList<>();

    // ========================
    // HELPER METHODS
    // ========================

    public void addEmail(CustomerEmail email) {
        emails.add(email);
        email.setCustomer(this);
    }

    public void removeEmail(CustomerEmail email) {
        emails.remove(email);
        email.setCustomer(null);
    }

    public void addPhone(CustomerPhone phone) {
        phones.add(phone);
        phone.setCustomer(this);
    }

    public void removePhone(CustomerPhone phone) {
        phones.remove(phone);
        phone.setCustomer(null);
    }

    public void addDocument(CustomerDocument document) {
        documents.add(document);
        document.setCustomer(this);
    }

    public void removeDocument(CustomerDocument document) {
        documents.remove(document);
        document.setCustomer(null);
    }

    public void addNote(CustomerNote note) {
        notes.add(note);
        note.setCustomer(this);
    }

    public void removeNote(CustomerNote note) {
        notes.remove(note);
        note.setCustomer(null);
    }

    /**
     * Get the display name based on customer type
     */
    @Transient
    public String getDisplayName() {
        if (customerType == CustomerType.INDIVIDUAL) {
            StringBuilder sb = new StringBuilder();
            if (title != null && !title.isBlank()) {
                sb.append(title).append(" ");
            }
            if (firstName != null) {
                sb.append(firstName);
            }
            if (lastName != null) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(lastName);
            }
            return sb.toString().trim();
        } else {
            return companyName != null ? companyName : "";
        }
    }

    /**
     * Check if customer can book safaris
     */
    @Transient
    public boolean canBook() {
        return isActive && !isBlacklisted;
    }

    /**
     * Increment booking count and update last booking date
     */
    public void recordBooking(BigDecimal amount) {
        this.totalBookings = (this.totalBookings == null ? 0 : this.totalBookings) + 1;
        this.totalSpent = (this.totalSpent == null ? BigDecimal.ZERO : this.totalSpent).add(amount);
        this.lastBookingDate = LocalDateTime.now();
    }

    /**
     * Generate customer code based on ID
     * Format: CUS-{6-digit padded (ID + 100)}
     * Example: ID=1 -> CUS-000101, ID=999 -> CUS-001099, ID=1000 -> CUS-001100
     */
    @Transient
    public String generateCode() {
        if (id == null) {
            return null;
        }
        return String.format("CUS-%06d", id + 100);
    }

    /**
     * Get the primary email address from the emails list
     */
    @Transient
    public String getPrimaryEmail() {
        if (emails == null || emails.isEmpty()) {
            return null;
        }
        return emails.stream()
            .filter(e -> Boolean.TRUE.equals(e.getIsPrimary()))
            .map(CustomerEmail::getEmail)
            .findFirst()
            .orElse(emails.get(0).getEmail());
    }

    /**
     * Get the primary phone number from the phones list
     */
    @Transient
    public String getPrimaryPhone() {
        if (phones == null || phones.isEmpty()) {
            return null;
        }
        return phones.stream()
            .filter(p -> Boolean.TRUE.equals(p.getIsPrimary()))
            .map(CustomerPhone::getPhoneNumber)
            .findFirst()
            .orElse(phones.get(0).getPhoneNumber());
    }
}
