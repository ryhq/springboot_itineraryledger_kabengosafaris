package com.itineraryledger.kabengosafaris.Accommodation.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * AccommodationBoardType Entity - Defines meal plan types for accommodations
 *
 * Represents the meal plan/board basis offered:
 * - Room Only (RO) - No meals included
 * - Bed & Breakfast (BB) - Breakfast only
 * - Half Board (HB) - Breakfast + Dinner
 * - Full Board (FB) - Breakfast + Lunch + Dinner
 * - All Inclusive (AI) - All meals + drinks + activities
 *
 * IMPROVED from old version:
 * - Uses self-referencing Accommodation (no separate branch entity!)
 * - Detailed meals included tracking
 * - Inclusions/exclusions support
 */
@Entity
@Table(name = "accommodation_board_types", indexes = {
    @Index(name = "idx_accommodation_board_type_accommodation_id", columnList = "accommodation_id"),
    @Index(name = "idx_accommodation_board_type_name", columnList = "name"),
    @Index(name = "idx_accommodation_board_type_is_active", columnList = "is_active")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_accommodation_board_type_accommodation_name", columnNames = {"accommodation_id", "name"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccommodationBoardType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    @Column(nullable = false, length = 100)
    private String name; // e.g., "Full Board", "Half Board", "Bed & Breakfast"

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "meals_included", length = 255)
    private String mealsIncluded; // e.g., "Breakfast, Lunch, Dinner"

    @Column(name = "breakfast_included", nullable = false)
    @Builder.Default
    private Boolean breakfastIncluded = false;

    @Column(name = "lunch_included", nullable = false)
    @Builder.Default
    private Boolean lunchIncluded = false;

    @Column(name = "dinner_included", nullable = false)
    @Builder.Default
    private Boolean dinnerIncluded = false;

    @Column(name = "snacks_included", nullable = false)
    @Builder.Default
    private Boolean snacksIncluded = false;

    @Column(name = "drinks_included", nullable = false)
    @Builder.Default
    private Boolean drinksIncluded = false;

    @Column(name = "alcoholic_drinks_included", nullable = false)
    @Builder.Default
    private Boolean alcoholicDrinksIncluded = false;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String inclusions; // Detailed list of what's included

    @Lob
    @Column(columnDefinition = "TEXT")
    private String exclusions; // What's NOT included

    @Column(name = "meal_times", length = 500)
    private String mealTimes; // e.g., "Breakfast: 7-10am, Lunch: 12-3pm, Dinner: 7-10pm"

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Helper method to check if any meals are included
     */
    public boolean hasMealsIncluded() {
        return breakfastIncluded || lunchIncluded || dinnerIncluded;
    }

    /**
     * Helper method to get total number of meals included
     */
    public int getMealCount() {
        int count = 0;
        if (breakfastIncluded) count++;
        if (lunchIncluded) count++;
        if (dinnerIncluded) count++;
        return count;
    }

    /**
     * Helper method to check if it's a full meal plan (all 3 meals)
     */
    public boolean isFullMealPlan() {
        return breakfastIncluded && lunchIncluded && dinnerIncluded;
    }
}
