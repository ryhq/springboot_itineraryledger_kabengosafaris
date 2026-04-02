package com.itineraryledger.kabengosafaris.VehicleHire.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Driver.Entity.Driver;
import com.itineraryledger.kabengosafaris.Driver.Repository.DriverRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.RentalClient.Entity.RentalClient;
import com.itineraryledger.kabengosafaris.RentalClient.Repository.RentalClientRepository;
import com.itineraryledger.kabengosafaris.Vehicle.Entity.Vehicle;
import com.itineraryledger.kabengosafaris.Vehicle.Repository.VehicleRepository;
import com.itineraryledger.kabengosafaris.Vehicle.Services.VehicleAvailabilityService;
import com.itineraryledger.kabengosafaris.Vehicle.Services.VehicleAvailabilityService.AvailabilityResult;
import com.itineraryledger.kabengosafaris.VehicleHire.DTOs.UpdateVehicleHireDTO;
import com.itineraryledger.kabengosafaris.VehicleHire.Entity.VehicleHire;
import com.itineraryledger.kabengosafaris.VehicleHire.Repository.VehicleHireRepository;
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
public class UpdateVehicleHireService {

    private final VehicleHireRepository vehicleHireRepository;
    private final VehicleRepository vehicleRepository;
    private final RentalClientRepository rentalClientRepository;
    private final DriverRepository driverRepository;
    private final VehicleAvailabilityService vehicleAvailabilityService;
    private final VehicleHireGetService vehicleHireGetService;
    private final IdObfuscator idObfuscator;

    @Transactional
    @AuditLogAnnotation(action = "UPDATE_VEHICLE_HIRE", description = "Updating vehicle hire", entityType = "VehicleHire")
    public ResponseEntity<ApiResponse<?>> updateVehicleHire(String idObfuscated, UpdateVehicleHireDTO updateDTO) {
        log.info("Updating vehicle hire: {}", idObfuscated);
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            VehicleHire hire = vehicleHireRepository.findById(id).orElse(null);
            if (hire == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Vehicle hire not found", "VEHICLE_HIRE_NOT_FOUND"));
            }

            boolean needsAvailabilityCheck = false;
            Long vehicleIdForCheck = hire.getVehicle().getId();

            if (updateDTO.getVehicleId() != null) {
                Long newVehicleId = idObfuscator.decodeId(updateDTO.getVehicleId());
                if (!newVehicleId.equals(hire.getVehicle().getId())) {
                    Vehicle newVehicle = vehicleRepository.findById(newVehicleId).orElse(null);
                    if (newVehicle == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                            ApiResponse.error(404, "Vehicle not found", "VEHICLE_NOT_FOUND"));
                    }
                    hire.setVehicle(newVehicle);
                    vehicleIdForCheck = newVehicleId;
                    needsAvailabilityCheck = true;
                }
            }

            if (updateDTO.getStartDate() != null) { hire.setStartDate(updateDTO.getStartDate()); needsAvailabilityCheck = true; }
            if (updateDTO.getEndDate() != null) { hire.setEndDate(updateDTO.getEndDate()); needsAvailabilityCheck = true; }

            LocalDate startDate = hire.getStartDate();
            LocalDate endDate = hire.getEndDate();
            if (startDate.isAfter(endDate)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Start date must be before or equal to end date", "INVALID_DATES"));
            }

            List<String> warnings = null;
            if (needsAvailabilityCheck) {
                AvailabilityResult availability = vehicleAvailabilityService.checkAvailability(
                    vehicleIdForCheck, startDate, endDate, null, hire.getId());
                if (availability.hasHardConflicts()) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        ApiResponse.error(409,
                            "Vehicle is not available for the requested dates. " + availability.hardConflicts().size() + " conflicting assignment(s) found: " + String.join("; ", availability.hardConflicts()),
                            "VEHICLE_DOUBLE_BOOKING"));
                }
                if (availability.hasSoftConflicts()) {
                    warnings = availability.softConflicts().stream()
                        .map(c -> "Same-day overlap: " + c + ". Ensure the vehicle is available for handover.")
                        .toList();
                }
            }

            if (updateDTO.getRentalClientId() != null) {
                Long newRentalClientId = idObfuscator.decodeId(updateDTO.getRentalClientId());
                RentalClient newClient = rentalClientRepository.findById(newRentalClientId).orElse(null);
                if (newClient == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error(404, "Rental client not found", "RENTAL_CLIENT_NOT_FOUND"));
                }
                hire.setRentalClient(newClient);
            }
            if (updateDTO.getDriverId() != null) {
                Long newDriverId = idObfuscator.decodeId(updateDTO.getDriverId());
                Driver newDriver = driverRepository.findById(newDriverId).orElse(null);
                if (newDriver == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error(404, "Driver not found", "DRIVER_NOT_FOUND"));
                }
                hire.setDriver(newDriver);
            }
            if (updateDTO.getPickupLocation() != null) hire.setPickupLocation(updateDTO.getPickupLocation());
            if (updateDTO.getDropoffLocation() != null) hire.setDropoffLocation(updateDTO.getDropoffLocation());
            if (updateDTO.getDailyRate() != null) hire.setDailyRate(updateDTO.getDailyRate());
            if (updateDTO.getTotalAmount() != null) hire.setTotalAmount(updateDTO.getTotalAmount());
            if (updateDTO.getCurrency() != null) hire.setCurrency(updateDTO.getCurrency());
            if (updateDTO.getStatus() != null) hire.setStatus(updateDTO.getStatus());
            if (updateDTO.getPaymentStatus() != null) hire.setPaymentStatus(updateDTO.getPaymentStatus());
            if (updateDTO.getNotes() != null) hire.setNotes(updateDTO.getNotes());

            hire = vehicleHireRepository.save(hire);
            log.info("Vehicle hire updated: {}", id);

            Object dto = vehicleHireGetService.convertToDTO(hire);
            if (warnings != null) {
                return ResponseEntity.ok(ApiResponse.successWithWarnings(200, "Vehicle hire updated successfully", dto, warnings));
            }
            return ResponseEntity.ok(ApiResponse.success(200, "Vehicle hire updated successfully", dto));
        } catch (Exception e) {
            log.error("Error updating vehicle hire: {}", idObfuscated, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update vehicle hire", "VEHICLE_HIRE_UPDATE_FAILED"));
        }
    }
}
