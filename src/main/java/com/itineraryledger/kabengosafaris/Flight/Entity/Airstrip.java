package com.itineraryledger.kabengosafaris.Flight.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A place an aircraft lands — Arusha, Seronera, Zanzibar, Kogatende.
 *
 * <p>A shared lookup, like {@code Park}: airstrips belong to the country, not to an airline, and two
 * airlines flying into Seronera are flying into the same strip. That is what makes "which airlines
 * serve Seronera" answerable at all.
 *
 * <p>The {@code code} is the airline's three-letter code (ARS, ZNZ, SER) and is the natural key.
 * Checked against a real price list, the 21 codes each map to exactly one name with no
 * disagreement, which is why it carries a unique constraint and why the transfer module can match
 * on it between companies.
 */
@Entity
@Table(name = "airstrips",
    indexes = {
        @Index(name = "idx_airstrip_is_active", columnList = "is_active"),
        @Index(name = "idx_airstrip_name", columnList = "name"),
        @Index(name = "idx_airstrip_region", columnList = "region"),
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_airstrip_code", columnNames = "code"),
        @UniqueConstraint(name = "uk_airstrip_slug", columnNames = "slug"),
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Airstrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 160)
    private String slug;

    @Column(length = 100)
    private String region;

    @Column(length = 100)
    private String district;

    @Column(nullable = false, length = 100)
    @Builder.Default
    private String country = "Tanzania";

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** "Seronera (SER)" — what a route reads as on a day of a trip. */
    public String getDisplayName() {
        return code == null ? name : name + " (" + code + ")";
    }
}
