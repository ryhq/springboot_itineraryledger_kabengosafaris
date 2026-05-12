package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Frequent-correspondent shortcut surfaced on the inbox folder rail. See
 * §6 in EMAIL_INBOX_API.md.
 */
@Entity
@Table(name = "pinned_contacts", indexes = {
    @Index(name = "idx_pinned_contact_account_id", columnList = "email_account_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_pinned_contact_account_email", columnNames = {"email_account_id", "email"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PinnedContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_account_id", nullable = false)
    private EmailAccount emailAccount;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(length = 120)
    private String name;

    @Column(length = 120)
    private String role;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
