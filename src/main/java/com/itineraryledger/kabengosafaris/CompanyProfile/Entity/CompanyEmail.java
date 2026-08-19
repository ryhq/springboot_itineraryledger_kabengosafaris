package com.itineraryledger.kabengosafaris.CompanyProfile.Entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One of the company's email addresses.
 *
 * A list, because reservations@ and accounts@ are different desks and a letter should reach the
 * right one. `isPrimary` is what `{{companyEmail}}` resolves to; `isActive` retires an address
 * without deleting the history of letters that quoted it.
 */
@Entity
@Table(name = "company_emails", indexes = @Index(name = "idx_company_email_profile", columnList = "company_profile_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyEmail {

    public enum EmailType { GENERAL, RESERVATIONS, BILLING, SUPPORT, MARKETING, MANAGEMENT, OTHER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_profile_id", nullable = false)
    private CompanyProfile companyProfile;

    @Column(nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_type", nullable = false, length = 50)
    @Builder.Default
    private EmailType emailType = EmailType.GENERAL;

    /** What to call this desk on a document: "Reservations", "Accounts". */
    @Column(length = 100)
    private String label;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
