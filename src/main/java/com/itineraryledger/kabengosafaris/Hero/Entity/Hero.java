package com.itineraryledger.kabengosafaris.Hero.Entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.Hero.Enums.HeroPage;
import com.itineraryledger.kabengosafaris.User.User;

import jakarta.validation.constraints.Pattern;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Hero Entity - Manages hero sections for website pages
 *
 * Hero sections are prominent content blocks displayed at the top of pages,
 * typically containing a title, subtitle, description, background image(s),
 * and call-to-action button(s).
 *
 * Supports:
 * - Multiple heroes per page for carousel/slider functionality
 * - Display ordering for carousel sequence
 * - Active/inactive status for content management
 * - Multiple images per hero via HeroImage relationship
 */
@Entity
@Table(name = "heroes", indexes = {
    @Index(name = "idx_hero_page", columnList = "page"),
    @Index(name = "idx_hero_is_active", columnList = "is_active"),
    @Index(name = "idx_hero_display_order", columnList = "display_order"),
    @Index(name = "idx_hero_page_order", columnList = "page, display_order"),
    @Index(name = "idx_hero_created_by_id", columnList = "created_by_id"),
    @Index(name = "idx_hero_updated_by_id", columnList = "updated_by_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Main heading/title - large prominent text
     */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /**
     * Secondary text - smaller than title, provides context
     */
    @Column(name = "subtitle", length = 500)
    private String subtitle;

    /**
     * Longer descriptive text content
     */
    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Page/location where this hero should be displayed
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "page", nullable = false, length = 50)
    private HeroPage page;

    /**
     * Call-to-action button text (e.g., "Book Now", "Learn More")
     */
    @Column(name = "cta_text", length = 100)
    private String ctaText;

    /**
     * Call-to-action button link/URL
     */
    @Column(name = "cta_link", length = 500)
    private String ctaLink;

    /**
     * Display order for sorting heroes (lower numbers display first)
     * Used for carousel/slider sequence
     */
    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    /**
     * Active status - only active heroes are displayed on website
     */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Text overlay color (hex code, e.g., "#000000")
     */
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Overlay color must be a valid 6-digit hex color (e.g., #000000)")
    @Column(name = "overlay_color", length = 7)
    private String overlayColor;

    /**
     * Text overlay opacity (0.0 to 1.0)
     */
    @Column(name = "overlay_opacity")
    private Double overlayOpacity;

    /**
     * Text alignment: LEFT, CENTER, RIGHT
     */
    @Column(name = "text_alignment", length = 20)
    private String textAlignment;

    /**
     * CSS class names for custom styling
     */
    @Column(name = "css_classes", length = 500)
    private String cssClasses;

    /**
     * Associated images for this hero (background, mobile variants, etc.)
     */
    @OneToMany(mappedBy = "hero", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<HeroImage> images = new ArrayList<>();

    /**
     * User who created this hero
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    /**
     * User who last updated this hero
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private User updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Helper method to get primary/featured image
     */
    public HeroImage getPrimaryImage() {
        return images.stream()
            .filter(HeroImage::getIsPrimary)
            .findFirst()
            .orElse(null);
    }

    /**
     * Helper method to get active images
     */
    public List<HeroImage> getActiveImages() {
        return images.stream()
            .filter(HeroImage::getIsActive)
            .toList();
    }
}
