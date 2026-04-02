package com.itineraryledger.kabengosafaris.Driver.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Driver.DTOs.CreateDriverDTO;
import com.itineraryledger.kabengosafaris.Driver.Entity.Driver;
import com.itineraryledger.kabengosafaris.Driver.Repository.DriverRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreateDriverService {

    private final DriverRepository driverRepository;
    private final DriverGetService driverGetService;

    @Transactional
    @AuditLogAnnotation(action = "CREATE_DRIVER", description = "Creating a new driver", entityType = "Driver")
    public ResponseEntity<ApiResponse<?>> createDriver(CreateDriverDTO createDTO) {
        log.info("Creating driver: {} {}", createDTO.getFirstName(), createDTO.getLastName());

        try {
            if (createDTO.getLicenseNumber() != null && !createDTO.getLicenseNumber().isBlank()) {
                if (driverRepository.existsByLicenseNumber(createDTO.getLicenseNumber())) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "A driver with license number '" + createDTO.getLicenseNumber() + "' already exists", "DUPLICATE_LICENSE_NUMBER")
                    );
                }
            }

            Driver driver = Driver.builder()
                .firstName(createDTO.getFirstName())
                .lastName(createDTO.getLastName())
                .phone(createDTO.getPhone())
                .email(createDTO.getEmail())
                .licenseNumber(createDTO.getLicenseNumber())
                .licenseExpiryDate(createDTO.getLicenseExpiryDate())
                .licenseClass(createDTO.getLicenseClass())
                .talaLicenseNumber(createDTO.getTalaLicenseNumber())
                .talaExpiryDate(createDTO.getTalaExpiryDate())
                .tourGuideId(createDTO.getTourGuideId())
                .tourGuideIdExpiryDate(createDTO.getTourGuideIdExpiryDate())
                .status(createDTO.getStatus() != null ? createDTO.getStatus() : Driver.builder().build().getStatus())
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .notes(createDTO.getNotes())
                .build();

            driver = driverRepository.save(driver);
            log.info("Driver created successfully: {} (ID: {})", driver.getFullName(), driver.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Driver created successfully", driverGetService.convertToDTO(driver))
            );

        } catch (Exception e) {
            log.error("Error creating driver: {} {}", createDTO.getFirstName(), createDTO.getLastName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create driver", "DRIVER_CREATE_FAILED")
            );
        }
    }
}
