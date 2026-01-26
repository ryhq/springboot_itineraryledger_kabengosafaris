package com.itineraryledger.kabengosafaris.Park.Entities;

import com.itineraryledger.kabengosafaris.Park.Park;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ParkDocument Entity - Manages reference documents for parks
 *
 * Stores various document files related to parks:
 * - Tariff documents and fee schedules
 * - Park regulations and rules
 * - Maps and guides
 * - Permits and licenses
 * - Conservation reports
 * - Brochures and marketing materials
 */
@Entity
@Table(name = "park_documents", indexes = {
    @Index(name = "idx_park_document_park_id", columnList = "park_id"),
    @Index(name = "idx_park_document_type", columnList = "document_type"),
    @Index(name = "idx_park_document_is_active", columnList = "is_active"),
    @Index(name = "idx_park_document_valid_from", columnList = "valid_from"),
    @Index(name = "idx_park_document_valid_to", columnList = "valid_to")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "park_id", nullable = false)
    private Park park;

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
     * Park-specific document type enumeration
     */
    public enum DocumentType {
        TARIFF("Tariff Document", "Park entry fees and tariff schedule"),
        FEE_SCHEDULE("Fee Schedule", "Detailed fee breakdown"),
        REGULATION("Regulation", "Park rules and regulations"),
        PERMIT("Permit", "Permits and authorization documents"),
        MAP("Map", "Park maps and trail guides"),
        BROCHURE("Brochure", "Marketing brochure or information leaflet"),
        GUIDE("Guide", "Visitor guide or handbook"),
        CONSERVATION("Conservation Report", "Conservation and wildlife reports"),
        RESEARCH("Research", "Research papers and studies"),
        SAFETY("Safety Document", "Safety guidelines and procedures"),
        EMERGENCY("Emergency", "Emergency procedures and contacts"),
        WILDLIFE_LIST("Wildlife List", "Species checklist"),
        BIRD_LIST("Bird List", "Bird species checklist"),
        PLANT_LIST("Plant List", "Flora and vegetation list"),
        CALENDAR("Calendar", "Events and activity calendar"),
        NEWSLETTER("Newsletter", "Park newsletter or updates"),
        ANNUAL_REPORT("Annual Report", "Annual park report"),
        MANAGEMENT_PLAN("Management Plan", "Park management plan"),
        POLICY("Policy", "Park policies"),
        AGREEMENT("Agreement", "Partnership or service agreements"),
        PRESENTATION("Presentation", "Information presentation"),
        HISTORICAL("Historical Document", "Historical records and documents"),
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
     * Check if document is a tariff/fee document
     */
    public boolean isTariffDocument() {
        return documentType == DocumentType.TARIFF || documentType == DocumentType.FEE_SCHEDULE;
    }
}
