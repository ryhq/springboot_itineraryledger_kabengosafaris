package com.itineraryledger.kabengosafaris.Vehicle.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Vehicle.DTOs.VehicleDTOs.CreateVehicleDTO;
import com.itineraryledger.kabengosafaris.Vehicle.Entity.Vehicle;
import com.itineraryledger.kabengosafaris.Vehicle.Repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreateVehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleGetService vehicleGetService;

    @Transactional
    @AuditLogAnnotation(action = "CREATE_VEHICLE", description = "Creating a new vehicle", entityType = "Vehicle")
    public ResponseEntity<ApiResponse<?>> createVehicle(CreateVehicleDTO createDTO) {
        log.info("Creating vehicle: {}", createDTO.getName());

        try {
            if (vehicleRepository.existsByRegistrationNumber(createDTO.getRegistrationNumber())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "A vehicle with registration number '" + createDTO.getRegistrationNumber() + "' already exists", "DUPLICATE_REGISTRATION_NUMBER")
                );
            }

            Vehicle vehicle = Vehicle.builder()
                .name(createDTO.getName())
                .registrationNumber(createDTO.getRegistrationNumber())
                .type(createDTO.getType())
                .make(createDTO.getMake())
                .model(createDTO.getModel())
                .year(createDTO.getYear())
                .color(createDTO.getColor())
                .capacity(createDTO.getCapacity())
                .fuelType(createDTO.getFuelType())
                .mileage(createDTO.getMileage())
                .insuranceExpiryDate(createDTO.getInsuranceExpiryDate())
                .inspectionExpiryDate(createDTO.getInspectionExpiryDate())
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .notes(createDTO.getNotes())
                .build();

            vehicle = vehicleRepository.save(vehicle);
            log.info("Vehicle created successfully: {} (ID: {})", vehicle.getName(), vehicle.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Vehicle created successfully", vehicleGetService.convertToDTO(vehicle))
            );

        } catch (Exception e) {
            log.error("Error creating vehicle: {}", createDTO.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create vehicle", "VEHICLE_CREATE_FAILED")
            );
        }
    }
}
