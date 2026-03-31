package com.itineraryledger.kabengosafaris.Vehicle.Services.VehicleServices;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Entity.SafariVehicle;
import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Enums.SafariVehicleStatus;
import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Repository.SafariVehicleRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Vehicle.DTOs.VehicleDTOs.VehicleScheduleEntryDTO;
import com.itineraryledger.kabengosafaris.Vehicle.DTOs.VehicleDTOs.VehicleScheduleOverviewDTO;
import com.itineraryledger.kabengosafaris.Vehicle.Entity.Vehicle;
import com.itineraryledger.kabengosafaris.Vehicle.Repository.VehicleRepository;
import com.itineraryledger.kabengosafaris.VehicleHire.Entity.VehicleHire;
import com.itineraryledger.kabengosafaris.VehicleHire.Enums.HireStatus;
import com.itineraryledger.kabengosafaris.VehicleHire.Repository.VehicleHireRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VehicleScheduleService {

    private final VehicleRepository vehicleRepository;
    private final SafariVehicleRepository safariVehicleRepository;
    private final VehicleHireRepository vehicleHireRepository;
    private final IdObfuscator idObfuscator;

    public ResponseEntity<ApiResponse<?>> getVehicleSchedule(
        String vehicleIdObfuscated, LocalDate startDate, LocalDate endDate,
        String entryType, SafariVehicleStatus safariStatus, HireStatus hireStatus,
        Boolean excludeCancelled
    ) {
        log.info("Fetching schedule for vehicle: {}", vehicleIdObfuscated);
        try {
            Long vehicleId = idObfuscator.decodeId(vehicleIdObfuscated);
            Vehicle vehicle = vehicleRepository.findById(vehicleId).orElse(null);
            if (vehicle == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Vehicle not found", "VEHICLE_NOT_FOUND"));
            }

            if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
            if (endDate == null) endDate = startDate.plusMonths(1).minusDays(1);

            List<VehicleScheduleEntryDTO> entries = buildScheduleEntries(
                vehicleId, startDate, endDate, entryType, safariStatus, hireStatus, excludeCancelled);

            Map<String, Object> response = new HashMap<>();
            response.put("vehicleId", idObfuscator.encodeId(vehicleId));
            response.put("vehicleName", vehicle.getName());
            response.put("registrationNumber", vehicle.getRegistrationNumber());
            response.put("schedule", entries);
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            response.put("totalEntries", entries.size());

            return ResponseEntity.ok(ApiResponse.success(200, "Vehicle schedule retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error fetching vehicle schedule", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to retrieve vehicle schedule", "VEHICLE_SCHEDULE_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getScheduleOverview(
        LocalDate startDate, LocalDate endDate,
        String entryType, SafariVehicleStatus safariStatus, HireStatus hireStatus,
        Boolean excludeCancelled
    ) {
        log.info("Fetching vehicle schedule overview");
        try {
            if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
            if (endDate == null) endDate = startDate.plusMonths(1).minusDays(1);

            List<Vehicle> vehicles = vehicleRepository.findAll();
            final LocalDate finalStart = startDate;
            final LocalDate finalEnd = endDate;

            List<VehicleScheduleOverviewDTO> overview = vehicles.stream().map(vehicle -> {
                List<VehicleScheduleEntryDTO> entries = buildScheduleEntries(
                    vehicle.getId(), finalStart, finalEnd, entryType, safariStatus, hireStatus, excludeCancelled);
                return VehicleScheduleOverviewDTO.builder()
                    .vehicleId(idObfuscator.encodeId(vehicle.getId()))
                    .vehicleName(vehicle.getName())
                    .registrationNumber(vehicle.getRegistrationNumber())
                    .vehicleType(vehicle.getType() != null ? vehicle.getType().getDisplayName() : null)
                    .capacity(vehicle.getCapacity())
                    .isActive(vehicle.getIsActive())
                    .entries(entries)
                    .totalAssignments(entries.size())
                    .build();
            }).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("overview", overview);
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            response.put("totalVehicles", overview.size());

            return ResponseEntity.ok(ApiResponse.success(200, "Vehicle schedule overview retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error fetching schedule overview", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to retrieve schedule overview", "SCHEDULE_OVERVIEW_FAILED"));
        }
    }

    private List<VehicleScheduleEntryDTO> buildScheduleEntries(
        Long vehicleId, LocalDate startDate, LocalDate endDate,
        String entryType, SafariVehicleStatus safariStatus, HireStatus hireStatus,
        Boolean excludeCancelled
    ) {
        List<VehicleScheduleEntryDTO> entries = new ArrayList<>();
        boolean includeSafari = entryType == null || "SAFARI".equalsIgnoreCase(entryType);
        boolean includeHire = entryType == null || "HIRE".equalsIgnoreCase(entryType);

        // Safari assignments
        if (includeSafari) {
            SafariVehicleStatus effectiveSafariStatus = safariStatus;
            if (effectiveSafariStatus == null && Boolean.TRUE.equals(excludeCancelled)) {
                // Use the old query that excludes cancelled
                List<SafariVehicle> safariAssignments = safariVehicleRepository.findByVehicleIdAndDateRange(vehicleId, startDate, endDate);
                addSafariEntries(entries, safariAssignments);
            } else {
                // Use the new query with optional status filter (null = all statuses)
                List<SafariVehicle> safariAssignments = safariVehicleRepository.findByVehicleIdAndDateRangeWithStatus(vehicleId, startDate, endDate, effectiveSafariStatus);
                addSafariEntries(entries, safariAssignments);
            }
        }

        // Vehicle hires
        if (includeHire) {
            HireStatus effectiveHireStatus = hireStatus;
            if (effectiveHireStatus == null && Boolean.TRUE.equals(excludeCancelled)) {
                List<VehicleHire> hires = vehicleHireRepository.findByVehicleIdAndDateRange(vehicleId, startDate, endDate);
                addHireEntries(entries, hires);
            } else {
                List<VehicleHire> hires = vehicleHireRepository.findByVehicleIdAndDateRangeWithStatus(vehicleId, startDate, endDate, effectiveHireStatus);
                addHireEntries(entries, hires);
            }
        }

        entries.sort(Comparator.comparing(VehicleScheduleEntryDTO::getStartDate));
        return entries;
    }

    private void addSafariEntries(List<VehicleScheduleEntryDTO> entries, List<SafariVehicle> safariAssignments) {
        for (SafariVehicle sv : safariAssignments) {
            entries.add(VehicleScheduleEntryDTO.builder()
                .id(idObfuscator.encodeId(sv.getId()))
                .type("SAFARI")
                .startDate(sv.getStartDate())
                .endDate(sv.getEndDate())
                .description("Safari: " + sv.getSafari().getName())
                .status(sv.getStatus().getDisplayName())
                .referenceId(idObfuscator.encodeId(sv.getSafari().getId()))
                .referenceName(sv.getSafari().getCode())
                .driverName(sv.getDriverName())
                .driverPhone(sv.getDriverPhone())
                .build());
        }
    }

    private void addHireEntries(List<VehicleScheduleEntryDTO> entries, List<VehicleHire> hires) {
        for (VehicleHire vh : hires) {
            entries.add(VehicleScheduleEntryDTO.builder()
                .id(idObfuscator.encodeId(vh.getId()))
                .type("HIRE")
                .startDate(vh.getStartDate())
                .endDate(vh.getEndDate())
                .description("Hire: " + vh.getClientName())
                .status(vh.getStatus().getDisplayName())
                .referenceId(idObfuscator.encodeId(vh.getId()))
                .referenceName(vh.getClientName())
                .build());
        }
    }
}
