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
 * AccommodationDocument Entity - Manages reference documents for accommodations
 *
 * Stores various document files related to accommodations:
 * - STO Rate Documents (Special Tour Operator pricing sheets)
 * - Rack Rate Documents (Public pricing sheets)
 * - Contracts and Agreements
 * - Licenses and Certifications
 * - Brochures and Marketing Materials
 * - Policies and Terms
 * - Floor Plans and Maps
 * - Other reference documents
 */
@Entity
@Table(name = "accommodation_documents", indexes = {
    @Index(name = "idx_accommodation_document_accommodation_id", columnList = "accommodation_id"),
    @Index(name = "idx_accommodation_document_type", columnList = "document_type"),
    @Index(name = "idx_accommodation_document_is_active", columnList = "is_active"),
    @Index(name = "idx_accommodation_document_valid_from", columnList = "valid_from"),
    @Index(name = "idx_accommodation_document_valid_to", columnList = "valid_to")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccommodationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    @Column(nullable = false, length = 200)
    private String title; // Document title/name

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl; // URL or path to the document file

    @Column(name = "file_name", length = 255)
    private String fileName; // Original file name

    @Column(name = "file_size")
    private Long fileSize; // File size in bytes

    @Column(name = "file_type", length = 100)
    private String fileType; // MIME type (e.g., "application/pdf", "image/jpeg")

    @Column(columnDefinition = "TEXT")
    private String description; // Document description/notes

    @Column(name = "version", length = 50)
    private String version; // Document version (e.g., "2024-Q1", "v2.0")

    @Column(name = "valid_from")
    private LocalDateTime validFrom; // When this document becomes valid

    @Column(name = "valid_to")
    private LocalDateTime validTo; // When this document expires

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes; // Internal notes

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Document type enumeration
     */
    public enum DocumentType {
        STO_RATE("STO Rate Document", "Special Tour Operator pricing sheet"),
        RACK_RATE("Rack Rate Document", "Public/published pricing sheet"),
        CONTRACT("Contract", "Service agreement or contract"),
        LICENSE("License", "Business or tourism license"),
        CERTIFICATE("Certificate", "Certification or accreditation"),
        BROCHURE("Brochure", "Marketing brochure or flyer"),
        FLOOR_PLAN("Floor Plan", "Property floor plan or layout"),
        MENU("Menu", "Restaurant or dining menu"),
        POLICY("Policy", "Terms, conditions, or policy document"),
        INSURANCE("Insurance", "Insurance certificate or policy"),
        SAFETY("Safety Document", "Safety procedures or guidelines"),
        TAX_DOCUMENT("Tax Document", "TIN, VRN, or tax registration"),
        INVOICE("Invoice", "Invoice or billing document"),
        RECEIPT("Receipt", "Payment receipt"),
        PHOTO("Photo", "Property or facility photo"),
        VIDEO("Video", "Property or promotional video"),
        MAP("Map", "Location or area map"),
        PRESENTATION("Presentation", "Sales or marketing presentation"),
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
     * Check if document is a rate document (STO or Rack)
     */
    public boolean isRateDocument() {
        return documentType == DocumentType.STO_RATE || documentType == DocumentType.RACK_RATE;
    }
}
