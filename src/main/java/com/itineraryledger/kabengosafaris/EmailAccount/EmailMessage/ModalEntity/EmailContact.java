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
@Table(name = "email_contacts", indexes = {
    @Index(name = "idx_email_contact_account_id", columnList = "email_account_id"),
    @Index(name = "idx_email_contact_email", columnList = "email_address"),
    @Index(name = "idx_email_contact_frequency", columnList = "frequency")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_email_contact_account_email", columnNames = {"email_account_id", "email_address"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_account_id", nullable = false)
    private EmailAccount emailAccount;

    @Column(name = "email_address", nullable = false)
    private String emailAddress;

    @Column(name = "display_name")
    private String displayName;

    /**
     * How many times this contact has been used (sent to / received from)
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer frequency = 1;

    @Column(name = "last_contacted_at")
    private LocalDateTime lastContactedAt;

    /**
     * How this contact was discovered: SENT, RECEIVED, CC, MANUAL
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContactSource source;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isStarred = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum ContactSource {
        SENT, RECEIVED, CC, MANUAL
    }
}
