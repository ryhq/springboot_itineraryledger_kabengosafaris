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
 * One of the company's marks.
 *
 * A logo is not one file. The panel needs a dark mark for light backgrounds and a light one for dark
 * backgrounds — the same wordmark cannot do both — and a browser tab needs a square favicon in each
 * theme. Four SVGs, one slot each, replaceable without a deploy.
 *
 * LOGO_EMAIL is the fifth and the awkward one: most mail clients refuse SVG outright, so a letter
 * needs a raster. Keeping it as its own slot means the office can upload a PNG for mail while the
 * app keeps the crisp vector — rather than discovering, from a customer, that the logo is a blank
 * square in Gmail.
 */
@Entity
@Table(
    name = "company_assets",
    uniqueConstraints = @UniqueConstraint(name = "uq_company_asset_kind", columnNames = {"company_profile_id", "asset_kind"}),
    indexes = @Index(name = "idx_company_asset_profile", columnList = "company_profile_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyAsset {

    public enum AssetKind {
        /** the mark for light backgrounds — dark ink */
        LOGO_LIGHT,
        /** the mark for dark backgrounds — light ink */
        LOGO_DARK,
        /** square, light theme */
        FAVICON_LIGHT,
        /** square, dark theme */
        FAVICON_DARK,
        /** raster for mail, because SVG mostly does not survive an email client */
        LOGO_EMAIL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_profile_id", nullable = false)
    private CompanyProfile companyProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_kind", nullable = false, length = 40)
    private AssetKind assetKind;

    /** Stored name on disk under {app.data.dir}/company — a content hash, never shown. */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** Whether a mail client will render this — SVG in an email is a blank square in most of them. */
    public boolean safeForEmail() {
        return mimeType != null && !mimeType.toLowerCase().contains("svg");
    }
}
