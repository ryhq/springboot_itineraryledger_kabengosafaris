package com.itineraryledger.kabengosafaris.Vehicle.Services;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Vehicle.DTOs.VehicleDTOs.VehicleDTO;
import com.itineraryledger.kabengosafaris.Vehicle.DTOs.VehicleDTOs.VehicleListItemDTO;
import com.itineraryledger.kabengosafaris.Vehicle.Entity.Vehicle;
import com.itineraryledger.kabengosafaris.Vehicle.Enums.FuelType;
import com.itineraryledger.kabengosafaris.Vehicle.Enums.VehicleType;
import com.itineraryledger.kabengosafaris.Vehicle.Repository.VehicleRepository;
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
public class VehicleGetService {

    private final VehicleRepository vehicleRepository;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "name", "registrationNumber", "type", "make", "model", "year",
        "capacity", "fuelType", "mileage", "isActive", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    public ResponseEntity<ApiResponse<?>> getVehicleById(String idObfuscated) {
        log.info("Fetching vehicle with ID: {}", idObfuscated);
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Vehicle vehicle = vehicleRepository.findById(id).orElse(null);

            if (vehicle == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Vehicle not found", "VEHICLE_NOT_FOUND")
                );
            }

            VehicleDTO dto = convertToDTO(vehicle);

            Long nextId = vehicleRepository.findNextId(id).orElse(null);
            Long previousId = vehicleRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = vehicleRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = vehicleRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("vehicle", dto);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok(ApiResponse.success(200, "Vehicle retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error fetching vehicle: {}", idObfuscated, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to retrieve vehicle", "VEHICLE_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getAllVehicles(
        String name, String registrationNumber, VehicleType type,
        String make, String model, Integer year, FuelType fuelType,
        Integer minCapacity, Integer maxCapacity, Boolean isActive,
        Boolean insuranceExpired, Boolean inspectionExpired,
        String keyword,
        Integer page, Integer size, String sortBy, String sortDirection
    ) {
        log.info("Fetching vehicles - page: {}, size: {}", page, size);
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

            Specification<Vehicle> spec = Specification.<Vehicle>unrestricted()
                .and(VehicleSpecification.nameLike(name))
                .and(VehicleSpecification.registrationNumberLike(registrationNumber))
                .and(VehicleSpecification.hasType(type))
                .and(VehicleSpecification.makeLike(make))
                .and(VehicleSpecification.modelLike(model))
                .and(VehicleSpecification.hasYear(year))
                .and(VehicleSpecification.hasFuelType(fuelType))
                .and(VehicleSpecification.minCapacity(minCapacity))
                .and(VehicleSpecification.maxCapacity(maxCapacity))
                .and(VehicleSpecification.isActive(isActive))
                .and(VehicleSpecification.insuranceExpired(insuranceExpired))
                .and(VehicleSpecification.inspectionExpired(inspectionExpired))
                .and(VehicleSpecification.keyword(keyword));

            Page<Vehicle> vehiclePage = vehicleRepository.findAll(spec, pageRequest);

            List<VehicleDTO> dtos = vehiclePage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("vehicles", dtos);
            response.put("currentPage", vehiclePage.getNumber());
            response.put("totalItems", vehiclePage.getTotalElements());
            response.put("totalPages", vehiclePage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");

            return ResponseEntity.ok(ApiResponse.success(200, "Vehicles retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error fetching vehicles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list vehicles", "VEHICLES_LIST_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getVehiclesList() {
        log.info("Fetching vehicles lightweight list");
        try {
            List<Vehicle> vehicles = vehicleRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
            List<VehicleListItemDTO> dtos = vehicles.stream()
                .map(v -> VehicleListItemDTO.builder()
                    .id(idObfuscator.encodeId(v.getId()))
                    .name(v.getName())
                    .registrationNumber(v.getRegistrationNumber())
                    .type(v.getType())
                    .typeDisplayName(v.getType() != null ? v.getType().getDisplayName() : null)
                    .capacity(v.getCapacity())
                    .isActive(v.getIsActive())
                    .build())
                .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(200, "Vehicles list retrieved successfully", dtos));

        } catch (Exception e) {
            log.error("Error fetching vehicles list", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list vehicles", "VEHICLES_LIST_FAILED")
            );
        }
    }

    public VehicleDTO convertToDTO(Vehicle vehicle) {
        return VehicleDTO.builder()
            .id(idObfuscator.encodeId(vehicle.getId()))
            .name(vehicle.getName())
            .registrationNumber(vehicle.getRegistrationNumber())
            .type(vehicle.getType())
            .typeDisplayName(vehicle.getType() != null ? vehicle.getType().getDisplayName() : null)
            .typeDescription(vehicle.getType() != null ? vehicle.getType().getDescription() : null)
            .make(vehicle.getMake())
            .model(vehicle.getModel())
            .year(vehicle.getYear())
            .color(vehicle.getColor())
            .capacity(vehicle.getCapacity())
            .fuelType(vehicle.getFuelType())
            .fuelTypeDisplayName(vehicle.getFuelType() != null ? vehicle.getFuelType().getDisplayName() : null)
            .mileage(vehicle.getMileage())
            .insuranceExpiryDate(vehicle.getInsuranceExpiryDate())
            .inspectionExpiryDate(vehicle.getInspectionExpiryDate())
            .isActive(vehicle.getIsActive())
            .notes(vehicle.getNotes())
            .createdAt(vehicle.getCreatedAt())
            .updatedAt(vehicle.getUpdatedAt())
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
