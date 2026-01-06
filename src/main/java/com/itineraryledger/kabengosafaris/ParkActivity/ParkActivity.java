package com.itineraryledger.kabengosafaris.ParkActivity;

import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Park.Park;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * ParkActivity Join Entity - Manages many-to-many relationship between Parks and Activities
 *
 * This entity represents the association between parks and activities, allowing:
 * - A park to have multiple activities
 * - An activity to be available in multiple parks
 * - Unique constraints to prevent duplicate associations
 * - Independent lifecycle (deleting a park or activity doesn't affect the other)
 */
@Entity
@Table(name = "parks_activities",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_park_activity", columnNames = {"park_id", "activity_id"})
    },
    indexes = {
        @Index(name = "idx_park_id", columnList = "park_id"),
        @Index(name = "idx_activity_id", columnList = "activity_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ParkActivityId.class)
public class ParkActivity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "park_id", nullable = false)
    private Park park;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes; // Optional notes about this activity in this specific park
}
