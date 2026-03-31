package com.itineraryledger.kabengosafaris.VehicleHire.Services.VehicleHireServices;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Vehicle.Entity.Vehicle;
import com.itineraryledger.kabengosafaris.Vehicle.Repository.VehicleRepository;
import com.itineraryledger.kabengosafaris.Vehicle.Services.VehicleServices.VehicleAvailabilityService;
import com.itineraryledger.kabengosafaris.VehicleHire.DTOs.VehicleHireDTOs.CreateVehicleHireDTO;
import com.itineraryledger.kabengosafaris.VehicleHire.Entity.VehicleHire;
import com.itineraryledger.kabengosafaris.VehicleHire.Repository.VehicleHireRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreateVehicleHireService {

    private final VehicleHireRepository vehicleHireRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleAvailabilityService vehicleAvailabilityService;
    private final VehicleHireGetService vehicleHireGetService;
    private final IdObfuscator idObfuscator;

    @Transactional
    @AuditLogAnnotation(action = "CREATE_VEHICLE_HIRE", description = "Creating vehicle hire", entityType = "VehicleHire")
    public ResponseEntity<ApiResponse<?>> createVehicleHire(CreateVehicleHireDTO createDTO) {
        log.info("Creating vehicle hire for client: {}", createDTO.getClientName());
        try {
            Long vehicleId = idObfuscator.decodeId(createDTO.getVehicleId());
            Vehicle vehicle = vehicleRepository.findById(vehicleId).orElse(null);
            if (vehicle == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Vehicle not found", "VEHICLE_NOT_FOUND"));
            }

            if (createDTO.getStartDate().isAfter(createDTO.getEndDate())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Start date must be before or equal to end date", "INVALID_DATES"));
            }

            List<String> conflicts = vehicleAvailabilityService.getConflictDescriptions(
                vehicleId, createDTO.getStartDate(), createDTO.getEndDate(), null, null);
            if (!conflicts.isEmpty()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ApiResponse.error(409,
                        "Vehicle is not available for the requested dates. " + conflicts.size() + " conflicting assignment(s) found: " + String.join("; ", conflicts),
                        "VEHICLE_DOUBLE_BOOKING"));
            }

            // Auto-calculate total if dailyRate provided and totalAmount not set
            BigDecimal totalAmount = createDTO.getTotalAmount();
            if (totalAmount == null && createDTO.getDailyRate() != null) {
                long days = ChronoUnit.DAYS.between(createDTO.getStartDate(), createDTO.getEndDate()) + 1;
                totalAmount = createDTO.getDailyRate().multiply(BigDecimal.valueOf(days));
            }

            VehicleHire hire = VehicleHire.builder()
                .vehicle(vehicle)
                .clientName(createDTO.getClientName())
                .clientPhone(createDTO.getClientPhone())
                .clientEmail(createDTO.getClientEmail())
                .startDate(createDTO.getStartDate())
                .endDate(createDTO.getEndDate())
                .pickupLocation(createDTO.getPickupLocation())
                .dropoffLocation(createDTO.getDropoffLocation())
                .dailyRate(createDTO.getDailyRate())
                .totalAmount(totalAmount)
                .currency(createDTO.getCurrency() != null ? createDTO.getCurrency() : "USD")
                .notes(createDTO.getNotes())
                .build();

            hire = vehicleHireRepository.save(hire);
            log.info("Vehicle hire created: {} for client {}", hire.getId(), hire.getClientName());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Vehicle hire created successfully", vehicleHireGetService.convertToDTO(hire)));
        } catch (Exception e) {
            log.error("Error creating vehicle hire", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create vehicle hire", "VEHICLE_HIRE_CREATE_FAILED"));
        }
    }
}
