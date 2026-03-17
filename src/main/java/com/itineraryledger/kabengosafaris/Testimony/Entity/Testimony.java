package com.itineraryledger.kabengosafaris.Testimony.Entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Testimony.Enums.TestimonySource;
import com.itineraryledger.kabengosafaris.User.User;

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

@Entity
@Table(name = "testimonies", indexes = {
    @Index(name = "idx_testimony_source", columnList = "source"),
    @Index(name = "idx_testimony_rating", columnList = "rating"),
    @Index(name = "idx_testimony_is_approved", columnList = "is_approved"),
    @Index(name = "idx_testimony_is_featured", columnList = "is_featured"),
    @Index(name = "idx_testimony_is_active", columnList = "is_active"),
    @Index(name = "idx_testimony_display_order", columnList = "display_order"),
    @Index(name = "idx_testimony_customer_id", columnList = "customer_id"),
    @Index(name = "idx_testimony_safari_id", columnList = "safari_id"),
    @Index(name = "idx_testimony_created_by_id", columnList = "created_by_id"),
    @Index(name = "idx_testimony_updated_by_id", columnList = "updated_by_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Testimony {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_name", nullable = false, length = 255)
    private String authorName;

    @Column(name = "author_title", length = 255)
    private String authorTitle;

    @Column(name = "author_country", length = 100)
    private String authorCountry;

    @Column(name = "author_email", length = 255)
    private String authorEmail;

    @Lob
    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Lob
    @Column(name = "admin_response", columnDefinition = "TEXT")
    private String adminResponse;

    @Column(name = "admin_response_date")
    private LocalDateTime adminResponseDate;

    @Column(name = "review_date")
    private LocalDate reviewDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 50)
    private TestimonySource source;

    @Builder.Default
    @Column(name = "is_verified_booking", nullable = false)
    private Boolean isVerifiedBooking = false;

    @Builder.Default
    @Column(name = "is_approved", nullable = false)
    private Boolean isApproved = false;

    @Builder.Default
    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "sentiment_tags", length = 500)
    private String sentimentTags;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "safari_id")
    private Safari safari;

    @OneToMany(mappedBy = "testimony", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TestimonyImage> images = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private User updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public TestimonyImage getPrimaryImage() {
        return images.stream()
            .filter(TestimonyImage::getIsPrimary)
            .findFirst()
            .orElse(null);
    }

    public List<TestimonyImage> getActiveImages() {
        return images.stream()
            .filter(TestimonyImage::getIsActive)
            .sorted((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()))
            .toList();
    }
}
