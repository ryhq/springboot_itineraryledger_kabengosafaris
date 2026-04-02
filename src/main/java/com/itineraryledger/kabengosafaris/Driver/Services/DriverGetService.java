package com.itineraryledger.kabengosafaris.Driver.Services;

import com.itineraryledger.kabengosafaris.Driver.DTOs.DriverDTO;
import com.itineraryledger.kabengosafaris.Driver.DTOs.DriverListItemDTO;
import com.itineraryledger.kabengosafaris.Driver.Entity.Driver;
import com.itineraryledger.kabengosafaris.Driver.Enums.DriverStatus;
import com.itineraryledger.kabengosafaris.Driver.Repository.DriverRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DriverGetService {

    private final DriverRepository driverRepository;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "firstName", "lastName", "phone", "licenseNumber", "status",
        "isActive", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    public ResponseEntity<ApiResponse<?>> getDriverById(String idObfuscated) {
        log.info("Fetching driver with ID: {}", idObfuscated);
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Driver driver = driverRepository.findById(id).orElse(null);

            if (driver == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Driver not found", "DRIVER_NOT_FOUND")
                );
            }

            DriverDTO dto = convertToDTO(driver);

            Long nextId = driverRepository.findNextId(id).orElse(null);
            Long previousId = driverRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = driverRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = driverRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("driver", dto);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok(ApiResponse.success(200, "Driver retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error fetching driver: {}", idObfuscated, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to retrieve driver", "DRIVER_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getAllDrivers(
        String firstName, String lastName, String phone,
        DriverStatus status, Boolean isActive,
        Boolean licenseExpired, Boolean talaExpired, Boolean tourGuideIdExpired,
        String keyword,
        Integer page, Integer size, String sortBy, String sortDirection
    ) {
        log.info("Fetching drivers - page: {}, size: {}", page, size);
        try {
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            Sort sort = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.by(validatedSortBy).descending()
                : Sort.by(validatedSortBy).ascending();

            PageRequest pageRequest = PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 10,
                sort
            );

            Specification<Driver> spec = Specification.<Driver>unrestricted()
                .and(DriverSpecification.firstNameLike(firstName))
                .and(DriverSpecification.lastNameLike(lastName))
                .and(DriverSpecification.phoneLike(phone))
                .and(DriverSpecification.hasStatus(status))
                .and(DriverSpecification.isActive(isActive))
                .and(DriverSpecification.licenseExpired(licenseExpired))
                .and(DriverSpecification.talaExpired(talaExpired))
                .and(DriverSpecification.tourGuideIdExpired(tourGuideIdExpired))
                .and(DriverSpecification.keyword(keyword));

            Page<Driver> driverPage = driverRepository.findAll(spec, pageRequest);

            List<DriverDTO> dtos = driverPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("drivers", dtos);
            response.put("currentPage", driverPage.getNumber());
            response.put("totalItems", driverPage.getTotalElements());
            response.put("totalPages", driverPage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");

            return ResponseEntity.ok(ApiResponse.success(200, "Drivers retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error fetching drivers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list drivers", "DRIVERS_LIST_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getDriversList() {
        log.info("Fetching drivers lightweight list");
        try {
            List<Driver> drivers = driverRepository.findAll(Sort.by(Sort.Direction.ASC, "firstName"));
            List<DriverListItemDTO> dtos = drivers.stream()
                .map(d -> DriverListItemDTO.builder()
                    .id(idObfuscator.encodeId(d.getId()))
                    .fullName(d.getFullName())
                    .phone(d.getPhone())
                    .licenseNumber(d.getLicenseNumber())
                    .status(d.getStatus())
                    .statusDisplayName(d.getStatus() != null ? d.getStatus().getDisplayName() : null)
                    .isActive(d.getIsActive())
                    .build())
                .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(200, "Drivers list retrieved successfully", dtos));

        } catch (Exception e) {
            log.error("Error fetching drivers list", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list drivers", "DRIVERS_LIST_FAILED")
            );
        }
    }

    public DriverDTO convertToDTO(Driver driver) {
        return DriverDTO.builder()
            .id(idObfuscator.encodeId(driver.getId()))
            .firstName(driver.getFirstName())
            .lastName(driver.getLastName())
            .fullName(driver.getFullName())
            .phone(driver.getPhone())
            .email(driver.getEmail())
            .licenseNumber(driver.getLicenseNumber())
            .licenseExpiryDate(driver.getLicenseExpiryDate())
            .licenseClass(driver.getLicenseClass())
            .talaLicenseNumber(driver.getTalaLicenseNumber())
            .talaExpiryDate(driver.getTalaExpiryDate())
            .tourGuideId(driver.getTourGuideId())
            .tourGuideIdExpiryDate(driver.getTourGuideIdExpiryDate())
            .status(driver.getStatus())
            .statusDisplayName(driver.getStatus() != null ? driver.getStatus().getDisplayName() : null)
            .notes(driver.getNotes())
            .isActive(driver.getIsActive())
            .createdAt(driver.getCreatedAt())
            .updatedAt(driver.getUpdatedAt())
            .build();
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }
}
