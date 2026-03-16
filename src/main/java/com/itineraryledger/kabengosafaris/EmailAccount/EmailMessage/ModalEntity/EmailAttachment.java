package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "email_attachments", indexes = {
    @Index(name = "idx_email_attach_message_id", columnList = "email_message_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_message_id", nullable = false)
    private EmailMessage emailMessage;

    /**
     * Storage filename (generated)
     */
    @Column(nullable = false)
    private String fileName;

    /**
     * Original filename from the email
     */
    private String originalFileName;

    private String mimeType;

    private Long fileSize;

    /**
     * Relative path to the attachment file on disk
     */
    private String storagePath;

    /**
     * Content-ID for inline images (CID reference)
     */
    private String contentId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isInline = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.isInline == null) this.isInline = false;
    }
}
