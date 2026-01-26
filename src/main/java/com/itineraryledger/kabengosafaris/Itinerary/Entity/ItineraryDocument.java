package com.itineraryledger.kabengosafaris.Itinerary.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ItineraryDocument Entity - Manages generated and uploaded documents for itineraries
 *
 * Stores various document files related to itineraries:
 * - Generated PDFs (quotations, travel plans, final itineraries)
 * - Booking confirmations and invoices
 * - Visa support letters
 * - Custom documents
 */
@Entity
@Table(name = "itinerary_documents", indexes = {
    @Index(name = "idx_itinerary_document_itinerary_id", columnList = "itinerary_id"),
    @Index(name = "idx_itinerary_document_type", columnList = "document_type"),
    @Index(name = "idx_itinerary_document_is_active", columnList = "is_active"),
    @Index(name = "idx_itinerary_document_valid_from", columnList = "valid_from"),
    @Index(name = "idx_itinerary_document_valid_to", columnList = "valid_to"),
    @Index(name = "idx_itinerary_document_is_generated", columnList = "is_generated")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;

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
     * Itinerary-specific document type enumeration
     */
    public enum DocumentType {
        QUOTATION("Quotation", "Price quotation for the safari itinerary"),
        TRAVEL_PLAN("Travel Plan", "Detailed travel plan document"),
        FINAL_ITINERARY("Final Itinerary", "Confirmed final itinerary for the trip"),
        BOOKING_CONFIRMATION("Booking Confirmation", "Booking confirmation document"),
        INVOICE("Invoice", "Invoice for the safari package"),
        PROFORMA_INVOICE("Proforma Invoice", "Proforma invoice for prepayment"),
        RECEIPT("Receipt", "Payment receipt"),
        VISA_SUPPORT_LETTER("Visa Support Letter", "Support letter for visa application"),
        FLIGHT_ITINERARY("Flight Itinerary", "Flight schedule and details"),
        ACCOMMODATION_VOUCHER("Accommodation Voucher", "Hotel/lodge confirmation vouchers"),
        ACTIVITY_VOUCHER("Activity Voucher", "Activity booking vouchers"),
        PARK_PERMITS("Park Permits", "National park entry permits"),
        TRANSFER_VOUCHER("Transfer Voucher", "Airport/ground transfer confirmation"),
        TRAVEL_INSURANCE("Travel Insurance", "Travel insurance documents"),
        EMERGENCY_CONTACTS("Emergency Contacts", "Emergency contact information sheet"),
        PACKING_LIST("Packing List", "Recommended packing list"),
        TERMS_CONDITIONS("Terms & Conditions", "Booking terms and conditions"),
        CANCELLATION_POLICY("Cancellation Policy", "Cancellation policy document"),
        HEALTH_REQUIREMENTS("Health Requirements", "Vaccination and health requirements"),
        VISA_REQUIREMENTS("Visa Requirements", "Visa requirements information"),
        SAFARI_GUIDELINES("Safari Guidelines", "Safari conduct and safety guidelines"),
        CUSTOM("Custom Document", "Custom or miscellaneous document"),
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
     * Get file extension from file name
     */
    public String getFileExtension() {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
