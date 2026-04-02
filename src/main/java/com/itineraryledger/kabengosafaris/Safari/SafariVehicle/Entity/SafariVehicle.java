package com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Driver.Entity.Driver;
import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Enums.SafariVehicleStatus;
import com.itineraryledger.kabengosafaris.Vehicle.Entity.Vehicle;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "safari_vehicles",
    indexes = {
        @Index(name = "idx_sv_safari_id", columnList = "safari_id"),
        @Index(name = "idx_sv_vehicle_id", columnList = "vehicle_id"),
        @Index(name = "idx_sv_status", columnList = "status"),
        @Index(name = "idx_sv_start_date", columnList = "start_date"),
        @Index(name = "idx_sv_end_date", columnList = "end_date"),
        @Index(name = "idx_sv_driver_id", columnList = "driver_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafariVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "safari_id", nullable = false)
    @JsonIgnore
    private Safari safari;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @NotNull(message = "Start date is required")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Lob
    @Column(name = "assignment_notes", columnDefinition = "TEXT")
    private String assignmentNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SafariVehicleStatus status = SafariVehicleStatus.ASSIGNED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
