package com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Repository;

import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Entity.SafariVehicle;
import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Enums.SafariVehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SafariVehicleRepository extends JpaRepository<SafariVehicle, Long> {

    List<SafariVehicle> findBySafariId(Long safariId);

    @Query("""
        SELECT sv FROM SafariVehicle sv
        WHERE sv.vehicle.id = :vehicleId
          AND sv.status <> com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Enums.SafariVehicleStatus.CANCELLED
          AND sv.startDate <= :endDate
          AND sv.endDate >= :startDate
          AND (:excludeId IS NULL OR sv.id <> :excludeId)
        """)
    List<SafariVehicle> findOverlappingAssignments(
        @Param("vehicleId") Long vehicleId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("excludeId") Long excludeId
    );

    @Query("""
        SELECT sv FROM SafariVehicle sv
        WHERE sv.vehicle.id = :vehicleId
          AND sv.status <> com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Enums.SafariVehicleStatus.CANCELLED
          AND sv.startDate <= :endDate
          AND sv.endDate >= :startDate
        ORDER BY sv.startDate ASC
        """)
    List<SafariVehicle> findByVehicleIdAndDateRange(
        @Param("vehicleId") Long vehicleId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT sv FROM SafariVehicle sv
        WHERE sv.vehicle.id = :vehicleId
          AND sv.startDate <= :endDate
          AND sv.endDate >= :startDate
          AND (:status IS NULL OR sv.status = :status)
        ORDER BY sv.startDate ASC
        """)
    List<SafariVehicle> findByVehicleIdAndDateRangeWithStatus(
        @Param("vehicleId") Long vehicleId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("status") SafariVehicleStatus status
    );

    // Parent-scoped circular navigation
    @Query("SELECT sv.id FROM SafariVehicle sv WHERE sv.safari.id = :safariId AND sv.id > :currentId ORDER BY sv.id ASC LIMIT 1")
    Optional<Long> findNextIdBySafariId(@Param("safariId") Long safariId, @Param("currentId") Long currentId);

    @Query("SELECT sv.id FROM SafariVehicle sv WHERE sv.safari.id = :safariId AND sv.id < :currentId ORDER BY sv.id DESC LIMIT 1")
    Optional<Long> findPreviousIdBySafariId(@Param("safariId") Long safariId, @Param("currentId") Long currentId);

    @Query("SELECT sv.id FROM SafariVehicle sv WHERE sv.safari.id = :safariId ORDER BY sv.id ASC LIMIT 1")
    Optional<Long> findFirstIdBySafariId(@Param("safariId") Long safariId);

    @Query("SELECT sv.id FROM SafariVehicle sv WHERE sv.safari.id = :safariId ORDER BY sv.id DESC LIMIT 1")
    Optional<Long> findLastIdBySafariId(@Param("safariId") Long safariId);
}
