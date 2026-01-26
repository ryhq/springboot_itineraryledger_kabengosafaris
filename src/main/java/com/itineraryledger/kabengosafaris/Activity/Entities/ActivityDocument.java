package com.itineraryledger.kabengosafaris.Activity.Entities;

import com.itineraryledger.kabengosafaris.Activity.Activity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ActivityDocument Entity - Manages reference documents for activities
 *
 * Stores various document files related to activities:
 * - Safety guidelines and procedures
 * - Waivers and liability forms
 * - Equipment checklists
 * - Training materials
 * - Certifications and permits
 */
@Entity
@Table(name = "activity_documents", indexes = {
    @Index(name = "idx_activity_document_activity_id", columnList = "activity_id"),
    @Index(name = "idx_activity_document_type", columnList = "document_type"),
    @Index(name = "idx_activity_document_is_active", columnList = "is_active"),
    @Index(name = "idx_activity_document_valid_from", columnList = "valid_from"),
    @Index(name = "idx_activity_document_valid_to", columnList = "valid_to")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

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
     * Activity-specific document type enumeration
     */
    public enum DocumentType {
        SAFETY_GUIDELINES("Safety Guidelines", "Safety instructions and guidelines"),
        WAIVER("Waiver", "Liability waiver and release forms"),
        LIABILITY_FORM("Liability Form", "Liability and indemnity forms"),
        EQUIPMENT_CHECKLIST("Equipment Checklist", "Required equipment checklist"),
        TRAINING_MATERIAL("Training Material", "Training and instructional documents"),
        CERTIFICATION("Certification", "Certifications and qualifications"),
        PERMIT("Permit", "Activity permits and licenses"),
        INSURANCE("Insurance", "Insurance documentation"),
        MEDICAL_FORM("Medical Form", "Medical information and health forms"),
        EMERGENCY_PROCEDURE("Emergency Procedure", "Emergency response procedures"),
        BRIEFING("Briefing Document", "Pre-activity briefing materials"),
        ITINERARY("Itinerary", "Activity schedule and itinerary"),
        MAP("Map", "Activity area maps and routes"),
        BROCHURE("Brochure", "Marketing brochure or information leaflet"),
        PRICE_LIST("Price List", "Pricing and tariff information"),
        TERMS_CONDITIONS("Terms & Conditions", "Terms and conditions document"),
        FAQ("FAQ", "Frequently asked questions"),
        GUIDE("Guide", "Activity guide or handbook"),
        MAINTENANCE("Maintenance Record", "Equipment maintenance records"),
        INSPECTION("Inspection Report", "Safety inspection reports"),
        INCIDENT_REPORT("Incident Report", "Incident report template or records"),
        POLICY("Policy", "Activity policies"),
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
