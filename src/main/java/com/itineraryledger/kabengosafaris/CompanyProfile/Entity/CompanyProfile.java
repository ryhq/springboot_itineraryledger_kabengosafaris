package com.itineraryledger.kabengosafaris.CompanyProfile.Entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Who this installation IS.
 *
 * Every letter, invoice, voucher and signature this system produces says a company's name, quotes
 * its TIN and prints its address — and until now it said them because 38 of the 42 shipped template
 * files had the words typed into them. That made a second company a find-and-replace across the
 * repository, and it made the first company's own change of address a deploy.
 *
 * So the identity is a record. One row, edited in Settings, read by both renderers.
 *
 * Contact details are NOT columns here: a company has several emails, several phones, more than one
 * address and a handful of links, each of which can be retired without being forgotten. They are
 * collections with the same primary/active flags an accommodation's contacts already use.
 *
 * Bank details are not here either. They live in the Bank accounts module, which the invoice PDF
 * already reads — a sort code should have exactly one home.
 */
@Entity
@Table(name = "company_profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyProfile {

    /**
     * One row. The service enforces it rather than the schema, because a UNIQUE constraint on a
     * constant is a constraint nobody can read.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** What customers call it: "Kabengo Safaris". */
    @Column(name = "trading_name", nullable = false, length = 200)
    private String tradingName;

    /** What the tax authority calls it: "Kabengo Safaris Ltd". Invoices need this one. */
    @Column(name = "legal_name", length = 200)
    private String legalName;

    @Column(length = 300)
    private String tagline;

    /* --------------------------------------------------------------- registration */

    /** Taxpayer Identification Number. Absent, the tax line on an invoice is empty. */
    @Column(name = "tin", length = 50)
    private String tin;

    /** VAT registration. Absent, an invoice cannot show VAT as reclaimable. */
    @Column(name = "vrn", length = 50)
    private String vrn;

    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    /** e.g. TALA / licence number a tour operator quotes on documents. */
    @Column(name = "licence_number", length = 100)
    private String licenceNumber;

    /* ------------------------------------------------------------------- regional */

    /** ISO 4217. What a figure means when nobody says otherwise. */
    @Column(name = "default_currency", length = 3)
    private String defaultCurrency;

    /** IANA zone, e.g. Africa/Dar_es_Salaam — what "today" means to this company. */
    @Column(name = "timezone", length = 64)
    private String timezone;

    /** BCP 47, e.g. en-TZ. */
    @Column(name = "locale", length = 16)
    private String locale;

    /*
     * How the company looks. Blank means "whatever the app ships with" — this is an override, not a
     * requirement, so an installation that never opens the Brand tab is unaffected.
     */
    @Column(name = "brand_accent", length = 32)
    private String brandAccent;

    @Column(name = "brand_radius", length = 16)
    private String brandRadius;

    @Column(name = "brand_font", length = 160)
    private String brandFont;

    /* --------------------------------------------------------------- collections */

    @OneToMany(mappedBy = "companyProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 32)
    @Builder.Default
    private List<CompanyEmail> emails = new ArrayList<>();

    @OneToMany(mappedBy = "companyProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 32)
    @Builder.Default
    private List<CompanyPhone> phones = new ArrayList<>();

    @OneToMany(mappedBy = "companyProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 32)
    @Builder.Default
    private List<CompanyAddress> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "companyProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 32)
    @Builder.Default
    private List<CompanyLink> links = new ArrayList<>();

    @OneToMany(mappedBy = "companyProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 32)
    @Builder.Default
    private List<CompanyAsset> assets = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** The name to sign a letter with: the trading name, or the legal one if that is all there is. */
    public String displayName() {
        if (tradingName != null && !tradingName.isBlank()) return tradingName;
        return legalName != null ? legalName : "";
    }
}
