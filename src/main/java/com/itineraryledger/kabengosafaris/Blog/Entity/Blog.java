package com.itineraryledger.kabengosafaris.Blog.Entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.User.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

/**
 * A blog article, as the public website renders it.
 *
 * The body is a LIST OF BLOCKS held as JSON rather than a slab of HTML: the website walks
 * paragraphs, headings, bullet lists and images to build both the page and its JSON-LD, and
 * a single HTML string could not be walked. The per-post FAQs are JSON for the same reason —
 * they are page furniture for one article, not rows anybody queries across.
 *
 * Long text is TEXT/LONGTEXT and never {@code @Lob}: a CLOB cannot be passed through
 * LOWER()/TRIM() in a query, which is exactly what the keyword search does.
 */
@Entity
@Table(name = "blogs", indexes = {
    @Index(name = "idx_blog_slug", columnList = "slug", unique = true),
    @Index(name = "idx_blog_is_published", columnList = "is_published"),
    @Index(name = "idx_blog_publish_date", columnList = "publish_date"),
    @Index(name = "idx_blog_display_order", columnList = "display_order"),
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The URL the website serves this at. Unique, because it IS the address. */
    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "excerpt", columnDefinition = "TEXT")
    private String excerpt;

    @Column(name = "author", length = 255)
    private String author;

    @Column(name = "publish_date")
    private LocalDate publishDate;

    /** Estimated from the body when it is left blank, and editable afterwards. */
    @Column(name = "read_minutes")
    private Integer readMinutes;

    /*
     * The block list, as JSON. Types: p, h2, h3, ul (items[]), image (url/alt/caption).
     * LONGTEXT because an article runs to thousands of words and a VARCHAR(500) truncation
     * is how we lost content once before.
     */
    @Column(name = "body_json", columnDefinition = "LONGTEXT")
    private String bodyJson;

    /** The article's own FAQ block: [{q, a}], embedded in its page. */
    @Column(name = "faqs_json", columnDefinition = "LONGTEXT")
    private String faqsJson;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "blog_tags", joinColumns = @JoinColumn(name = "blog_id"))
    @Column(name = "tag", length = 160)
    @BatchSize(size = 30)
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Builder.Default
    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;

    /**
     * When this article FIRST went live, and therefore when its slug froze.
     *
     * Policy: a slug is not edited once the post has been published. The address is out in the
     * world by then — in search results, in somebody's bookmarks, in a WhatsApp message — and
     * renaming it turns every one of those into a 404. Current state is not enough to enforce
     * that: unpublish, rename, republish would sidestep it, so the moment is recorded once and
     * never cleared. Nullable, so it is safe to add to an existing table.
     */
    @Column(name = "first_published_at")
    private LocalDateTime firstPublishedAt;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "meta_title", length = 500)
    private String metaTitle;

    @Column(name = "meta_description", columnDefinition = "TEXT")
    private String metaDescription;

    @OneToMany(mappedBy = "blog", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC, id ASC")
    @BatchSize(size = 30)
    @Builder.Default
    private List<BlogImage> images = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
