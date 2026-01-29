package com.itineraryledger.kabengosafaris.Customer.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * CustomerDocument Entity - Manages reference documents for customers
 *
 * Stores various document files related to customers:
 * - Passport scans
 * - Visa copies
 * - Travel insurance documents
 * - Corporate registration documents
 * - Signed contracts/agreements
 * - ID documents
 * - Medical certificates
 */
@Entity
@Table(name = "customer_documents", indexes = {
    @Index(name = "idx_customer_document_customer_id", columnList = "customer_id"),
    @Index(name = "idx_customer_document_type", columnList = "document_type"),
    @Index(name = "idx_customer_document_is_active", columnList = "is_active"),
    @Index(name = "idx_customer_document_valid_from", columnList = "valid_from"),
    @Index(name = "idx_customer_document_valid_to", columnList = "valid_to")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "document_number", length = 100)
    private String documentNumber;

    @Column(name = "version", length = 50)
    private String version;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Customer-specific document type enumeration
     */
    public enum DocumentType {
        PASSPORT("Passport", "Passport scan or copy"),
        VISA("Visa", "Visa copy or travel authorization"),
        ID_CARD("ID Card", "National ID or identity card"),
        DRIVERS_LICENSE("Driver's License", "Driving license"),
        INSURANCE("Insurance", "Travel or medical insurance document"),
        VACCINATION("Vaccination", "Vaccination certificate (e.g., Yellow Fever)"),
        MEDICAL("Medical Certificate", "Medical certificates or health documents"),
        PRESCRIPTION("Prescription", "Medical prescriptions"),
        CONTRACT("Contract", "Signed booking contract or agreement"),
        INVOICE("Invoice", "Invoice or receipt"),
        CORPORATE_REG("Corporate Registration", "Company registration documents"),
        TAX_ID("Tax ID", "Tax identification documents"),
        POWER_OF_ATTORNEY("Power of Attorney", "Authorization documents"),
        CONSENT("Consent Form", "Consent or waiver forms"),
        EMERGENCY_CONTACT("Emergency Contact", "Emergency contact information"),
        FLIGHT_TICKET("Flight Ticket", "Flight booking or ticket"),
        HOTEL_VOUCHER("Hotel Voucher", "Hotel or accommodation voucher"),
        ITINERARY("Itinerary", "Travel itinerary"),
        REFERENCE("Reference Letter", "Reference or recommendation letter"),
        PHOTO("Photo", "Customer photo or headshot"),
        OTHER("Other", "Other document type");

        private final String displayName;
        private final String description;

        DocumentType(String displayName, String description) {
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

    /**
     * Check if document is currently valid
     */
    public boolean isCurrentlyValid() {
        LocalDateTime now = LocalDateTime.now();

        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }

        if (validTo != null && now.isAfter(validTo)) {
            return false;
        }

        return isActive;
    }

    /**
     * Check if document is valid for a specific date
     */
    public boolean isValidForDate(LocalDateTime date) {
        if (validFrom != null && date.isBefore(validFrom)) {
            return false;
        }

        if (validTo != null && date.isAfter(validTo)) {
            return false;
        }

        return isActive;
    }

    /**
     * Get file extension from file name
     */
    public String getFileExtension() {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Check if document is an identity document
     */
    public boolean isIdentityDocument() {
        return documentType == DocumentType.PASSPORT ||
               documentType == DocumentType.ID_CARD ||
               documentType == DocumentType.DRIVERS_LICENSE;
    }

    /**
     * Check if document is expiring soon (within 6 months)
     */
    public boolean isExpiringSoon() {
        if (validTo == null) {
            return false;
        }
        LocalDateTime sixMonthsFromNow = LocalDateTime.now().plusMonths(6);
        return validTo.isBefore(sixMonthsFromNow);
    }
}
