package com.itineraryledger.kabengosafaris.Driver.Entity;

import com.itineraryledger.kabengosafaris.Driver.Enums.DriverStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "drivers",
    indexes = {
        @Index(name = "idx_driver_license_number", columnList = "license_number"),
        @Index(name = "idx_driver_tala_license_number", columnList = "tala_license_number"),
        @Index(name = "idx_driver_tour_guide_id", columnList = "tour_guide_id"),
        @Index(name = "idx_driver_status", columnList = "status"),
        @Index(name = "idx_driver_is_active", columnList = "is_active"),
        @Index(name = "idx_driver_last_name", columnList = "last_name")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_driver_license_number", columnNames = {"license_number"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "First name is required")
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 50)
    private String phone;

    @Column(length = 200)
    private String email;

    @Column(name = "license_number", length = 100, unique = true)
    private String licenseNumber;

    @Column(name = "license_expiry_date")
    private LocalDate licenseExpiryDate;

    @Column(name = "license_class", length = 20)
    private String licenseClass;

    @Column(name = "tala_license_number", length = 100)
    private String talaLicenseNumber;

    @Column(name = "tala_expiry_date")
    private LocalDate talaExpiryDate;

    @Column(name = "tour_guide_id", length = 100)
    private String tourGuideId;

    @Column(name = "tour_guide_id_expiry_date")
    private LocalDate tourGuideIdExpiryDate;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DriverStatus status = DriverStatus.AVAILABLE;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
