package com.itineraryledger.kabengosafaris.Vehicle.Entity;

import com.itineraryledger.kabengosafaris.Vehicle.Enums.FuelType;
import com.itineraryledger.kabengosafaris.Vehicle.Enums.VehicleType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles",
    indexes = {
        @Index(name = "idx_vehicle_registration_number", columnList = "registration_number"),
        @Index(name = "idx_vehicle_type", columnList = "type"),
        @Index(name = "idx_vehicle_is_active", columnList = "is_active"),
        @Index(name = "idx_vehicle_fuel_type", columnList = "fuel_type")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_vehicle_registration_number", columnNames = {"registration_number"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Vehicle name is required")
    @Column(nullable = false, length = 200)
    private String name;

    @NotBlank(message = "Registration number is required")
    @Column(name = "registration_number", nullable = false, length = 50, unique = true)
    private String registrationNumber;

    @NotNull(message = "Vehicle type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VehicleType type;

    @Column(length = 100)
    private String make;

    @Column(length = 100)
    private String model;

    @Column(name = "manufacture_year")
    private Integer year;

    @Column(length = 50)
    private String color;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", length = 20)
    private FuelType fuelType;

    @Column
    private Long mileage;

    @Column(name = "insurance_expiry_date")
    private LocalDate insuranceExpiryDate;

    @Column(name = "inspection_expiry_date")
    private LocalDate inspectionExpiryDate;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
