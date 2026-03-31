package com.itineraryledger.kabengosafaris.VehicleHire.Repository;

import com.itineraryledger.kabengosafaris.VehicleHire.Entity.VehicleHire;
import com.itineraryledger.kabengosafaris.VehicleHire.Enums.HireStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleHireRepository extends JpaRepository<VehicleHire, Long>, JpaSpecificationExecutor<VehicleHire> {

    List<VehicleHire> findByVehicleId(Long vehicleId);

    @Query("""
        SELECT vh FROM VehicleHire vh
        WHERE vh.vehicle.id = :vehicleId
          AND vh.status <> com.itineraryledger.kabengosafaris.VehicleHire.Enums.HireStatus.CANCELLED
          AND vh.startDate <= :endDate
          AND vh.endDate >= :startDate
          AND (:excludeId IS NULL OR vh.id <> :excludeId)
        """)
    List<VehicleHire> findOverlappingHires(
        @Param("vehicleId") Long vehicleId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("excludeId") Long excludeId
    );

    @Query("""
        SELECT vh FROM VehicleHire vh
        WHERE vh.vehicle.id = :vehicleId
          AND vh.status <> com.itineraryledger.kabengosafaris.VehicleHire.Enums.HireStatus.CANCELLED
          AND vh.startDate <= :endDate
          AND vh.endDate >= :startDate
        ORDER BY vh.startDate ASC
        """)
    List<VehicleHire> findByVehicleIdAndDateRange(
        @Param("vehicleId") Long vehicleId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT vh FROM VehicleHire vh
        WHERE vh.vehicle.id = :vehicleId
          AND vh.startDate <= :endDate
          AND vh.endDate >= :startDate
          AND (:status IS NULL OR vh.status = :status)
        ORDER BY vh.startDate ASC
        """)
    List<VehicleHire> findByVehicleIdAndDateRangeWithStatus(
        @Param("vehicleId") Long vehicleId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("status") HireStatus status
    );

    @Query("SELECT e.id FROM VehicleHire e WHERE e.id > :currentId ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM VehicleHire e WHERE e.id < :currentId ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM VehicleHire e ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT e.id FROM VehicleHire e ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
