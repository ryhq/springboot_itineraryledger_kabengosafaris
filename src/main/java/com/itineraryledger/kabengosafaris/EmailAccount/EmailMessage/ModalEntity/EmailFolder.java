package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "email_folders", indexes = {
    @Index(name = "idx_email_folder_account_id", columnList = "email_account_id"),
    @Index(name = "idx_email_folder_type", columnList = "type")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_email_folder_account_name", columnNames = {"email_account_id", "name"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailFolder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_account_id", nullable = false)
    private EmailAccount emailAccount;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailFolderType type;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer messageCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer unreadCount = 0;

    /**
     * IMAP remote folder name mapping (e.g., "INBOX", "[Gmail]/Sent Mail")
     */
    private String remoteFolderName;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.isSystem == null) this.isSystem = false;
        if (this.messageCount == null) this.messageCount = 0;
        if (this.unreadCount == null) this.unreadCount = 0;
    }
}
