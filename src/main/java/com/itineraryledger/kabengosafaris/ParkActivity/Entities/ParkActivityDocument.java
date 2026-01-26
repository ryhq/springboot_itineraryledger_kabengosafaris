package com.itineraryledger.kabengosafaris.ParkActivity.Entities;

import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ParkActivityDocument Entity - Manages reference documents for park-activity relationships
 *
 * Stores various document files related to activities within specific parks:
 * - Park-specific safety guidelines for the activity
 * - Location-specific waivers and forms
 * - Park rules and regulations for the activity
 * - Meeting point instructions
 *
 * The ParkActivity relationship MUST exist for a document to be created.
 */
@Entity
@Table(name = "park_activity_documents", indexes = {
    @Index(name = "idx_park_activity_document_park_id", columnList = "park_id"),
    @Index(name = "idx_park_activity_document_activity_id", columnList = "activity_id"),
    @Index(name = "idx_park_activity_document_type", columnList = "document_type"),
    @Index(name = "idx_park_activity_document_is_active", columnList = "is_active"),
    @Index(name = "idx_park_activity_document_valid_from", columnList = "valid_from"),
    @Index(name = "idx_park_activity_document_valid_to", columnList = "valid_to")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkActivityDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the ParkActivity relationship.
     * Both park_id and activity_id are required as a composite foreign key.
     * The ParkActivity must exist for this document to exist.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "park_id", referencedColumnName = "park_id", nullable = false),
        @JoinColumn(name = "activity_id", referencedColumnName = "activity_id", nullable = false)
    })
    private ParkActivity parkActivity;

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
     * Park-Activity specific document type enumeration
     * Documents specific to activities performed at particular parks
     */
    public enum DocumentType {
        SAFETY_GUIDELINES("Safety Guidelines", "Park-specific safety instructions for this activity"),
        WAIVER("Waiver", "Liability waiver specific to this park activity"),
        LIABILITY_FORM("Liability Form", "Liability forms for this park activity"),
        PARK_RULES("Park Rules", "Park rules specific to this activity"),
        EQUIPMENT_CHECKLIST("Equipment Checklist", "Equipment requirements for this park"),
        TRAINING_MATERIAL("Training Material", "Training materials for this park location"),
        CERTIFICATION("Certification", "Certifications specific to this park"),
        PERMIT("Permit", "Activity permits for this specific park"),
        INSURANCE("Insurance", "Insurance documentation for this park activity"),
        MEDICAL_FORM("Medical Form", "Medical forms required by this park"),
        EMERGENCY_PROCEDURE("Emergency Procedure", "Emergency procedures at this park"),
        BRIEFING("Briefing Document", "Pre-activity briefing for this park"),
        ITINERARY("Itinerary", "Activity schedule at this park"),
        MAP("Map", "Maps and routes within this park"),
        MEETING_POINT("Meeting Point", "Meeting point instructions and directions"),
        BROCHURE("Brochure", "Activity brochure for this park"),
        PRICE_LIST("Price List", "Pricing specific to this park activity"),
        TERMS_CONDITIONS("Terms & Conditions", "Terms and conditions for this park"),
        FAQ("FAQ", "Frequently asked questions for this park activity"),
        GUIDE("Guide", "Activity guide specific to this park"),
        WILDLIFE_CHECKLIST("Wildlife Checklist", "Expected wildlife during activity"),
        SEASONAL_INFO("Seasonal Information", "Seasonal activity information"),
        INCIDENT_REPORT("Incident Report", "Incident reports for this park activity"),
        POLICY("Policy", "Policies for this park activity"),
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
