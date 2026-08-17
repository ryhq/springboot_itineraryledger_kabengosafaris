package com.itineraryledger.kabengosafaris.Blog.Entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An uploaded image belonging to a blog post.
 *
 * A post has one cover ({@code isPrimary}) and any number of gallery images; an image block
 * in the body points at one of these by URL. Mirrors the accommodation/testimony image
 * pattern exactly, so the upload, ordering and delete flows are the ones already in use.
 */
@Entity
@Table(name = "blog_images", indexes = {
    @Index(name = "idx_blog_image_blog_id", columnList = "blog_id"),
    @Index(name = "idx_blog_image_is_primary", columnList = "is_primary"),
    @Index(name = "idx_blog_image_display_order", columnList = "display_order"),
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id", nullable = false)
    private Blog blog;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_file_name", length = 500)
    private String originalFileName;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "caption", length = 500)
    private String caption;

    /** The cover: one per post, enforced when one is set. */
    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
