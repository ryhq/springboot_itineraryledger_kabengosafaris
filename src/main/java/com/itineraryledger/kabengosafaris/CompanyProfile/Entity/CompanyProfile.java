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

    /**
     * When that licence stops being true.
     *
     * <p>A tour operator licence is renewed yearly, so the number alone is a half-fact: documents
     * go on printing it long after it lapses, and the first anybody hears is from the authority or
     * a client. Null means nobody has recorded a date, not that it never expires.
     */
    @Column(name = "licence_expiry")
    private java.time.LocalDate licenceExpiry;

    /** Days until the licence lapses; negative once it has. Null when there is nothing to judge. */
    public Long daysUntilLicenceExpiry() {
        if (licenceExpiry == null) return null;
        return java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), licenceExpiry);
    }

    /** True once the recorded expiry is in the past. */
    public boolean licenceLapsed() {
        Long d = daysUntilLicenceExpiry();
        return d != null && d < 0;
    }

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
    /** drawn when no logo file is uploaded; blank means "the first letter of the trading name" */
    @Column(name = "brand_mark", length = 8)
    private String brandMark;

    @Column(name = "brand_accent", length = 32)
    private String brandAccent;

    @Column(name = "brand_radius", length = 16)
    private String brandRadius;

    @Column(name = "brand_font", length = 160)
    private String brandFont;

    /* ----------------------------------------------------------------- website */

    /**
     * The public website's origin, e.g. https://example.com — scheme and host, no path.
     *
     * <p>Deliberately NOT the "website" link in the links collection. That one is for printing on a
     * letterhead and may be a pretty form with a locale on the end; this one is dialled by a machine
     * and has to be the real origin the site is served from. Blank means this installation has no
     * website to talk to, and every cache call becomes a no-op rather than an error.
     */
    @Column(name = "website_url", length = 300)
    private String websiteUrl;

    /**
     * The shared secret the website requires before it will throw a cached page away, stored
     * encrypted.
     *
     * <p>Never returned by any endpoint. The profile payload carries only whether one is set, so a
     * screenshot of the Settings page, or a browser tab left open on a shared machine, cannot leak
     * it. Rotating means pasting a new value in both places; there is no way to read the old one
     * back, which is what a credential should do.
     */
    @Column(name = "website_cache_secret", length = 512)
    private String websiteCacheSecret;

    /** Whether saving public content should clear the website by itself. Off = the button only. */
    @Column(name = "website_cache_auto", nullable = false)
    @Builder.Default
    private Boolean websiteCacheAuto = true;

    @Column(name = "website_cache_last_called_at")
    private LocalDateTime websiteCacheLastCalledAt;

    /** Whether the website answered the last call. Null until one has been made. */
    @Column(name = "website_cache_last_ok")
    private Boolean websiteCacheLastOk;

    /** What it said, or why nobody answered. The only honest record of cache state we have. */
    @Column(name = "website_cache_last_detail", length = 500)
    private String websiteCacheLastDetail;

    /** True once a URL and a secret are both present — the two things a cache call needs. */
    public boolean websiteCacheConfigured() {
        return websiteUrl != null && !websiteUrl.isBlank()
            && websiteCacheSecret != null && !websiteCacheSecret.isBlank();
    }

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
