package com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing email templates for specific email events.
 * Each event must have at least one system default template.
 * System default templates can be restored to their original state but cannot be deleted.
 *
 * Note: Variables are now defined at the EmailEvent level, not template level.
 * Templates must use the variable names defined in their associated EmailEvent.
 */
@Entity
@Table(name = "email_templates",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"email_event_id", "name"}),
        @UniqueConstraint(columnNames = {"email_event_id", "file_name"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The email event this template belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_event_id", nullable = false)
    private EmailEvent emailEvent;

    /**
     * Name of the template (e.g., "UserRegistrationTemplate")
     * Must be unique within the same email event
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Description of the template
     */
    @Column(length = 500)
    private String description;

    /**
     * Name of the HTML template file stored on disk
     * Format: {eventName}_{templateName}_{timestamp}.html
     */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /**
     * Whether this template is the default template for the event
     * Only one template per event can be marked as default
     */
    @Column(nullable = false)
    private Boolean isDefault;

    /**
     * Whether this is a system-generated default template
     * System default templates cannot be deleted, only restored or modified
     */
    @Column(nullable = false)
    private Boolean isSystemDefault;

    /**
     * Whether this template is enabled for use
     * Disabled templates will not be used even if marked as default
     */
    @Column(nullable = false)
    private Boolean enabled;

    /**
     * Size of the template file in bytes
     */
    @Column(nullable = false)
    private Long fileSize;

    /**
     * Timestamp when the template was created
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the template was last updated
     */
    @UpdateTimestamp
    @Column(nullable = false)
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
    }
}
