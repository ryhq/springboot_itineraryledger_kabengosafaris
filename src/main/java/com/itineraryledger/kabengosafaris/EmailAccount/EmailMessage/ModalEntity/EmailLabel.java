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

/**
 * User- or system-defined label attached to email messages via the
 * email_message_labels join table. See §1 in EMAIL_INBOX_API.md.
 */
@Entity
@Table(name = "email_labels", indexes = {
    @Index(name = "idx_email_label_account_id", columnList = "email_account_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_email_label_account_name", columnNames = {"email_account_id", "name"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLabel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_account_id", nullable = false)
    private EmailAccount emailAccount;

    @Column(nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailLabelColor color;

    /**
     * True for the four system labels (Quote / Booking / Vendor / Internal)
     * seeded on account creation. System labels cannot be deleted or renamed.
     */
    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.isSystem == null) this.isSystem = false;
    }
}
