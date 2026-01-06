package com.itineraryledger.kabengosafaris.Activity;

import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity Entity - Represents activities available at parks or destinations
 *
 * Activities are standalone entities that can exist independently.
 * They represent various tourism activities such as game drives, walking safaris,
 * bird watching, mountain climbing, snorkeling, diving, etc.
 *
 * Activities can have tariffs (costs) or be free, and can be charged on different bases
 * (per person, per vehicle, per group, etc.)
 */
@Entity
@Table(name = "activities", indexes = {
    @Index(name = "idx_name", columnList = "name"),
    @Index(name = "idx_web_active", columnList = "is_web_active"),
    @Index(name = "idx_has_tariff", columnList = "has_tariff"),
    @Index(name = "idx_slug", columnList = "slug")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "slug", unique = true, length = 200)
    private String slug;

    @Column(name = "has_tariff", nullable = false)
    @Builder.Default
    private Boolean hasTariff = false;

    @Column(name = "is_web_active", nullable = false)
    @Builder.Default
    private Boolean isWebActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "charging_basis", length = 50)
    @Builder.Default
    private ChargingBasis chargingBasis = ChargingBasis.PER_PERSON;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Lob
    @Column(name = "detailed_description", columnDefinition = "LONGTEXT")
    private String detailedDescription;

    @Column(name = "minimum_age")
    private Integer minimumAge;

    @Column(name = "maximum_participants")
    private Integer maximumParticipants;

    @Column(name = "equipment_required", columnDefinition = "TEXT")
    private String equipmentRequired;

    @Column(name = "season_availability", length = 200)
    private String seasonAvailability;

    @Column(name = "primary_image", length = 500)
    private String primaryImage;

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    @Column(name = "safety_information", columnDefinition = "TEXT")
    private String safetyInformation;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ParkActivity> parkActivities = new ArrayList<>();

    @PrePersist
    @PreUpdate
    private void generateSlug() {
        if (this.slug == null || this.slug.isEmpty()) {
            this.slug = this.name
                    .toLowerCase()
                    .replaceAll("[^a-z0-9\\s-]", "")
                    .replaceAll("\\s+", "-")
                    .replaceAll("-+", "-")
                    .trim();
        }
    }
}
