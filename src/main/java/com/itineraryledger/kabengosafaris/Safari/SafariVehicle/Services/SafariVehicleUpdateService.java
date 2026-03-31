package com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.DTOs.UpdateSafariVehicleDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Entity.SafariVehicle;
import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Repository.SafariVehicleRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Vehicle.Entity.Vehicle;
import com.itineraryledger.kabengosafaris.Vehicle.Repository.VehicleRepository;
import com.itineraryledger.kabengosafaris.Vehicle.Services.VehicleServices.VehicleAvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SafariVehicleUpdateService {

    private final SafariVehicleRepository safariVehicleRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleAvailabilityService vehicleAvailabilityService;
    private final SafariVehicleGetService safariVehicleGetService;
    private final IdObfuscator idObfuscator;

    @Transactional
    @AuditLogAnnotation(action = "UPDATE_SAFARI_VEHICLE", description = "Updating safari vehicle assignment", entityType = "SafariVehicle")
    public ResponseEntity<ApiResponse<?>> updateSafariVehicle(String safariIdObfuscated, String idObfuscated, UpdateSafariVehicleDTO updateDTO) {
        log.info("Updating safari vehicle: {}", idObfuscated);

        try {
            Long safariId = idObfuscator.decodeId(safariIdObfuscated);
            Long id = idObfuscator.decodeId(idObfuscated);

            SafariVehicle safariVehicle = safariVehicleRepository.findById(id).orElse(null);
            if (safariVehicle == null || !safariVehicle.getSafari().getId().equals(safariId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Safari vehicle assignment not found", "SAFARI_VEHICLE_NOT_FOUND")
                );
            }

            Safari safari = safariVehicle.getSafari();
            boolean needsAvailabilityCheck = false;
            Long vehicleIdForCheck = safariVehicle.getVehicle().getId();

            // Update vehicle if changed
            if (updateDTO.getVehicleId() != null) {
                Long newVehicleId = idObfuscator.decodeId(updateDTO.getVehicleId());
                if (!newVehicleId.equals(safariVehicle.getVehicle().getId())) {
                    Vehicle newVehicle = vehicleRepository.findById(newVehicleId).orElse(null);
                    if (newVehicle == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                            ApiResponse.error(404, "Vehicle not found", "VEHICLE_NOT_FOUND")
                        );
                    }
                    safariVehicle.setVehicle(newVehicle);
                    vehicleIdForCheck = newVehicleId;
                    needsAvailabilityCheck = true;
                }
            }

            if (updateDTO.getStartDate() != null) {
                safariVehicle.setStartDate(updateDTO.getStartDate());
                needsAvailabilityCheck = true;
            }
            if (updateDTO.getEndDate() != null) {
                safariVehicle.setEndDate(updateDTO.getEndDate());
                needsAvailabilityCheck = true;
            }

            // Validate dates
            LocalDate startDate = safariVehicle.getStartDate();
            LocalDate endDate = safariVehicle.getEndDate();

            if (startDate.isAfter(endDate)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Start date must be before or equal to end date", "INVALID_DATES")
                );
            }

            if (startDate.isBefore(safari.getStartDate()) || endDate.isAfter(safari.getEndDate())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Assignment dates must be within safari dates (" + safari.getStartDate() + " to " + safari.getEndDate() + ")", "DATES_OUTSIDE_SAFARI_RANGE")
                );
            }

            // Re-check availability if vehicle or dates changed
            if (needsAvailabilityCheck) {
                List<String> conflicts = vehicleAvailabilityService.getConflictDescriptions(
                    vehicleIdForCheck, startDate, endDate, safariVehicle.getId(), null);
                if (!conflicts.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        ApiResponse.error(409,
                            "Vehicle is not available for the requested dates. " + conflicts.size() + " conflicting assignment(s) found: " + String.join("; ", conflicts),
                            "VEHICLE_DOUBLE_BOOKING")
                    );
                }
            }

            if (updateDTO.getDriverName() != null) safariVehicle.setDriverName(updateDTO.getDriverName());
            if (updateDTO.getDriverPhone() != null) safariVehicle.setDriverPhone(updateDTO.getDriverPhone());
            if (updateDTO.getAssignmentNotes() != null) safariVehicle.setAssignmentNotes(updateDTO.getAssignmentNotes());
            if (updateDTO.getStatus() != null) safariVehicle.setStatus(updateDTO.getStatus());

            safariVehicle = safariVehicleRepository.save(safariVehicle);
            log.info("Safari vehicle updated: {}", id);

            return ResponseEntity.ok(
                ApiResponse.success(200, "Safari vehicle updated successfully", safariVehicleGetService.convertToDTO(safariVehicle))
            );

        } catch (Exception e) {
            log.error("Error updating safari vehicle: {}", idObfuscated, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update safari vehicle", "SAFARI_VEHICLE_UPDATE_FAILED")
            );
        }
    }
}
