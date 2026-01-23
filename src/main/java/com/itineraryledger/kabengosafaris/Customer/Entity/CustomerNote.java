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

import java.time.LocalDateTime;

/**
 * CustomerNote Entity - Communication history and notes for customers
 *
 * Tracks:
 * - Phone call notes
 * - Email summaries
 * - Meeting notes
 * - General observations
 * - Follow-up reminders
 */
@Entity
@Table(name = "customer_notes",
    indexes = {
        @Index(name = "idx_customer_note_customer_id", columnList = "customer_id"),
        @Index(name = "idx_customer_note_type", columnList = "note_type"),
        @Index(name = "idx_customer_note_created_at", columnList = "created_at"),
        @Index(name = "idx_customer_note_is_pinned", columnList = "is_pinned")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    // ========================
    // NOTE DETAILS
    // ========================

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", length = 30)
    @Builder.Default
    private NoteType noteType = NoteType.GENERAL;

    @Column(length = 200)
    private String subject; // Brief subject/title

    @NotBlank(message = "Note content is required")
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // ========================
    // FOLLOW-UP
    // ========================

    @Column(name = "follow_up_date")
    private LocalDateTime followUpDate; // When to follow up

    @Builder.Default
    @Column(name = "follow_up_completed", nullable = false)
    private Boolean followUpCompleted = false;

    @Column(name = "follow_up_completed_at")
    private LocalDateTime followUpCompletedAt;

    @Column(name = "follow_up_completed_by")
    private Long followUpCompletedBy;

    // ========================
    // VISIBILITY & PRIORITY
    // ========================

    @Builder.Default
    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned = false; // Pin to top of notes list

    @Builder.Default
    @Column(name = "is_private", nullable = false)
    private Boolean isPrivate = false; // Only visible to creator

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private NotePriority priority = NotePriority.NORMAL;

    // ========================
    // RELATED ENTITIES
    // ========================

    /**
     * Optional reference to related entity (e.g., Safari, Quotation)
     */
    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    // ========================
    // METADATA
    // ========================

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_by_name", length = 200)
    private String createdByName; // Denormalized for display

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
     * Check if follow-up is overdue
     */
    @Transient
    public boolean isFollowUpOverdue() {
        if (followUpDate == null || followUpCompleted) {
            return false;
        }
        return LocalDateTime.now().isAfter(followUpDate);
    }

    /**
     * Check if follow-up is due within specified hours
     */
    @Transient
    public boolean isFollowUpDueSoon(int hours) {
        if (followUpDate == null || followUpCompleted) {
            return false;
        }
        LocalDateTime warningTime = LocalDateTime.now().plusHours(hours);
        return !followUpDate.isAfter(warningTime);
    }

    /**
     * Note types for customer notes
     */
    public enum NoteType {
        PHONE_CALL("Phone Call", "Notes from a phone conversation"),
        EMAIL("Email", "Summary of email communication"),
        MEETING("Meeting", "Notes from an in-person or virtual meeting"),
        GENERAL("General", "General observations or notes"),
        FOLLOW_UP("Follow-up", "Follow-up reminder or action item"),
        COMPLAINT("Complaint", "Customer complaint or issue"),
        FEEDBACK("Feedback", "Customer feedback or suggestion");

        private final String displayName;
        private final String description;

        NoteType(String displayName, String description) {
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
     * Priority levels for customer notes
     */
    public enum NotePriority {
        LOW("Low", "Low priority - no immediate action required"),
        NORMAL("Normal", "Normal priority - standard handling"),
        HIGH("High", "High priority - requires prompt attention"),
        URGENT("Urgent", "Urgent - requires immediate attention");

        private final String displayName;
        private final String description;

        NotePriority(String displayName, String description) {
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
}
