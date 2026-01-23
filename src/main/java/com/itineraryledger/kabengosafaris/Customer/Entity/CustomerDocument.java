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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * CustomerDocument Entity - Document storage for customers
 *
 * Stores references to customer documents such as:
 * - Passport scans
 * - Visa copies
 * - Travel insurance documents
 * - Corporate registration documents
 * - Signed contracts/agreements
 */
@Entity
@Table(name = "customer_documents",
    indexes = {
        @Index(name = "idx_customer_doc_customer_id", columnList = "customer_id"),
        @Index(name = "idx_customer_doc_type", columnList = "document_type"),
        @Index(name = "idx_customer_doc_expiry", columnList = "expiry_date")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    // ========================
    // DOCUMENT DETAILS
    // ========================

    @NotBlank(message = "Document name is required")
    @Column(nullable = false, length = 200)
    private String name; // Display name

    @NotBlank(message = "Document type is required")
    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType; // PASSPORT, VISA, INSURANCE, CONTRACT, OTHER

    @Column(name = "document_number", length = 100)
    private String documentNumber; // Passport number, visa number, etc.

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    // ========================
    // FILE INFORMATION
    // ========================

    @NotBlank(message = "File URL is required")
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl; // URL or path to the stored file

    @Column(name = "file_name", length = 255)
    private String fileName; // Original file name

    @Column(name = "file_size")
    private Long fileSize; // Size in bytes

    @Column(name = "mime_type", length = 100)
    private String mimeType; // application/pdf, image/jpeg, etc.

    // ========================
    // VALIDITY
    // ========================

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "issuing_authority", length = 200)
    private String issuingAuthority; // e.g., "Tanzania Immigration"

    // ========================
    // STATUS
    // ========================

    @Builder.Default
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false; // Document verified by staff

    @Column(name = "verified_by")
    private Long verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // ========================
    // METADATA
    // ========================

    @Column(name = "uploaded_by")
    private Long uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ========================
    // HELPER METHODS
    // ========================

    /**
     * Check if document has expired
     */
    @Transient
    public boolean isExpired() {
        if (expiryDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(expiryDate);
    }

    /**
     * Check if document is expiring within specified days
     */
    @Transient
    public boolean isExpiringSoon(int days) {
        if (expiryDate == null) {
            return false;
        }
        LocalDate warningDate = LocalDate.now().plusDays(days);
        return !expiryDate.isAfter(warningDate);
    }
}
