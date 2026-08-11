package com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.DTOs.CreateSafariVehicleDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Entity.SafariVehicle;
import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Repository.SafariVehicleRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Driver.Entity.Driver;
import com.itineraryledger.kabengosafaris.Driver.Repository.DriverRepository;
import com.itineraryledger.kabengosafaris.Vehicle.Entity.Vehicle;
import com.itineraryledger.kabengosafaris.Vehicle.Repository.VehicleRepository;
import com.itineraryledger.kabengosafaris.Vehicle.Services.VehicleAvailabilityService;
import com.itineraryledger.kabengosafaris.Vehicle.Services.VehicleAvailabilityService.AvailabilityResult;

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
public class SafariVehicleCreateService {

    private final SafariVehicleRepository safariVehicleRepository;
    private final SafariRepository safariRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final VehicleAvailabilityService vehicleAvailabilityService;
    private final SafariVehicleGetService safariVehicleGetService;
    private final IdObfuscator idObfuscator;

    @Transactional
    @AuditLogAnnotation(action = "CREATE_SAFARI_VEHICLE", description = "Assigning vehicle to safari", entityType = "SafariVehicle")
    public ResponseEntity<ApiResponse<?>> createSafariVehicle(String safariIdObfuscated, CreateSafariVehicleDTO createDTO) {
        log.info("Assigning vehicle to safari: {}", safariIdObfuscated);

        try {
            Long safariId = idObfuscator.decodeId(safariIdObfuscated);
            Safari safari = safariRepository.findById(safariId).orElse(null);
            if (safari == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            /*
             * Assigning a vehicle is operations, not editing.
             *
             * isEditable() excludes FULLY_PAID and IN_PROGRESS — which is exactly
             * when the fleet is arranged. A trip that is paid for and about to
             * start could not be given a Land Cruiser, and one that lost a
             * vehicle mid-safari could not be given another. Only a finished or
             * abandoned booking is closed to it.
             */
            if (safari.getState().isTerminal()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "This safari is " + safari.getState().getDisplayName().toLowerCase()
                            + ", so its vehicles are a record rather than a plan.",
                        "SAFARI_NOT_EDITABLE")
                );
            }

            Long vehicleId = idObfuscator.decodeId(createDTO.getVehicleId());
            Vehicle vehicle = vehicleRepository.findById(vehicleId).orElse(null);
            if (vehicle == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Vehicle not found", "VEHICLE_NOT_FOUND")
                );
            }

            // Auto-populate dates from safari if not specified
            LocalDate startDate = createDTO.getStartDate() != null ? createDTO.getStartDate() : safari.getStartDate();
            LocalDate endDate = createDTO.getEndDate() != null ? createDTO.getEndDate() : safari.getEndDate();

            // Validate dates
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

            // Check availability — hard conflicts block, soft (same-day boundary) conflicts warn
            AvailabilityResult availability = vehicleAvailabilityService.checkAvailability(
                vehicleId, startDate, endDate, null, null);
            if (availability.hasHardConflicts()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ApiResponse.error(409,
                        "Vehicle is not available for the requested dates. " + availability.hardConflicts().size() + " conflicting assignment(s) found: " + String.join("; ", availability.hardConflicts()),
                        "VEHICLE_DOUBLE_BOOKING")
                );
            }

            List<String> warnings = availability.hasSoftConflicts()
                ? availability.softConflicts().stream()
                    .map(c -> "Same-day overlap: " + c + ". Ensure the vehicle is available for handover.")
                    .toList()
                : null;

            // Look up driver if provided
            Driver driver = null;
            if (createDTO.getDriverId() != null && !createDTO.getDriverId().isBlank()) {
                Long driverId = idObfuscator.decodeId(createDTO.getDriverId());
                driver = driverRepository.findById(driverId).orElse(null);
                if (driver == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error(404, "Driver not found", "DRIVER_NOT_FOUND")
                    );
                }
            }

            SafariVehicle safariVehicle = SafariVehicle.builder()
                .safari(safari)
                .vehicle(vehicle)
                .startDate(startDate)
                .endDate(endDate)
                .driver(driver)
                .assignmentNotes(createDTO.getAssignmentNotes())
                .build();

            safariVehicle = safariVehicleRepository.save(safariVehicle);
            log.info("Vehicle {} assigned to safari {} ({} to {})", vehicle.getName(), safari.getName(), startDate, endDate);

            Object dto = safariVehicleGetService.convertToDTO(safariVehicle);
            if (warnings != null) {
                return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.successWithWarnings(201, "Vehicle assigned to safari successfully", dto, warnings)
                );
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Vehicle assigned to safari successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error assigning vehicle to safari", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to assign vehicle to safari", "SAFARI_VEHICLE_CREATE_FAILED")
            );
        }
    }
}
