package com.itineraryledger.kabengosafaris.Legal.Entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A privacy notice or terms, as a versioned record rather than a page in a website.
 *
 * <p>Three things made this a record instead of markup. It has to be readable from the client
 * portal and from the public site, and there are two public sites which are copies of each other,
 * so hand-written markup is two edits and a deploy every time a sentence changes. It has to be
 * editable by the office rather than by a developer. And above all it has to be <strong>versioned
 * </strong>: somebody consented to a particular text, and "the policy as it stands today" is not
 * an answer to the question of what they agreed to.
 *
 * <p>So a participant's accepted version is stamped on them at the moment they first verify, and
 * the text they saw stays retrievable even after the notice has moved on.
 *
 * <p>One row per type per version, and exactly one of them published at a time.
 */
@Entity
@Table(
    name = "legal_documents",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_legal_document_version", columnNames = {"document_type", "version"}),
    indexes = @Index(name = "idx_legal_document_published", columnList = "document_type, published"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    private LegalDocumentType documentType;

    /** Whatever the office calls it: "2026-10", "v3". Unique within the type, and immutable. */
    @Column(name = "version", nullable = false, length = 30)
    private String version;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * The notice itself, as text rather than markup.
     *
     * <p>There is no HTML sanitiser on this side of the application, and this text is edited in
     * the panel and rendered on two public websites and inside the portal. Accepting markup would
     * put a stored-script hole on every one of those pages, one careless paste away.
     *
     * <p>So it is written in a small plain markup, rendered by
     * {@code LegalDocumentService.renderHtml}: a blank line starts a paragraph, {@code ## } a
     * heading, {@code - } a bullet, and every other character is escaped. Whatever is typed here,
     * what reaches a page is text.
     */
    @Column(name = "body", nullable = false, columnDefinition = "LONGTEXT")
    private String body;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /** Exactly one per type. Publishing one unpublishes the rest. */
    @Column(name = "published", nullable = false)
    @Builder.Default
    private Boolean published = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (published == null) published = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
