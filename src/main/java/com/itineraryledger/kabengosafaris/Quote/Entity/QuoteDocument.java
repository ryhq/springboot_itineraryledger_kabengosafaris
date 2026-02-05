package com.itineraryledger.kabengosafaris.Quote.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * QuoteDocument Entity - Manages reference documents for quotes
 *
 * Stores various document files related to quotes:
 * - Quote PDF/proposals
 * - Terms and conditions
 * - Itinerary details
 * - Payment schedules
 * - Contracts and agreements
 * - Supporting documents
 */
@Entity
@Table(name = "quote_documents", indexes = {
    @Index(name = "idx_quote_document_quote_id", columnList = "quote_id"),
    @Index(name = "idx_quote_document_type", columnList = "document_type"),
    @Index(name = "idx_quote_document_is_active", columnList = "is_active"),
    @Index(name = "idx_quote_document_valid_from", columnList = "valid_from"),
    @Index(name = "idx_quote_document_valid_to", columnList = "valid_to"),
    @Index(name = "idx_quote_document_is_generated", columnList = "is_generated")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

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

    @Column(name = "version", length = 50)
    private String version;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_generated", nullable = false)
    private Boolean isGenerated = false;

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
     * Quote-specific document type enumeration
     */
    public enum DocumentType {
        QUOTE_PDF("Quote PDF", "Main quote document in PDF format"),
        PROPOSAL("Proposal", "Detailed travel proposal"),
        ITINERARY("Itinerary", "Detailed itinerary document"),
        TERMS_AND_CONDITIONS("Terms & Conditions", "Terms and conditions document"),
        PAYMENT_SCHEDULE("Payment Schedule", "Payment terms and schedule"),
        CONTRACT("Contract", "Service contract or agreement"),
        INVOICE("Invoice", "Invoice document"),
        RECEIPT("Receipt", "Payment receipt"),
        CONFIRMATION("Confirmation", "Booking confirmation"),
        VOUCHER("Voucher", "Travel voucher"),
        INSURANCE("Insurance", "Travel insurance documents"),
        VISA_SUPPORT("Visa Support", "Visa application support letter"),
        FLIGHT_DETAILS("Flight Details", "Flight itinerary and tickets"),
        ACCOMMODATION_DETAILS("Accommodation Details", "Hotel/lodge confirmation"),
        ACTIVITY_DETAILS("Activity Details", "Activity booking confirmations"),
        TRANSPORT_DETAILS("Transport Details", "Vehicle rental or transfer details"),
        HEALTH_REQUIREMENTS("Health Requirements", "Vaccination and health information"),
        PACKING_LIST("Packing List", "Recommended packing list"),
        TRAVEL_GUIDE("Travel Guide", "Destination travel guide"),
        MAP("Map", "Route maps and location details"),
        EMERGENCY_CONTACTS("Emergency Contacts", "Emergency contact information"),
        CORRESPONDENCE("Correspondence", "Email or letter correspondence"),
        AMENDMENT("Amendment", "Quote amendment or revision"),
        CANCELLATION("Cancellation", "Cancellation document"),
        REFUND("Refund", "Refund documentation"),
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
     * Check if document is a primary quote document
     */
    public boolean isPrimaryQuoteDocument() {
        return documentType == DocumentType.QUOTE_PDF ||
               documentType == DocumentType.PROPOSAL ||
               documentType == DocumentType.CONTRACT;
    }

    /**
     * Check if document is payment-related
     */
    public boolean isPaymentDocument() {
        return documentType == DocumentType.INVOICE ||
               documentType == DocumentType.RECEIPT ||
               documentType == DocumentType.PAYMENT_SCHEDULE ||
               documentType == DocumentType.REFUND;
    }

    /**
     * Check if document is booking-related
     */
    public boolean isBookingDocument() {
        return documentType == DocumentType.CONFIRMATION ||
               documentType == DocumentType.VOUCHER ||
               documentType == DocumentType.ITINERARY;
    }
}
