package com.itineraryledger.kabengosafaris.Vehicle.Services.VehicleServices;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Vehicle.DTOs.VehicleDTOs.UpdateVehicleDTO;
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
public class UpdateVehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleGetService vehicleGetService;
    private final IdObfuscator idObfuscator;

    @Transactional
    @AuditLogAnnotation(action = "UPDATE_VEHICLE", description = "Updating vehicle", entityType = "Vehicle")
    public ResponseEntity<ApiResponse<?>> updateVehicle(String idObfuscated, UpdateVehicleDTO updateDTO) {
        log.info("Updating vehicle: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Vehicle vehicle = vehicleRepository.findById(id).orElse(null);

            if (vehicle == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Vehicle not found", "VEHICLE_NOT_FOUND")
                );
            }

            if (updateDTO.getRegistrationNumber() != null && !updateDTO.getRegistrationNumber().equals(vehicle.getRegistrationNumber())) {
                if (vehicleRepository.existsByRegistrationNumber(updateDTO.getRegistrationNumber())) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "A vehicle with registration number '" + updateDTO.getRegistrationNumber() + "' already exists", "DUPLICATE_REGISTRATION_NUMBER")
                    );
                }
                vehicle.setRegistrationNumber(updateDTO.getRegistrationNumber());
            }

            if (updateDTO.getName() != null) vehicle.setName(updateDTO.getName());
            if (updateDTO.getType() != null) vehicle.setType(updateDTO.getType());
            if (updateDTO.getMake() != null) vehicle.setMake(updateDTO.getMake());
            if (updateDTO.getModel() != null) vehicle.setModel(updateDTO.getModel());
            if (updateDTO.getYear() != null) vehicle.setYear(updateDTO.getYear());
            if (updateDTO.getColor() != null) vehicle.setColor(updateDTO.getColor());
            if (updateDTO.getCapacity() != null) vehicle.setCapacity(updateDTO.getCapacity());
            if (updateDTO.getFuelType() != null) vehicle.setFuelType(updateDTO.getFuelType());
            if (updateDTO.getMileage() != null) vehicle.setMileage(updateDTO.getMileage());
            if (updateDTO.getInsuranceExpiryDate() != null) vehicle.setInsuranceExpiryDate(updateDTO.getInsuranceExpiryDate());
            if (updateDTO.getInspectionExpiryDate() != null) vehicle.setInspectionExpiryDate(updateDTO.getInspectionExpiryDate());
            if (updateDTO.getIsActive() != null) vehicle.setIsActive(updateDTO.getIsActive());
            if (updateDTO.getNotes() != null) vehicle.setNotes(updateDTO.getNotes());

            vehicle = vehicleRepository.save(vehicle);
            log.info("Vehicle updated successfully: {}", vehicle.getName());

            return ResponseEntity.ok(
                ApiResponse.success(200, "Vehicle updated successfully", vehicleGetService.convertToDTO(vehicle))
            );

        } catch (Exception e) {
            log.error("Error updating vehicle: {}", idObfuscated, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update vehicle", "VEHICLE_UPDATE_FAILED")
            );
        }
    }
}
