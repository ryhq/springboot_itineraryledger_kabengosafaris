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
 * A web address the company puts on its documents — the site, and the profiles a guest checks
 * before booking. `{{companyWebsite}}` is the primary active WEBSITE; a footer can print the rest.
 */
@Entity
@Table(name = "company_links", indexes = @Index(name = "idx_company_link_profile", columnList = "company_profile_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyLink {

    public enum LinkType { WEBSITE, BOOKING, FACEBOOK, INSTAGRAM, TRIPADVISOR, LINKEDIN, X, YOUTUBE, TIKTOK, OTHER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_profile_id", nullable = false)
    private CompanyProfile companyProfile;

    @Column(nullable = false, length = 500)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 50)
    @Builder.Default
    private LinkType linkType = LinkType.WEBSITE;

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

    /** "www.kabengosafaris.com" — a document prints the address, not the protocol. */
    public String display() {
        if (url == null) return "";
        return url.replaceFirst("^https?://", "").replaceFirst("/$", "");
    }
}
