package com.itineraryledger.kabengosafaris.PdfDocument.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing PDF templates for specific PDF document types.
 * Each document type must have at least one system default template.
 * System default templates can be restored to their original state but cannot be deleted.
 *
 * Note: Variables/schema are defined at the PdfDocument level.
 * Templates use Thymeleaf syntax to access the DTO fields.
 */
@Entity
@Table(name = "pdf_templates",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"pdf_document_id", "name"}),
        @UniqueConstraint(columnNames = {"pdf_document_id", "file_name"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The PDF document type this template belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pdf_document_id", nullable = false)
    private PdfDocument pdfDocument;

    /**
     * Name of the template (e.g., "Luxury Safari Template", "Budget Itinerary")
     * Must be unique within the same document type
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Description of the template
     */
    @Column(length = 500)
    private String description;

    /**
     * Name of the HTML template file stored on disk
     * Format: {documentName}_{templateName}_{timestamp}.html
     */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /**
     * Paper size for PDF generation
     * A4, LETTER, LEGAL, A3
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "paper_size", nullable = false, length = 20)
    @Builder.Default
    private PaperSize paperSize = PaperSize.A4;

    /**
     * Page orientation
     * PORTRAIT or LANDSCAPE
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Orientation orientation = Orientation.PORTRAIT;

    /**
     * Top margin in millimeters
     */
    @Column(name = "margin_top")
    @Builder.Default
    private Integer marginTop = 20;

    /**
     * Bottom margin in millimeters
     */
    @Column(name = "margin_bottom")
    @Builder.Default
    private Integer marginBottom = 20;

    /**
     * Left margin in millimeters
     */
    @Column(name = "margin_left")
    @Builder.Default
    private Integer marginLeft = 15;

    /**
     * Right margin in millimeters
     */
    @Column(name = "margin_right")
    @Builder.Default
    private Integer marginRight = 15;

    /**
     * Whether this template is the default template for the document type
     * Only one template per document type can be marked as default
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * Whether this is a system-generated default template
     * System default templates cannot be deleted, only restored or modified
     */
    @Column(name = "is_system_default", nullable = false)
    @Builder.Default
    private Boolean isSystemDefault = false;

    /**
     * Whether this template is enabled for use
     * Disabled templates will not be used even if marked as default
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /**
     * Size of the template file in bytes
     */
    @Column(name = "file_size", nullable = false)
    @Builder.Default
    private Long fileSize = 0L;

    /**
     * Template version for tracking changes
     */
    @Column(length = 50)
    private String version;

    /**
     * Timestamp when the template was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the template was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Initialize default values before persisting
     */
    @PrePersist
    protected void onCreate() {
        if (this.isDefault == null) {
            this.isDefault = false;
        }
        if (this.isSystemDefault == null) {
            this.isSystemDefault = false;
        }
        if (this.enabled == null) {
            this.enabled = true;
        }
        if (this.fileSize == null) {
            this.fileSize = 0L;
        }
        if (this.paperSize == null) {
            this.paperSize = PaperSize.A4;
        }
        if (this.orientation == null) {
            this.orientation = Orientation.PORTRAIT;
        }
        if (this.marginTop == null) {
            this.marginTop = 20;
        }
        if (this.marginBottom == null) {
            this.marginBottom = 20;
        }
        if (this.marginLeft == null) {
            this.marginLeft = 15;
        }
        if (this.marginRight == null) {
            this.marginRight = 15;
        }
    }
}
