package com.itineraryledger.kabengosafaris.Driver.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Driver.DTOs.UpdateDriverDTO;
import com.itineraryledger.kabengosafaris.Driver.Entity.Driver;
import com.itineraryledger.kabengosafaris.Driver.Repository.DriverRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UpdateDriverService {

    private final DriverRepository driverRepository;
    private final DriverGetService driverGetService;
    private final IdObfuscator idObfuscator;

    @Transactional
    @AuditLogAnnotation(action = "UPDATE_DRIVER", description = "Updating driver", entityType = "Driver")
    public ResponseEntity<ApiResponse<?>> updateDriver(String idObfuscated, UpdateDriverDTO updateDTO) {
        log.info("Updating driver: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Driver driver = driverRepository.findById(id).orElse(null);

            if (driver == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Driver not found", "DRIVER_NOT_FOUND")
                );
            }

            if (updateDTO.getLicenseNumber() != null && !updateDTO.getLicenseNumber().equals(driver.getLicenseNumber())) {
                if (driverRepository.existsByLicenseNumber(updateDTO.getLicenseNumber())) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "A driver with license number '" + updateDTO.getLicenseNumber() + "' already exists", "DUPLICATE_LICENSE_NUMBER")
                    );
                }
                driver.setLicenseNumber(updateDTO.getLicenseNumber());
            }

            if (updateDTO.getFirstName() != null) driver.setFirstName(updateDTO.getFirstName());
            if (updateDTO.getLastName() != null) driver.setLastName(updateDTO.getLastName());
            if (updateDTO.getPhone() != null) driver.setPhone(updateDTO.getPhone());
            if (updateDTO.getEmail() != null) driver.setEmail(updateDTO.getEmail());
            if (updateDTO.getLicenseExpiryDate() != null) driver.setLicenseExpiryDate(updateDTO.getLicenseExpiryDate());
            if (updateDTO.getLicenseClass() != null) driver.setLicenseClass(updateDTO.getLicenseClass());
            if (updateDTO.getTalaLicenseNumber() != null) driver.setTalaLicenseNumber(updateDTO.getTalaLicenseNumber());
            if (updateDTO.getTalaExpiryDate() != null) driver.setTalaExpiryDate(updateDTO.getTalaExpiryDate());
            if (updateDTO.getTourGuideId() != null) driver.setTourGuideId(updateDTO.getTourGuideId());
            if (updateDTO.getTourGuideIdExpiryDate() != null) driver.setTourGuideIdExpiryDate(updateDTO.getTourGuideIdExpiryDate());
            if (updateDTO.getStatus() != null) driver.setStatus(updateDTO.getStatus());
            if (updateDTO.getIsActive() != null) driver.setIsActive(updateDTO.getIsActive());
            if (updateDTO.getNotes() != null) driver.setNotes(updateDTO.getNotes());

            driver = driverRepository.save(driver);
            log.info("Driver updated successfully: {}", driver.getFullName());

            return ResponseEntity.ok(
                ApiResponse.success(200, "Driver updated successfully", driverGetService.convertToDTO(driver))
            );

        } catch (Exception e) {
            log.error("Error updating driver: {}", idObfuscated, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update driver", "DRIVER_UPDATE_FAILED")
            );
        }
    }
}
